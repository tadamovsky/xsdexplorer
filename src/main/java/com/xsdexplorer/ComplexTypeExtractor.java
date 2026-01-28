package com.xsdexplorer;
import static org.apache.xerces.impl.xs.SchemaGrammar.isAnyType;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xs.*;

import com.google.common.base.Preconditions;
import com.xsdexplorer.XsdTreeView.Options;

/**
 * Does 2 things:<BR>
 * 1. For types created by extension (either anonymous or not),
 *    remove (recursively) the sequence of extension.<BR>
 * 2. For types created by extension, split children to base and ext 
 *
 */
public class ComplexTypeExtractor {
    
    private int baseTypeSize;
    //whether to split type if possible
    private boolean splitRequested; 
    
    ComplexTypeExtractor(boolean splitRequested) {
        this.splitRequested = splitRequested;
    }
    
    public int getBaseTypeSize() {
        return baseTypeSize;
    }
    
    private static List<XSParticle> removeSeqOfExt(XSComplexTypeDecl td) {
        XSParticle p = td.getParticle();
        
        LinkedList<XSParticle> ret = new LinkedList<>();
        ret.add(p);
        while (!isAnyType(td/*.getBaseType()*/) && td.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
            XSComplexTypeDecl base = (XSComplexTypeDecl) td.getBaseType();
            XSParticle baseP = base.getParticle();
            if (baseP == null) { //nothing in base type (only attributes)
                return ret;
            }
            if (p == baseP) { //nothing in ext type (only attributes)
                td = base;
                continue;
            }
            
            List<XSParticle> groupParticles = groupParticles(p.getTerm());
            Preconditions.checkState(groupParticles.size() == 2 && groupParticles.get(0) == baseP, "removeSeqOfExt: unexpected condition in type processing");
            ret.remove(0);
            ret.addAll(0, groupParticles);
             
            td = base;
            p = groupParticles.get(0); //p is particle of baseType
        }
        return ret;
    }    
    
    Pair<List<XSAttributeUse>, List<XSAttributeUse>> getAttributesSplitByExtension(XSComplexTypeDecl t, Options options) {
        List<XSAttributeUse> allAttrs = options.showAttributes.get() ? castObjectList(t.getAttributeUses()) : Collections.emptyList();
        if (!options.showTypes.get() || !splitRequested || allAttrs.isEmpty() || !(t.getBaseType() instanceof XSComplexTypeDecl)) { //no split
            return Pair.of(Collections.emptyList(), allAttrs);
        }
        
        if (/*t.getBaseType() != fAnyType && */t.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
            List<XSAttributeUse> baseAttrs = castObjectList(((XSComplexTypeDecl) t.getBaseType()).getAttributeUses());
            if (!baseAttrs.isEmpty()) {
                ArrayList<XSAttributeUse> extendedAttr = new ArrayList<XSAttributeUse>(allAttrs);
                extendedAttr.removeAll(baseAttrs);
                return Pair.of(baseAttrs, extendedAttr);
            }
        }
        return Pair.of(Collections.emptyList(), allAttrs);
    }
    
    Pair<List<XSParticle>, List<XSParticle>> getParticlesSplitByExtension(XSComplexTypeDecl td, Options options) {
        XSParticle p = td.getParticle();
        if (!options.showTypes.get() || !splitRequested || p == null) //no split
            return Pair.of(Collections.emptyList(), p == null ? Collections.emptyList() : removeSeqOfExt(td));
        
        //there are elements in base or ext or both
        if (/*td.getBaseType() != fAnyType && */td.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
            XSComplexTypeDecl base = (XSComplexTypeDecl) td.getBaseType();
            XSParticle baseP = base.getParticle();
            if (baseP == null) { //nothing in base type (only attributes)
                return Pair.of(Collections.emptyList(), List.of(p));
            }
            if (p == baseP) { //nothing in ext type (only attributes)
                return Pair.of(removeSeqOfExt(base), Collections.emptyList());
            }
            List<XSParticle> particles = groupParticles(p.getTerm());
            Preconditions.checkState(particles.size() == 2 && particles.get(0) == baseP, "getParticlesSplitByExtension: unexpected condition in type processing");
            return Pair.of(removeSeqOfExt(base), Arrays.asList(particles.get(1)));
         }        
        
        return Pair.of(Collections.emptyList(), List.of(p));
    }    
    
    public List<TreeNodeControl> extractComplexTypeChildren(XSComplexTypeDecl td, XsdTreeNode parent, Options options) {
        Pair<List<XSAttributeUse>, List<XSAttributeUse>> attrs = getAttributesSplitByExtension(td, options);
        Pair<List<XSParticle>, List<XSParticle>> particles = getParticlesSplitByExtension(td, options);
        baseTypeSize = (attrs.getLeft().isEmpty() ? 0 : 1) + particles.getLeft().size();
        int total = baseTypeSize + (attrs.getRight().isEmpty() ? 0 : 1) + particles.getRight().size(); 
        if (total == 0) {
            return Collections.emptyList();
        }
        List<TreeNodeControl> ret = new ArrayList<TreeNodeControl>(total);
        if (!attrs.getLeft().isEmpty()) {
            ret.add(new AttributeNode(attrs.getLeft(), parent.getParentLayout()));
        }
        for (XSParticle p : particles.getLeft()) {
            ret.add(new XsdTreeNode(p, parent));
        }
        if (!attrs.getRight().isEmpty()) {
            ret.add(new AttributeNode(attrs.getRight(), parent.getParentLayout()));
        }
        for (XSParticle p : particles.getRight()) {
            ret.add(new XsdTreeNode(p, parent));
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> castObjectList(XSObjectList l){
        return l;
    }
   
    @SuppressWarnings("unchecked")
    private static List<XSParticle> groupParticles(XSTerm term) {
        return ((XSModelGroup) term).getParticles();
    }        

}
