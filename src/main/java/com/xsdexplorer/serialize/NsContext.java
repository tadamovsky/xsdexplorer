package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSObject;
import org.codehaus.stax2.XMLStreamWriter2;

import com.google.common.base.Preconditions;
import com.xsdexplorer.AnnotationExtractor;
import com.xsdexplorer.loader.XmlFactory;
import com.xsdexplorer.tools.gensample.IndentingXMLStreamWriter;

public class NsContext {
    public final XMLStreamWriter2 xsw;
    public final String targetNamespace;
    public final SchemaContext sc;

    private final XSNamespaceItem nsItem;
    //referenced namespaces that need to be imported
    private Set<String> namespaces = new HashSet<>();
    
    private StringBuilderWriter output = new StringBuilderWriter();
    
    public NsContext(SchemaContext sc, XSNamespaceItem nsItem) {
        this.sc = sc;
        this.nsItem = nsItem;
        this.xsw = createWriter(output);
        this.targetNamespace = wrapNull(nsItem.getSchemaNamespace());
    }
    
    public void startSchemaDocument() throws XMLStreamException {
        for (var e : sc.getNamespacePrefixMap().entrySet()) {
            if (!e.getKey().isEmpty()) {
                xsw.setPrefix(e.getValue(), e.getKey());
            }
        }
        xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "schema");
    }
    
    public String finalizeDocument(boolean elementFormQualified) throws XMLStreamException {
        xsw.writeEndElement(); // schema
        xsw.writeEndDocument();
        xsw.close();
        
        StringBuilderWriter headerWriter = new StringBuilderWriter();
        XMLStreamWriter2 w = createWriter(headerWriter);
        w.writeStartDocument();
        for (Map.Entry<String, String> e : sc.getNamespacePrefixMap().entrySet()) {
            if (!e.getKey().isEmpty())
                w.setPrefix(e.getValue(), e.getKey());
        }
 
        w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "schema");
        if (!targetNamespace.isEmpty()) {
            w.writeAttribute("elementFormDefault", elementFormQualified ? "qualified" : "unqualified");
            w.writeAttribute("attributeFormDefault", "unqualified");
        }

        namespaces.add(W3C_XML_SCHEMA_NS_URI); //sanity
        for (String ns : namespaces) {
            if (ns.isEmpty() || ns.equals(XMLConstants.XML_NS_URI))
                continue;
            String prefix = Preconditions.checkNotNull(sc.getPrefix(ns), "No prefix mapping found for namespace %s", ns);
            w.writeNamespace(prefix, ns);
        }
        
        if (!targetNamespace.isEmpty()) {
            w.writeAttribute("targetNamespace", targetNamespace);
        }
        writeAnnotation(extractOne(nsItem.getAnnotations()), w);        
        for (String ns : namespaces) {
            if (!ns.equals(targetNamespace) && !ns.equals(W3C_XML_SCHEMA_NS_URI)) {
                w.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "import");
                if  (!ns.equals(""))
                    w.writeAttribute("namespace", ns);
                if  (!ns.equals(XMLConstants.XML_NS_URI))
                    w.writeAttribute("schemaLocation", sc.getLocation(ns));
            }
        }
        w.writeCharacters(""); //if namespaces are empty, close schema start element
        w.flush();
        
        StringBuilder out = output.getBuilder();
        int k = out.indexOf(">"); //move past schema element start
        out.delete(0, k + 1);
        out.insert(0, headerWriter);
        return out.toString();
    }    
    
    public void writeQNameAttribute(String localName, XSObject value) throws XMLStreamException {
        xsw.writeAttribute(localName, qname(value));
    }
    
    public String qname(XSObject value) {
        String ns = wrapNull(value.getNamespace());
        namespaces.add(ns);
        if (ns.isEmpty())
            return value.getName();
        String prefix = sc.getPrefix(ns);
        Preconditions.checkState(prefix != null && !prefix.isEmpty(), "No prefix mapping found for namespace %s", ns);
        return prefix+":"+value.getName();
    }
    
    private XMLStreamWriter2 createWriter(StringBuilderWriter output) {
        try {
            XMLStreamWriter2 w = (XMLStreamWriter2) XmlFactory.outputFactory.createXMLStreamWriter(output);
            if (sc.isIndent())
                w = new IndentingXMLStreamWriter(w);
            return w;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to create XMLStreamWriter", e);
        }
    }
    
    public void writeAnnotation(XSAnnotation annotation) throws XMLStreamException {
        writeAnnotation(annotation, xsw);
    }
    
    public void writeAnnotation(XSAnnotation annotation, XMLStreamWriter2 w) throws XMLStreamException {
        if (annotation != null) {
            w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "annotation");
            w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "documentation");
            w.writeCharacters(AnnotationExtractor.extract(annotation.getAnnotationString()));
            w.writeEndElement(); // documentation
            w.writeEndElement(); // annotation
        }
    }
     
}
