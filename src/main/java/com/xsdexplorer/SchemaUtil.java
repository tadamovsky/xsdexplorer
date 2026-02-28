package com.xsdexplorer;

import java.util.Collection;
import java.util.List;

import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;

public class SchemaUtil {
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> castList(@SuppressWarnings("rawtypes") List l){
        return l;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> castList(@SuppressWarnings("rawtypes") Collection l){
        return l;
    }
    
    @SuppressWarnings("unchecked")
    public static List<XSParticle> groupParticles(XSTerm term) {
        return ((XSModelGroup) term).getParticles();
    }        

    public static String wrapNull(String s) {
        return s == null ? "" : s;
    }

    public static String groupCompositor(XSModelGroup group) {
        final String[] values = { "sequence", "choice", "all" };
        return values[group.getCompositor() - 1];
    }
 
    public static <T extends XSObject> List<T> sortByName(Collection<T> objects) {
        return objects.stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).toList();
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends XSObject> T extractOne(XSObjectList list) {
        return list.getLength() > 0 ? (T) list.item(0) : null;
    }
    
}
