package com.xsdexplorer.tools.gensample;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.util.StreamWriter2Delegate;

/**
 * adopted from com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter,
 * suitable when creator do not write any characters other then the value for leaf nodes 
 */
public class IndentingXMLStreamWriter extends StreamWriter2Delegate {
    private String indentStep = "  ";
    private int depth = 0;
    private boolean seenText = false;

    public IndentingXMLStreamWriter(XMLStreamWriter2 writer) {
        super(writer);
        mDelegate2 = writer;
    }


    public void setIndentStep(String s) {
        this.indentStep = s;
    }

    private void onStartElement() throws XMLStreamException {
        seenText = false;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
        depth++;
    }

    private void onEndElement() throws XMLStreamException {
        depth--;
        if (!seenText) {
            super.writeCharacters("\n");
            doIndent();
        }
        seenText = false;
    }

    private void onEmptyElement() throws XMLStreamException {
        seenText = false;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
    }

    private void doIndent() throws XMLStreamException {
        for (int i = 0; i < depth; i++)
            super.writeCharacters(indentStep);
    }

    public void writeStartDocument() throws XMLStreamException {
        super.writeStartDocument();
        super.writeCharacters("\n");
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        super.writeStartDocument(version);
        super.writeCharacters("\n");
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        super.writeStartDocument(encoding, version);
        super.writeCharacters("\n");
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(localName);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(namespaceURI, localName);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(localName);
    }

    public void writeEndElement() throws XMLStreamException {
        onEndElement();
        super.writeEndElement();
    }

    public void writeCharacters(String text) throws XMLStreamException {
        seenText = true;
        super.writeCharacters(text);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        seenText = true;
        super.writeCharacters(text, start, len);
    }

    public void writeCData(String data) throws XMLStreamException {
        seenText = true;
        super.writeCData(data);
    }
    
    public void skipIndentOfClosingTag() {
        seenText = true;
    }
}
