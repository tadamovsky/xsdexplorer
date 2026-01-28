package com.xsdexplorer.tools.gensample;

import java.io.Writer;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xs.*;
import org.codehaus.stax2.XMLStreamWriter2;
import static com.xsdexplorer.ComplexTypeExtractor.*;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;

import com.xsdexplorer.loader.XmlFactory;

public class GenSample {
    
    public static class GenSampleBuilder {
        boolean createOptional = true;
        boolean repeatedChoiceCreateAll = true; 
        int repeatedCount = 1;
        XSElementDeclaration root;
        
        public GenSampleBuilder setCreateOptional(boolean createOptional) {
            this.createOptional = createOptional;
            return this;
        }
        public GenSampleBuilder setRepeatedCount(int repeatedCount) {
            this.repeatedCount = repeatedCount;
            return this;
        }
        public GenSampleBuilder setRoot(XSElementDeclaration root) {
            this.root = root;
            return this;
        }
        
        public GenSampleBuilder setRepeatedChoiceCreateAll(boolean selected) {
            this.repeatedChoiceCreateAll = selected;
            return this;
        }
        
        public GenSample build(XSModel model, Writer writer) {
            return new GenSample(model, writer, this);
        }
    }
    
    
    private XSModel model;
    private IndentingXMLStreamWriter w;
    private GenSampleBuilder options;
    private Set<XSTypeDefinition> processingSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private SimpleTypeGen simpleTypeGen = new SimpleTypeGen();
    
    private GenSample(XSModel model, Writer writer, GenSampleBuilder options) {
        this.model = model;
        this.options = options;
        createForWriter(writer);
    }
    
