package com.xsdexplorer.loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLStreamReader2;

import com.google.common.base.Preconditions;

/**
 * Check if input is xsd schema and retrieve the following info:
 * - is version xsd 1.1
 * - ns prefixes map as set on root schema element 
 */
public class XsdInfoLoader {
    private boolean isXsd11;
    private Map<String, String> nsContext = new HashMap<>(); //namespace to prefix

    public enum FormDefault {
        qualified,
        unqualified
    }
    private FormDefault attrFormDefault = FormDefault.unqualified;
    private FormDefault elementFormDefault = FormDefault.unqualified;
    
    public void getSchemaInfo(File f) {
        XMLStreamReader2 r = null;
        try  {
            r = XmlFactory.factory.createXMLStreamReader(f);
            getSchemaInfo(r);
        } catch (Exception e) {
            System.err.println("Warning in detecting xsd version: "+e.getMessage());
        } finally {
            try {
                if (r != null)
                    r.close();
            } catch (XMLStreamException ee) {
            }
        }
    }
    
    //post completion, current element will be schema (or exception is thrown)
    public void getSchemaInfo(XMLStreamReader2 r) throws XMLStreamException {
        readToSchemaRoot(r);
        collectFromSchemaRoot(r);
    }    
    
    private void readToSchemaRoot(XMLStreamReader r) throws XMLStreamException {
        while (r.hasNext()) {
            int eventType = r.next();
            if (eventType == XMLEvent.START_ELEMENT) {
                Preconditions.checkState(r.getLocalName().equals("schema"), "Not a schema document!)");
                return;
            }
        }
        throw new IllegalStateException("Not a schema document!");
    }
    
    private void collectFromSchemaRoot(XMLStreamReader r) {
        if (!isXsd11) {
            String versionStr = r.getAttributeValue("http://www.w3.org/2007/XMLSchema-versioning", "minVersion");
            isXsd11 = "1.1".equals(versionStr);
        }
        collectNamespaces(r);
        String form = r.getAttributeValue(null, "attributeFormDefault");
        if (form != null)
            attrFormDefault = FormDefault.valueOf(form);
        form = r.getAttributeValue(null, "elementFormDefault");
        if (form != null)
            elementFormDefault = FormDefault.valueOf(form);

    }

    private void collectNamespaces(XMLStreamReader r) {
        final int nsCount = r.getNamespaceCount();
        for (int i = 0; i < nsCount; ++i) {
            //System.out.println(r.getNamespacePrefix(i)+":"+r.getNamespaceURI(i));
            nsContext.put(r.getNamespaceURI(i), r.getNamespacePrefix(i));
        }
    }
    
    
    public boolean isXsd11() {
        return isXsd11;
    }

    public Map<String, String> getNsContext() {
        return nsContext;
    }

    public FormDefault getAttrFormDefault() {
        return attrFormDefault;
    }

    public FormDefault getElementFormDefault() {
        return elementFormDefault;
    }

}
