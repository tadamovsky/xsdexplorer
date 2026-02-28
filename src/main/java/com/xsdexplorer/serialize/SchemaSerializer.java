package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.castList;
import static com.xsdexplorer.SchemaUtil.wrapNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;

import com.xsdexplorer.loader.XsdInfoLoader;

public class SchemaSerializer {
    private final File outputDir;
    
    private final SchemaContext sc;
    
    public SchemaSerializer(XSModel model, String outputDir, XsdInfoLoader xsdInfo) {
        this.sc = new SchemaContext(model, xsdInfo);
        this.outputDir = new File(outputDir);
        try {
            Files.createDirectories(this.outputDir.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create output directory "+outputDir, e);
        }
    }
    
    public void setIndent(boolean indent) {
        sc.setIndent(indent);
    }
    
    public void serialize() {
        List<XSNamespaceItem> nsList = castList(sc.model.getNamespaceItems());
        for (XSNamespaceItem nsItem : nsList) {
            String ns = nsItem.getSchemaNamespace();
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(ns) || XMLConstants.XML_NS_URI.equals(ns))
                continue;
            //System.out.println(ns);
            try {
                doSerialization(nsItem);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize namespace "+ns, e);
            }
        }
        
    }
    
    private void doSerialization(XSNamespaceItem nsItem) throws IOException, XMLStreamException {
        NsItemSerializer serializer = new NsItemSerializer(sc, nsItem);
        QuilifiedDetector detector = new QuilifiedDetector(nsItem);
       
        String output = serializer.serialize(detector.isElementFormQualified());
        
        String ns = wrapNull(nsItem.getSchemaNamespace());
        String filename = sc.getLocation(ns);
        try (BufferedWriter writer = Files.newBufferedWriter(new File(outputDir, filename).toPath()))   {
            writer.write(output);
        }
    }
    
    
}
