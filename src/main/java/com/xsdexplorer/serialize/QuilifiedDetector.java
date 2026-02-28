package com.xsdexplorer.serialize;
import static com.xsdexplorer.SchemaUtil.castList;
import static com.xsdexplorer.SchemaUtil.groupParticles;
import static com.xsdexplorer.SchemaUtil.wrapNull;
import static org.apache.xerces.impl.xs.SchemaGrammar.isAnyType;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.*;

import com.google.common.base.Preconditions;

public class QuilifiedDetector {
    private boolean elementFormQualified;
    //private Set<String> namespaces = new HashSet<>(); //referenced namespaces (including own namespace if not empty)
    private String targetNamespace;
    private XSNamespaceItem nsItem;
    
    public QuilifiedDetector(XSNamespaceItem nsItem) {
        this.nsItem = nsItem;
        //String targetNamespace = wrapNull(nsItem.getSchemaNamespace());
        //namespaces.add(targetNamespace);
        this.targetNamespace = wrapNull(nsItem.getSchemaNamespace());
        if (!targetNamespace.isEmpty()) {
            extractInfo();
        }
    }
    
    public boolean isElementFormQualified() {
        return elementFormQualified;
    }
    
     private void extractInfo() {
        this.elementFormQualified = detectElementFormQualified();
    }
    
    private boolean detectElementFormQualified() {
        // check if local elements are qualified or unqualified
        int quilifiedCount = 0;
        int unquilifiedCount = 0;
        
        //String ns = nsItem.getSchemaNamespace();
        ElementExpander expander = new ElementExpander();
        Collection<XSElementDeclaration> elements = castList(nsItem.getComponents(XSConstants.ELEMENT_DECLARATION).values());
        for (XSElementDeclaration el : elements) {
            //System.out.println("Expanding global element "+el.getName());
            ArrayDeque<XSElementDeclaration> queue = new ArrayDeque<>(expander.extractLocalChildrenGlobal(el));
            while (!queue.isEmpty()) {
                XSElementDeclaration e = queue.remove();
                if (e.getNamespace() != null) {
                    quilifiedCount++;
                } else {
                    unquilifiedCount++;
                }
                queue.addAll(expander.extractLocalChildren(e));
            }
            //if (el.getAbstract() && el.getTypeDefinition().getAnonymous()) {
            //    anonAbstactType.add(el.getTypeDefinition());
            //}
        }
        
        Collection<XSTypeDefinition> types = castList(nsItem.getComponents(XSConstants.TYPE_DEFINITION).values());
        for (XSTypeDefinition t : types) {
            if (t instanceof XSComplexTypeDefinition ct && ct.getParticle() != null) {
                //System.out.println("Expanding global type "+ct.getName());
                ArrayDeque<XSElementDeclaration> queue = new ArrayDeque<>(expander.extractLocalChildren(ct));
                while (!queue.isEmpty()) {
                    XSElementDeclaration e = queue.remove();
                    if (e.getNamespace() != null) {
                        quilifiedCount++;
                    } else {
                        unquilifiedCount++;
                    }
                    queue.addAll(expander.extractLocalChildren(e));
                }
            }
        }

        
        System.err.println("Namespace '"+targetNamespace+"': "+quilifiedCount+" qualified, "+unquilifiedCount+" unqualified");
        return quilifiedCount >= unquilifiedCount;
    }    
    
    private class ElementExpander {
        //private Set<XSTypeDefinition> processingSet = Collections.newSetFromMap(new IdentityHashMap<>());

        void doExpand(XSParticle particle, List<XSElementDeclaration> ret) {
            XSTerm term = particle.getTerm();
            switch (term.getType()) {
            case MODEL_GROUP: {
                XSModelGroup group = (XSModelGroup) term;
                List<XSParticle> particles = castList(group.getParticles());
                for (XSParticle p : particles) {
                    doExpand(p, ret);
                }
                break;
            }
            case ELEMENT_DECLARATION: {
                XSElementDeclaration el = (XSElementDeclaration) term;
                if (el.getScope() != XSElementDecl.SCOPE_GLOBAL) {
                    //System.out.println("\tAdding local element "+el.getName());
                    ret.add(el);
                }
                break;
            }
            }
        }
        
        public List<XSElementDeclaration> extractLocalChildren(XSElementDeclaration el) {
            XSTypeDefinition t = el.getTypeDefinition();
            if (t instanceof XSComplexTypeDefinition ct && ct.getAnonymous() && ct.getParticle() != null) {
                return extractLocalChildren(ct);
            }
            return Collections.emptyList();
        }

        public List<XSElementDeclaration> extractLocalChildrenGlobal(XSElementDeclaration el) {
            XSTypeDefinition t = el.getTypeDefinition();
            //checkNamepsace(t);
            
            if (t instanceof XSComplexTypeDefinition ct && ct.getAnonymous() && ct.getParticle() != null) {// && el.getSubstitutionGroupAffiliation() != null && !anonAbstactType.contains(t)) {
                XSElementDeclaration subst = el.getSubstitutionGroupAffiliation();
                if (subst == null || subst.getTypeDefinition() != t) {
                    return extractLocalChildren(ct);
                }
                System.out.println("Skipping Element "+el.getName()+" in substitution group of "+subst.getName()+", which has the same anonymous type");
            }
            return Collections.emptyList();
        }
        
        /*
        private void checkNamepsace(XSTypeDefinition t) {
            XSNamespaceItem tItem = t.getNamespaceItem();
            if (tItem == null) { //local definition
                return;
            }
            if (tItem != nsItem) {
                namespaces.add(wrapNull(t.getNamespace()));
            }
        }
        */

        private List<XSElementDeclaration> extractLocalChildren(XSComplexTypeDefinition ct) {
            XSParticle p = ct.getParticle();
            if (p == null)
                return Collections.emptyList();
            XSComplexTypeDecl base = (XSComplexTypeDecl) ct.getBaseType();
            if (ct.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
                XSParticle baseP = base.getParticle();
                if (baseP != p) { //there are elements in ext type
                    List<XSElementDeclaration> ret = new ArrayList<>();
                    if (baseP != null) {
                        List<XSParticle> particles = groupParticles(p.getTerm());
                        Preconditions.checkState(particles.size() == 2 && particles.get(0) == baseP, "unexpected condition in type processing");
                        doExpand(particles.get(1), ret);
                    }
                    else {
                        doExpand(p, ret);
                    }
                    return ret;
                }
                else {
                    return Collections.emptyList(); //all elements are in base type
                }
            }
            else if (isAnyType(base)) {
                List<XSElementDeclaration> ret = new ArrayList<>();
                doExpand(p, ret);
                return ret;
            }
            else
                return Collections.emptyList(); //restricted type has the definition
        }
        
    }        
}
