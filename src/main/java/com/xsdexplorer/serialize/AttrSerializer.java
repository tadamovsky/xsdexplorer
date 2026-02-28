package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.castList;
import static com.xsdexplorer.SchemaUtil.sortByName;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.xs.*;

import com.google.common.base.Preconditions;

public class AttrSerializer {
    private final NsContext c;
    private final SimpleTypeSerializer sts;

    private Set<XSObject> processedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
    
    public AttrSerializer(NsContext c, SimpleTypeSerializer sts) {
        this.c = c;
        this.sts = sts;
    }
    
    // for global attributes, scope is GLOBAL, for local attributes scope is LOCAL,
    // for attributes from group scope is ABSENT
    //note global attribute still can be from group
    public void writeTypeAttributes(List<XSAttributeUse> attrs) throws XMLStreamException {
        final var attrGroup = c.sc.attrGroup;
        Set<XSAttributeUse> attributesFromGroups = attrs.stream().filter(a -> attrGroup.containsKey(a)).collect(Collectors.toSet());
        Set<XSAttributeGroupDefinition> validGroups = attributesFromGroups.stream().map(a -> attrGroup.get(a))
                .flatMap(List::stream).distinct().filter(g -> attributesFromGroups.containsAll(g.getAttributeUses()))
                .collect(Collectors.toSet());

        processedGroups.clear();
        var xsw = c.xsw;
        for (XSAttributeUse attr : attrs) {
            XSAttributeDeclaration decl = attr.getAttrDeclaration();
            //global attribute that is not from group or from group that is not fully contained in attributes, write as global attribute reference
            if (decl.getScope() == XSConstants.SCOPE_LOCAL || 
                    (decl.getScope() == XSConstants.SCOPE_GLOBAL && (!attrGroup.containsKey(attr) || attrGroup.get(attr).stream().noneMatch(validGroups::contains)))) {
                writeAttribute(decl, attr);
                continue;
            }
            // attribute from group, write group reference
            List<XSAttributeGroupDefinition> groups = Preconditions.checkNotNull(attrGroup.get(attr), "Attribute %s is from group but group not found in map", decl);
            if (groups.size() == 1) {
                XSAttributeGroupDefinition group = groups.get(0);
                if (processedGroups.add(group)) {
                    // group not yet processed, write group reference
                    xsw.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "attributeGroup");
                    c.writeQNameAttribute("ref", group);
                }
            }
            else {
                if (Collections.disjoint(processedGroups, groups)) { //no group in processedGroups
                    //filter by validGroups and take max size to find best match
                    XSAttributeGroupDefinition group = groups.stream().filter(validGroups::contains).max(Comparator.comparingInt(g -> g.getAttributeUses().size())).orElseThrow();
                    processedGroups.add(group);
    
                    xsw.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "attributeGroup");
                    c.writeQNameAttribute("ref", group);
                }
            }
        }
    }
    
    public void writeGlobalAttributesAndGroups(XSNamespaceItem nsItem) throws XMLStreamException {
        Collection<XSAttributeDeclaration> globalAttrs = castList(nsItem.getComponents(XSConstants.ATTRIBUTE_DECLARATION).values());
        for (XSAttributeDeclaration attr : sortByName(globalAttrs)) {
            writeAttribute(attr, null);
         }
        
        var xsw = c.xsw;
        Collection<XSAttributeGroupDefinition> attrGroups = castList(nsItem.getComponents(XSConstants.ATTRIBUTE_GROUP).values());
        for (XSAttributeGroupDefinition ag : sortByName(attrGroups)) {
            xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "attributeGroup");
            xsw.writeAttribute("name", ag.getName());
            List<XSAttributeUse> attrs = castList(ag.getAttributeUses());
            for (XSAttributeUse attr : attrs) {
                writeAttribute(attr.getAttrDeclaration(), attr);
            }
            xsw.writeEndElement();
        }
    }
    
    private void writeAttribute(XSAttributeDeclaration attr, XSAttributeUse parent) throws XMLStreamException {
        var xsw = c.xsw;
        xsw.writeStartElement(W3C_XML_SCHEMA_NS_URI, "attribute");
        if (parent != null) { //inside attribute group or complex type
            if (attr.getScope() == XSConstants.SCOPE_GLOBAL) {
                // global attribute, write ref
                c.writeQNameAttribute("ref", attr);
                if (parent.getRequired()) {
                    xsw.writeAttribute("use", "required");
                }
                xsw.writeEndElement();
                return;
            }
            xsw.writeAttribute("name", attr.getName());
            if (parent.getRequired()) {
                xsw.writeAttribute("use", "required");
            }
            String key = parent.getConstraintType() == XSConstants.VC_DEFAULT ? "default" : parent.getConstraintType() == XSConstants.VC_FIXED ? "fixed" : null;
            if (key != null) {
                xsw.writeAttribute(key, parent.getValueConstraintValue().getNormalizedValue());
            }
            if (attr.getNamespace() != null) {
                //quilifed
                xsw.writeAttribute("form", "qualified");
            }
        }
        else {
            xsw.writeAttribute("name", attr.getName());
            String key = attr.getConstraintType() == XSConstants.VC_DEFAULT ? "default" : attr.getConstraintType() == XSConstants.VC_FIXED ? "fixed" : null;
            if (key != null) {
                xsw.writeAttribute(key, attr.getValueConstraintValue().getNormalizedValue());
            }
        }
        XSSimpleTypeDefinition type = attr.getTypeDefinition();
        if (type.getAnonymous()) {
            // inline anonymous type
            sts.writeSimpleType(type);
        } else if (type != SchemaGrammar.fAnySimpleType) {
            c.writeQNameAttribute("type", type);
        }
        xsw.writeEndElement();
    }
    
    public void writeForbiddenAttributes(Collection<XSAttributeUse> attrs) throws XMLStreamException {
        var xsw = c.xsw;
        for (XSAttributeUse attr : attrs) {
            XSAttributeDeclaration decl = attr.getAttrDeclaration();
            xsw.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "attribute");
            if (decl.getScope() == XSConstants.SCOPE_GLOBAL) {
                c.writeQNameAttribute("ref", decl);
            }
            else {
                xsw.writeAttribute("name", decl.getName());
            }
            xsw.writeAttribute("use", "prohibited");
        }
    }
    
}