    public void generate() {
        XSElementDeclaration rootDecl = options.root;
        try {
            writeRootAndNamespaces(rootDecl, new HashMap<>());
            completeTree(rootDecl);
            w.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                w.closeCompletely();
            } catch (XMLStreamException e) {
            }
        }
    }

    private void completeTree(XSElementDeclaration el) throws XMLStreamException {
        XSTypeDefinition td = el.getTypeDefinition();
        if (td instanceof XSComplexTypeDecl t) {
            if (t.getAbstract()) {
                t = getNonAbstact(t);
                writeXsiTypeAttr(t);
            }
            writeAttributes(t);
            XSParticle particle = t.getParticle();
            if (particle == null) {
                if (t.getSimpleType() != null) {
                    w.writeCharacters(simpleTypeGen.genValue((XSSimpleTypeDecl) t.getSimpleType()));
                }
                else {
                    w.skipIndentOfClosingTag();
                }
                return;
            }
            
            processingSet.add(td);
            /*
            if (!processingSet.add(particle)) {
                //in recursion case might not be valid to skip if minOccurs=1, means we had to choose different choice somewhere to stop recursion
                return;
            }
            */
            ElementExpander en = new ElementExpander();
            for (XSElementDeclaration child : en.expand(particle)) {
                w.writeStartElement(wrapNull(child.getNamespace()), child.getName());
                completeTree(child);
                w.writeEndElement();
            }
            processingSet.remove(td);
        }
        else {
            //simple type
            w.writeCharacters(simpleTypeGen.genValue((XSSimpleTypeDecl) td));
        }
    }
    
    private void writeXsiTypeAttr(XSComplexTypeDecl t) throws XMLStreamException {
        QName value = new QName(t.getNamespace(), t.getName());
        w.writeQNameAttribute("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", value);
    }

    private XSComplexTypeDecl getNonAbstact(XSComplexTypeDecl type) {
        XSObject ret = getTypes().stream().filter(t -> (t instanceof XSComplexTypeDecl td && td.derivedFromType(type, (short)0))).findAny().orElseThrow();
        return (XSComplexTypeDecl) ret;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Collection<T> castList(@SuppressWarnings("rawtypes") Collection l){
        return l;
    }
    
    private Collection<XSTypeDefinition> getTypes() {
        return castList(model.getComponents(TYPE_DEFINITION).values());
    }

    private void writeRootAndNamespaces(XSElementDeclaration root, Map<String, String> namespaceToPrefix) throws XMLStreamException {
        Map<String, String> prefixToNamespace = createNsMap(namespaceToPrefix);
        for (Map.Entry<String, String> en : prefixToNamespace.entrySet()) {
            w.setPrefix(en.getKey(), en.getValue());
        }
        w.writeStartElement(wrapNull(root.getNamespace()), root.getName());

        for (Map.Entry<String, String> en : prefixToNamespace.entrySet()) {
            w.writeNamespace(en.getKey(), en.getValue());
        }
    }
    
    private static String wrapNull(String s) {
        return s == null ? "" : s;
    }

    private void writeAttributes(XSComplexTypeDecl t) throws XMLStreamException {
        List<XSAttributeUse> allAttrs = castObjectList(t.getAttributeUses());
        for (XSAttributeUse attr : allAttrs) {
            if (options.createOptional || attr.getRequired()) {
                String value = null;
                if (attr.getConstraintType() != XSConstants.VC_NONE) { //fixed or default
                    value = attr.getValueConstraintValue().getNormalizedValue();
                }
                else {
                    value = simpleTypeGen.genValue((XSSimpleTypeDecl) attr.getAttrDeclaration().getTypeDefinition());
                }
                w.writeAttribute(wrapNull(attr.getAttrDeclaration().getNamespace()), attr.getAttrDeclaration().getName(), value);
            }
        }
    }

    private Map<String, String> createNsMap(Map<String, String> namespaceToPrefix) {
        if (namespaceToPrefix.values().stream().anyMatch(p -> p.startsWith("n") && p.length() == 2 && Character.isDigit(p.charAt(1)))) {
            //corner case, ignore namespaceToPrefix map as it contains prefixes like n1
            namespaceToPrefix.clear();
        }
        Map<String, String> prefixToNamespace = new HashMap<>();
        namespaceToPrefix.put(XMLConstants.XML_NS_URI, "xml");
        if (getTypes().stream().anyMatch(td -> td instanceof XSComplexTypeDecl t && t.getAbstract())) {
            namespaceToPrefix.put(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
            prefixToNamespace.put("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        }
        
        StringList namespaces = model.getNamespaces();
        final int len = namespaces.getLength();
        int j = 1;
        for (int i = 0; i < len; ++i) {
            String ns = namespaces.item(i);
            if (ns == null || ns.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI))
                continue;
            String prefix = namespaceToPrefix.get(ns);
            if (prefix == null || prefix.equals("")) {
                prefix = "n" + (j++);
            }
            prefixToNamespace.put(prefix, ns);
        }
        return prefixToNamespace;
    }

    private void createForWriter(Writer writer) {
        try {
            XMLStreamWriter2 wr = (XMLStreamWriter2) XmlFactory.outputFactory.createXMLStreamWriter(writer);
            w = new IndentingXMLStreamWriter(wr);
            w.writeStartDocument();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private class ElementExpander {
        List<XSElementDeclaration> ret = new ArrayList<>();
         
        void doExpand(XSParticle particle) {
            if (particle.getMinOccurs() == 0 && !options.createOptional) {
                return;
            }
            int minxCount = Math.max(particle.getMinOccurs(), 1);
            int maxCount = particle.getMaxOccursUnbounded() ? options.repeatedCount : particle.getMaxOccurs();
            final int count = Math.max(minxCount, maxCount);
            for (int i = 0; i<count; ++i) {
                XSTerm term = particle.getTerm();
                switch (term.getType()) {
                case MODEL_GROUP: 
                {
                    XSModelGroup group = (XSModelGroup) term;
                    List<XSParticle> particles = castObjectList(group.getParticles());
                    switch (group.getCompositor()) {
                    case XSModelGroup.COMPOSITOR_SEQUENCE:
                    case XSModelGroup.COMPOSITOR_ALL:
                        for (XSParticle p : particles) {
                            doExpand(p);
                        }
                        break;
                    case XSModelGroup.COMPOSITOR_CHOICE:
                        if (particle.getMaxOccursUnbounded() && options.repeatedChoiceCreateAll) {
                            for (int j = 0; j<particles.size(); ++j) {
                                doExpand(particles.get(j));
                            }
                        }
                        else {
                            if (particles.size() > 0)
                                doExpand(particles.get(0));
                        }
                        break;
                    default:  
                        throw new RuntimeException("Unknown compositor!");
                    }
                    break;
                }
                case ELEMENT_DECLARATION:
                {
                    XSElementDeclaration el = (XSElementDeclaration) term;
                    if (el.getAbstract()) {
                        List<XSElementDeclaration> subst = castObjectList(model.getSubstitutionGroup(el));
                        el = subst.get(0);
                    }
                    if (!processingSet.contains(el.getTypeDefinition()))
                        ret.add(el);
                    break;
                }
                }
            }
        }

        public List<XSElementDeclaration> expand(XSParticle particle) {
            doExpand(particle);
            return ret;
        }
    }
    
}
