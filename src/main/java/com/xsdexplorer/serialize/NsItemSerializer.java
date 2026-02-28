package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.apache.xerces.impl.xs.SchemaGrammar.isAnyType;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.WILDCARD;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.xerces.xs.*;
import org.codehaus.stax2.XMLStreamWriter2;

import com.xsdexplorer.ComplexTypeExtractor;
import com.xsdexplorer.XsdTreeView.Options;

public class NsItemSerializer {
    private final NsContext c;
    private final XSNamespaceItem nsItem;
    private final SimpleTypeSerializer sts;
    private final AttrSerializer attrSerializer;
    
    private boolean elementFormQualified;
    
    public NsItemSerializer(SchemaContext sc, XSNamespaceItem nsItem) {
        this.c = new NsContext(sc, nsItem);
        this.nsItem = nsItem;
        sts = new SimpleTypeSerializer(c);
        attrSerializer = new AttrSerializer(c, sts);
    }
    
    public String serialize(boolean elementFormQualified) throws XMLStreamException {
        this.elementFormQualified = elementFormQualified;
        c.startSchemaDocument();
        
        XMLStreamWriter2 xsw = c.xsw;
        writeElements(xsw);
        writeTypes(xsw);

        { //global groups
            Collection<XSModelGroupDefinition> globalGroups = castList(nsItem.getComponents(XSConstants.MODEL_GROUP_DEFINITION).values());
            for (XSModelGroupDefinition group : sortByName(globalGroups)) {
                xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "group");

                xsw.writeAttribute("name", group.getName());
                c.writeAnnotation(group.getAnnotation());
                writeModelGroup(group.getModelGroup(), null, xsw);
                xsw.writeEndElement();
            }
        }
        
        attrSerializer.writeGlobalAttributesAndGroups(nsItem);
        
