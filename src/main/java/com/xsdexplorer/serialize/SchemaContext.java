package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.castList;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.*;

import javax.xml.XMLConstants;

import org.apache.xerces.xs.*;

import com.xsdexplorer.loader.XsdInfoLoader;

public class SchemaContext {
    public final XSModel model;
    
    private Map<String, String> namespacePrefixMap = new HashMap<>();
    private Map<String, String> namespaceToLocationMap = new HashMap<>();

    // map from attribute to its parent AttributeGroup (if any)
    Map<XSAttributeUse, List<XSAttributeGroupDefinition>> attrGroup = new IdentityHashMap<>();
    
    // map from XSModelGroup to its parent XSModelGroupDefinition
    private Map<XSModelGroup, XSModelGroupDefinition> modelGroup = new IdentityHashMap<>();

    private boolean indent = true;
    
    public SchemaContext(XSModel model, XsdInfoLoader xsdInfo) {
        this.model = model;
        
        createMapsFromModel(xsdInfo);

        Collection<XSAttributeGroupDefinition> attrGroups = castList(model.getComponents(XSConstants.ATTRIBUTE_GROUP).values());
        for (XSAttributeGroupDefinition group : attrGroups) {
            List<XSAttributeUse> attrs = castList(group.getAttributeUses());
            for (XSAttributeUse attr : attrs) {
                //attrGroup.put(au, group);
               attrGroup.computeIfAbsent(attr, k -> new ArrayList<>()).add(group);
            }
        }
        attrGroup.values().forEach(l -> Collections.sort(l, (g1, g2) -> Integer.compare(g2.getAttributeUses().size(), g1.getAttributeUses().size())));
        
        Collection<XSModelGroupDefinition> groups = castList(model.getComponents(XSConstants.MODEL_GROUP_DEFINITION).values());
        for (XSModelGroupDefinition gd : groups) {
            XSModelGroup g = gd.getModelGroup();
            modelGroup.put(g, gd);
        }
    }
    
    public String getPrefix(String namespace) {
        return namespacePrefixMap.get(namespace);
    }
    
    public String getLocation(String namespace) {
        return namespaceToLocationMap.get(namespace);
    }
    
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }
    
    public XSModelGroupDefinition getModelGroup(XSModelGroup g) {
        return modelGroup.get(g);
    }

    public boolean isIndent() {
        return indent;
    }

    public void setIndent(boolean indent) {
        this.indent = indent;
    }
    
    private void createMapsFromModel(XsdInfoLoader xsdInfo) {
        Map<String, String> hintMap = xsdInfo.getNsContext();
        namespacePrefixMap.put(W3C_XML_SCHEMA_NS_URI, "xs");
        //namespacePrefixMap.put(XMLConstants.XML_NS_URI, "xml");
        HashSet<String> usedPrefix = new HashSet<>();
        HashSet<String> usedFilenames = new HashSet<>();

        usedPrefix.add("xs");
        //usedPrefix.add("xml");
        Collection<XSNamespaceItem> nsItems = castList(model.getNamespaceItems());
        for (XSNamespaceItem nsItem : nsItems) {
            String ns = nsItem.getSchemaNamespace();
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(ns))
                continue;
            if (XMLConstants.XML_NS_URI.equals(ns)) {
                namespacePrefixMap.put(ns, "xml");
                continue;
            }
            if (ns != null) {
                String prefix = hintMap.get(ns);
                if (prefix == null || prefix.isEmpty() || usedPrefix.contains(prefix)) {
                    prefix = generatePrefix(ns, usedPrefix);
                }
                namespacePrefixMap.put(ns, prefix);
            }
            else {
                ns = "";
            }
            
            //update location map
            String fileName = extractFileName(ns, usedFilenames);
            namespaceToLocationMap.put(ns, fileName);
        }
        
    }

    private String generatePrefix(String ns, HashSet<String> usedPrefix) {
        String prefix = extractName(ns);
        if (Character.isDigit(prefix.charAt(0))) { //sanity
            //drop it
            prefix = "ns";
        }
        if (prefix.length() > 3)
            prefix = prefix.substring(0, 3);
        int suffix = 1;
        String candidate = prefix;
        while (!usedPrefix.add(candidate)) {
            candidate = prefix + suffix;
            suffix++;
        }
        return candidate;
    }

    private String extractFileName(String ns, HashSet<String> usedFilenames) {
        String baseName = extractName(ns);
        int suffix = 1;
        String candidate = baseName;
        while (!usedFilenames.add(candidate)) {
            candidate = baseName + suffix;
            suffix++;
        }
        return candidate + ".xsd";
    }
    
    private static String extractName(String targetNamespace) {
        if (targetNamespace.isEmpty()) {
            return "default";
        }
        int i = targetNamespace.indexOf(':');
        if (i > 0) {
            targetNamespace = targetNamespace.substring(i + 1);
        }
        while (targetNamespace.startsWith("/")) {
            targetNamespace = targetNamespace.substring(1);
        }
        if (targetNamespace.startsWith("www.")) {
            targetNamespace = targetNamespace.substring(4);
        }
        i = targetNamespace.indexOf('.');
        if (i > 0) {
            targetNamespace = targetNamespace.substring(0, i);
        }
        return targetNamespace.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    
}
