package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.castList;
import static com.xsdexplorer.SchemaUtil.extractOne;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.codehaus.stax2.XMLStreamWriter2;

public class SimpleTypeSerializer {
    private final NsContext c;

    SimpleTypeSerializer(NsContext c) {
        this.c = c;
    }
    
    public void writeSimpleType(XSSimpleTypeDefinition t) throws XMLStreamException {
        XMLStreamWriter2 w = c.xsw;
        w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "simpleType");
        if (!t.getAnonymous()) {
            w.writeAttribute("name", t.getName());
        }
        c.writeAnnotation(extractOne(t.getAnnotations()), w);

        switch (t.getVariety()) {
            case XSSimpleTypeDefinition.VARIETY_ATOMIC:
                writeRestrictionSimpleType(t);
                break;
            case XSSimpleTypeDefinition.VARIETY_LIST:
                writeListSimpleType(t);
                break;
            case XSSimpleTypeDefinition.VARIETY_UNION:
                writeUnionSimpleType(t);
                break;
            default:
                throw new IllegalStateException("Unknown simple type variety "+t.getVariety());
        }
        
        w.writeEndElement(); // simpleType        
    }

    private void writeUnionSimpleType(XSSimpleTypeDefinition t) throws XMLStreamException {
        XMLStreamWriter w = c.xsw;
        w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "union");
        List<XSSimpleTypeDefinition> memberTypes = castList(t.getMemberTypes());
        StringBuilder memberTypesVal = new StringBuilder();
        boolean hasInlineTypes = false;
        for (XSSimpleTypeDefinition mt : memberTypes) {
            if (mt.getAnonymous()) {
                hasInlineTypes = true;
            }
            else {
                memberTypesVal.append(c.qname(mt)).append(" ");
            }
        }
        if (memberTypesVal.length() > 0)
            w.writeAttribute("memberTypes", memberTypesVal.substring(0, memberTypesVal.length() - 1));
        if (hasInlineTypes) {
            for (XSSimpleTypeDefinition mt : memberTypes) {
                if (mt.getAnonymous()) 
                    writeSimpleType(mt);
            }
        }
        w.writeEndElement(); // union
    }

    private void writeListSimpleType(XSSimpleTypeDefinition t) throws XMLStreamException {
        XMLStreamWriter w = c.xsw;
        w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "list");
        XSTypeDefinition itemType = t.getItemType();
        if (itemType == null) {
            // Anonymous item type
            XSSimpleTypeDefinition st = (XSSimpleTypeDefinition) t.getBaseType();
            writeSimpleType(st);
        } else {
            c.writeQNameAttribute("itemType", itemType);
        }
        w.writeEndElement(); // list
    }

    public void writeRestrictionSimpleType(XSSimpleTypeDefinition t) throws XMLStreamException {
        XMLStreamWriter w = c.xsw;
        w.writeStartElement(W3C_XML_SCHEMA_NS_URI, "restriction");

        XSTypeDefinition base = t.getBaseType();
        if (base.getAnonymous()) {
            XSSimpleTypeDefinition st = (XSSimpleTypeDefinition) base;
            writeSimpleType(st);
        } 
        else { /*&& !base.getName().equals("anySimpleType")*/
            c.writeQNameAttribute("base", base);
        }

        writeSimpleTypeDiff(t);
        w.writeEndElement(); // restriction
        
    }

    public void writeSimpleTypeDiff(XSSimpleTypeDefinition t) throws XMLStreamException {
        XMLStreamWriter w = c.xsw;
        writeEnumerationFacetIfPresent(t, w);
        writePatternFacetIfPresent(t, w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_LENGTH, "length", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MINLENGTH, "minLength", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MAXLENGTH, "maxLength", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MININCLUSIVE, "minInclusive", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MAXINCLUSIVE, "maxInclusive", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MINEXCLUSIVE, "minExclusive", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE, "maxExclusive", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_TOTALDIGITS, "totalDigits", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_FRACTIONDIGITS, "fractionDigits", w);
        writeFacetIfPresent(t, XSSimpleTypeDefinition.FACET_WHITESPACE, "whiteSpace", w);
        
    }
    
    private void writeEnumerationFacetIfPresent(XSSimpleTypeDefinition t, XMLStreamWriter w) throws XMLStreamException {
        XSSimpleTypeDefinition base = (XSSimpleTypeDefinition) t.getBaseType();
        List<String> enums = t.getLexicalEnumeration().size() == base.getLexicalEnumeration().size() ? Collections.emptyList() : castList(t.getLexicalEnumeration());
        for (String e : enums) {
            w.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "enumeration");
            w.writeAttribute("value", e);
        }
    }
    
    private void writePatternFacetIfPresent(XSSimpleTypeDefinition t, XMLStreamWriter w) throws XMLStreamException {
        XSSimpleTypeDefinition base = (XSSimpleTypeDefinition) t.getBaseType();
        List<String> pats = reduceList(t.getLexicalPattern(), base.getLexicalPattern());
        for (String p : pats) {
            w.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "pattern");
            w.writeAttribute("value", p);
        }
    }
    
    private void writeFacetIfPresent(XSSimpleTypeDefinition t, short facetKind, String xsdName, XMLStreamWriter w)
            throws XMLStreamException {
        XSSimpleTypeDefinition base = (XSSimpleTypeDefinition) t.getBaseType();
        String v = t.getLexicalFacetValue(facetKind);
        if (v != null && !v.equals(base.getLexicalFacetValue(facetKind))) {
            w.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, xsdName);
            w.writeAttribute("value", v);
        }
    }
    
    private List<String> reduceList(StringList list, StringList base) {
        if (list == null) {
            return Collections.emptyList();
        }
        if (base == null || base.isEmpty()) {
            return castList(list);
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            String v = list.item(i);
            if (!base.contains(v)) {
                result.add(v);
            }
        }
        return result;
    }

    
}