        { //notations
            Collection<XSNotationDeclaration> notations = castList(nsItem.getComponents(XSConstants.NOTATION_DECLARATION).values());
            for (XSNotationDeclaration notation : sortByName(notations)) {
                xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "notation");
                xsw.writeAttribute("name", notation.getName());
                if (notation.getSystemId() != null) {
                    xsw.writeAttribute("system", notation.getSystemId());
                }
                if (notation.getPublicId() != null) {
                    xsw.writeAttribute("public", notation.getPublicId());
                }
                c.writeAnnotation(notation.getAnnotation());
                xsw.writeEndElement();
            }
        }
        
        return c.finalizeDocument(elementFormQualified);
    }
    
    private void writeTypes(XMLStreamWriter2 xsw) throws XMLStreamException {
        Collection<XSTypeDefinition> types = castList(nsItem.getComponents(XSConstants.TYPE_DEFINITION).values());
        for (XSTypeDefinition type : sortByName(types)) {
            writeType(type, xsw);
        }
    }

    private void writeType(XSTypeDefinition type, XMLStreamWriter2 xsw) throws XMLStreamException {
        if (type instanceof XSComplexTypeDefinition ct) {
            writeComplexType(ct, xsw);
        }
        else {
            sts.writeSimpleType((XSSimpleTypeDefinition) type);
        }
    }

    private void writeElements(XMLStreamWriter2 xsw) throws XMLStreamException {
        Collection<XSElementDeclaration> elements = castList(nsItem.getComponents(XSConstants.ELEMENT_DECLARATION).values());
        for (XSElementDeclaration el : sortByName(elements)) {
            writeElement(el, null, xsw);
        }
        
    }
    
    private void writeElement(XSElementDeclaration el, XSParticle parent, XMLStreamWriter2 xsw) throws XMLStreamException {
        xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "element");

        boolean skipType = false;
        if (el.getScope() == XSConstants.SCOPE_GLOBAL) { 
            if (parent != null) { //ref
                c.writeQNameAttribute("ref", el);
                writeOccurs(parent, xsw);
                xsw.writeEndElement();
                return;
            }
            xsw.writeAttribute("name", el.getName());
            if (el.getAbstract())
                xsw.writeAttribute("abstract", "true");
            if (el.getSubstitutionGroupAffiliation() != null) {
                c.writeQNameAttribute("substitutionGroup", el.getSubstitutionGroupAffiliation());
                skipType = el.getSubstitutionGroupAffiliation().getTypeDefinition() == el.getTypeDefinition(); //some weird case Book2 in pub.xsd
            }
        }
        else if (!c.targetNamespace.isEmpty()) { //check local element form
            xsw.writeAttribute("name", el.getName());
            String ns = wrapNull(el.getNamespace());
            if (ns.isEmpty() && elementFormQualified) {
                xsw.writeAttribute("form", "unqualified");
            }
            else if (!ns.isEmpty() && !elementFormQualified) {
                xsw.writeAttribute("form", "qualified");
            }
        }
        writeOccurs(parent, xsw);
        if (el.getNillable())
            xsw.writeAttribute("nillable", "true");
        if (el.getConstraintType() == XSConstants.VC_DEFAULT) {
            xsw.writeAttribute("default", el.getValueConstraintValue().getNormalizedValue());
        } else if (el.getConstraintType() == XSConstants.VC_FIXED) {
            xsw.writeAttribute("fixed", el.getValueConstraintValue().getNormalizedValue());
        }
        
        boolean annotationWritten = false;
        if (!skipType) {
            XSTypeDefinition type = el.getTypeDefinition();
            if (type.getAnonymous()) {
                c.writeAnnotation(el.getAnnotation());
                annotationWritten = true;
                writeType(type, xsw);
            }
            else if (!isAnyType(type)) {
                c.writeQNameAttribute("type", type);
            }
        } 
        if (!annotationWritten) {
            c.writeAnnotation(el.getAnnotation());
        }
        
        xsw.writeEndElement();
    }

    private void writeComplexType(XSComplexTypeDefinition ct, XMLStreamWriter2 xsw) throws XMLStreamException {
        xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "complexType");

        if (!ct.getAnonymous()) {
            xsw.writeAttribute("name", ct.getName());
        }
        if (ct.getAbstract())
            xsw.writeAttribute("abstract", "true");
        if (ct.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED)
            xsw.writeAttribute("mixed", "true");
        c.writeAnnotation(extractOne(ct.getAnnotations()), xsw);

        ComplexTypeExtractor ctExtractor = new ComplexTypeExtractor(true);
        ctExtractor.setSplitRestrictionAttributes(true);
        Options o = new Options();
        List<XSParticle> extParticles = ctExtractor.getParticlesSplitByExtension(ct, o).getRight();
        List<XSAttributeUse> extAttributes = ctExtractor.getAttributesSplitByExtension(ct, o).getRight();
        XSTypeDefinition baseType = ct.getBaseType();
        
        boolean extension = ct.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION;
        boolean restriction = ct.getDerivationMethod() == XSConstants.DERIVATION_RESTRICTION && !isAnyType(baseType);
        boolean simpleContent = ct.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
        boolean simpleContentRestriction = simpleContent && restriction;
        if (extension || restriction) {
            xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, simpleContent ? "simpleContent" : "complexContent");
            xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, extension ? "extension" : "restriction");
            if (simpleContentRestriction) {
                if (!baseType.getAnonymous()) {
                    c.writeQNameAttribute("base", baseType);
                }
                //this is tricky part to detect if simpeContent restriction should contain simpleType definition and/or only "restrictions" of added facets 
                XSSimpleTypeDefinition t = ct.getSimpleType();
                XSSimpleTypeDefinition tbase = ((XSComplexTypeDefinition)baseType).getSimpleType();
                if (tbase == t.getBaseType()) { 
                    sts.writeSimpleTypeDiff(t); //only facets, no new simpleType definition
                } else {
                    sts.writeSimpleType((XSSimpleTypeDefinition) t.getBaseType());
                    sts.writeSimpleTypeDiff(t);
                }
            }
            else {
                c.writeQNameAttribute("base", baseType);
            }
        }
        if (!extParticles.isEmpty()) { //complexContent 
            writeParticle(extParticles.get(0), xsw);
        } 
        if (!extAttributes.isEmpty()) {
            attrSerializer.writeTypeAttributes(extAttributes);
        }
        if (restriction) {
            Collection<XSAttributeUse> forbiddenAttrs = ctExtractor.getRestrictionForbiddenAttributes(ct);
            attrSerializer.writeForbiddenAttributes(forbiddenAttrs);
        }
        writeWildcard(ctExtractor.getAttributeWildcard(ct), "anyAttribute", null, xsw);
        if (extension || restriction) {
            xsw.writeEndElement(); // extension/restriction
            xsw.writeEndElement(); // complexContent
        }
        xsw.writeEndElement(); // complexType
     }
 

    private void writeParticle(XSParticle p, XMLStreamWriter2 xsw) throws XMLStreamException {
        switch (p.getTerm().getType()) {
            case ELEMENT_DECLARATION:
                writeElement((XSElementDeclaration) p.getTerm(), p, xsw);
                break;
            case MODEL_GROUP:
                writeModelGroup((XSModelGroup) p.getTerm(), p, xsw);
                break;
            case WILDCARD:
                writeWildcard((XSWildcard) p.getTerm(), "any", p, xsw);
                break;
            default: 
                throw new IllegalStateException("Unexpected particle term type: "+p.getTerm().getType());
        }
    }

   //attrName is "any" or "anyAttribute"
   private void writeWildcard(XSWildcard term, String attrName, XSParticle p, XMLStreamWriter2 xsw) throws XMLStreamException {
       if (term == null) {
           return;
       }
       xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, attrName);
       if ((term.getConstraintType() == XSWildcard.NSCONSTRAINT_NOT || term.getConstraintType() == XSWildcard.NSCONSTRAINT_LIST) && term.getNsConstraintList().getLength() > 0) {
           List<String> nsList = castList(term.getNsConstraintList());
           String targetNs = nsItem.getSchemaNamespace();
           String ns = nsList.stream().map(s -> s == null ? "##local" : s.equals(targetNs) ? "##targetNamespace" : s).distinct().sorted().collect(Collectors.joining(" "));
           String nsAttrName = "namespace";
           if (term.getConstraintType() == XSWildcard.NSCONSTRAINT_NOT) {
               if (ns.equals("##local ##targetNamespace") || (targetNs == null && ns.equals("##local"))) { //notNamepsace of this equals namespace of ##other
                   ns = "##other";
               }
               else {
                   nsAttrName = "notNamespace"; //must be xsd 1.1
               }
           }
           xsw.writeAttribute(nsAttrName, ns);
       }
       writeOccurs(p, xsw);
       String processContents = switch (term.getProcessContents()) {
           case XSWildcard.PC_LAX -> "lax";
           case XSWildcard.PC_SKIP -> "skip";
           default -> null; //default is strict, can be omitted
       };
       if (processContents != null) {
           xsw.writeAttribute("processContents", processContents);
       }
       c.writeAnnotation(term.getAnnotation());
       xsw.writeEndElement();
   }

    private void writeModelGroup(XSModelGroup term, XSParticle p, XMLStreamWriter2 xsw) throws XMLStreamException {
        XSModelGroupDefinition gd;
        if (p == null || (gd = c.sc.getModelGroup(term)) == null) {
            // local group, write as compositor
            String name = groupCompositor(term);
            xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, name);

            writeOccurs(p, xsw);
            c.writeAnnotation(term.getAnnotation());
            
            List<XSParticle> particles = castList(term.getParticles());
            for (XSParticle pp : particles) {
                writeParticle(pp, xsw);
            }
            xsw.writeEndElement();
        }
        else {
            // group reference
            xsw.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "group");
            c.writeQNameAttribute("ref", gd);
            writeOccurs(p, xsw);
        }
    }
    
    private void writeOccurs(XSParticle p, XMLStreamWriter2 xsw) throws XMLStreamException {
        if (p != null) {
            if (p.getMinOccurs() != 1) {
                xsw.writeAttribute("minOccurs", String.valueOf(p.getMinOccurs()));
            }
            if (p.getMaxOccurs() != 1) {
                xsw.writeAttribute("maxOccurs", p.getMaxOccursUnbounded() ? "unbounded" : String.valueOf(p.getMaxOccurs()));
            }
        }
    }
    
   
}
