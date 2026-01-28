package com.xsdexplorer.loader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLStreamReader2;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;

/**
 * Extract top level xsd files from directory - that are not referenced by others, while including all others.
 * Supports circular deps (will bring one of the circular files). 
 *  
 * Note that "simple" solution of just loading every file will work, declarations are merged per namespace via GrammarMerger pool. The difference with full sol. like below:
 * 1. dependent xsds will be loaded a number of times and causes weird bugs sometimes (like model.getNamespaces() containing multiple copies of same namespace, truncated subst groups in energistics schema test) 
 * 2. include of no-ns to some ns: simple solution will also bring no-ns declarations.   
 */
public class FromDirectory {

    List<Path> files;
    //Path baseFileDir;
    
    private MutableGraph<Path> graph;
    private ArrayList<Path> topLevel = new ArrayList<>();
    
    private ReaderCreator xmlReaderCreator;
    
    private XsdInfoLoader xsdInfo;
    
    FromDirectory(File f, XsdInfoLoader xsdInfo) throws IOException {
        this.xsdInfo = xsdInfo;
        //Path baseFileDir = f.toPath();
        try (Stream<Path> walk = Files.walk(f.toPath())) {
            files = walk.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".xsd")).collect(Collectors.toList());
        }
        xmlReaderCreator = p -> XmlFactory.factory.createXMLStreamReader(p.toFile());
    }
    
    FromDirectory (Map<Path, byte[]> xsdFilesMap, XsdInfoLoader xsdInfo) {
        this.xsdInfo = xsdInfo;
        files = new ArrayList<>(xsdFilesMap.keySet());
        xmlReaderCreator = p ->  (XMLStreamReader2) XmlFactory.factory.createXMLStreamReader(new ByteArrayInputStream(xsdFilesMap.get(p)));
    }
    
    List<Path> extractTopLevel() {
        graph = GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(files.size()).build();
        
        for (Path p : files) {
            graph.addNode(p);
            doLoad(p);
        }
        
       Graph<Path> transitiveClosure = Graphs.transitiveClosure(graph);
        
        while (!graph.nodes().isEmpty()) {
            Path node = graph.nodes().iterator().next();
            
            boolean isTopLevel = graph.inDegree(node) == 0;
            Set<Path> successors = transitiveClosure.successors(node);
            if (!isTopLevel) { //check for loops
                Set<Path> predecessors = transitiveClosure.predecessors(node);
                isTopLevel = successors.containsAll(predecessors);
            }
            if (isTopLevel) {
                topLevel.add(node);
            }
            for (Path p : successors) { //includes node
                graph.removeNode(p);
            }
        }        
        
        return topLevel;
    }
    
    private void doLoad(Path p) {
        XMLStreamReader2 r = null;
        try {
            r = xmlReaderCreator.create(p);
            xsdInfo.getSchemaInfo(r);
            while (r.hasNext()) {
                int eventType = r.nextTag();
                if (eventType != XMLEvent.START_ELEMENT) {
                    return; 
                }
                String localName = r.getLocalName();
                if (localName.equals("import") || localName.equals("include")) {
                    String location = r.getAttributeValue(null, "schemaLocation");
                    if (location != null) {
                        Path path = p.getParent().resolve(location).normalize();
                        if (!path.equals(p))
                            graph.putEdge(p, path);
                    }
                }
                r.skipElement();
            }
            
        } catch (Exception e) {
            System.err.println("Warning in processing xsd includes: "+e.getMessage());
        } finally {
            try {
                if (r != null)
                    r.closeCompletely();
            } catch (XMLStreamException ee) {
            }
        }
    }

    
    @FunctionalInterface
    private interface ReaderCreator {
        XMLStreamReader2 create(Path p) throws XMLStreamException;
    }    
    
}
