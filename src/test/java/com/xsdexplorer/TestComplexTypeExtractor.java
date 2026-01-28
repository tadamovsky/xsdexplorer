package com.xsdexplorer;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSParticle;
import org.testng.annotations.Test;

import com.xsdexplorer.XsdTreeView.Options;
import com.xsdexplorer.loader.SchemaLoader;

public class TestComplexTypeExtractor {

    /*
    private XSElementDeclaration getElement(XSModel model, String name) {
        return model.getElementDeclaration(name, model.getNamespaces().item(0));
    }
    */
    
    private XSComplexTypeDecl getType(XSModel model, String name) {
        return (XSComplexTypeDecl) model.getTypeDefinition(name, model.getNamespaces().item(0));
    }
    
    private XSComplexTypeDecl getElementType(XSModel model, String name) {
        XSElementDeclaration el = model.getElementDeclaration(name, model.getNamespaces().item(0));
        return (XSComplexTypeDecl) el.getTypeDefinition();
    }

    
    private void runTest(XSComplexTypeDecl type, Options o, int expectedBaseSize, 
            String expectedAttsBase, String expectedAttrsExt, String expectedModelBase, String expectedModelExt) {
        ComplexTypeExtractor ct = new ComplexTypeExtractor(type.getAnonymous());
        Pair<List<XSAttributeUse>, List<XSAttributeUse>> attrs = ct.getAttributesSplitByExtension(type, o);
        Pair<List<XSParticle>, List<XSParticle>> particles = ct.getParticlesSplitByExtension(type, o);
        int baseTypeSize = (attrs.getLeft().isEmpty() ? 0 : 1) + particles.getLeft().size();
        assertEquals(baseTypeSize, expectedBaseSize, "base type size");
        
        String attrBase = attrArrToString(attrs.getLeft());
        assertEquals(attrBase, expectedAttsBase, "base attributes");

        String attrExt = attrArrToString(attrs.getRight());
        assertEquals(attrExt, expectedAttrsExt, "ext attributes");
        
        String partBase = partArrToString(particles.getLeft());
        assertEquals(partBase, expectedModelBase, "base model");

        String partExt = partArrToString(particles.getRight());
        assertEquals(partExt, expectedModelExt, "ext model");
    }
    
    

    private String partArrToString(List<XSParticle> left) {
        return left.toString();
    }

    private String attrArrToString(List<XSAttributeUse> attr) {
        return attr.stream().map(a -> a.getAttrDeclaration().getName()).collect(Collectors.joining(" "));
    }
    
    @Test
    public void test1() throws Exception {
        String f = "samples/complexContent/anonExtension2.xsd";
        Options o = new Options();
        XSModel model = new SchemaLoader().loadSchemaFile(new File(f));
  
        {
            XSComplexTypeDecl td = getElementType(model, "ExtTest5");
            runTest(td, o, 3, "CCCCC", "BBB", "[(AA1,AA2), (BB)]", "[]");
        }

        {
            XSComplexTypeDecl td = getElementType(model, "ExtTest4");
            runTest(td, o, 2, "Attr4 Attr3 Attr2 Attr1", "Attr5", "[(C)]", "[(D)]");
        }
        
        {      
            Options o2 = new Options();
            o2.showTypes.set(false);
            XSComplexTypeDecl td = getElementType(model, "ExtTest4");
            runTest(td, o2, 0, "", "Attr5 Attr4 Attr3 Attr2 Attr1", "[]", "[(C), (D)]");
        }
        
        {
            XSComplexTypeDecl td = getElementType(model, "ExtTest3");
            runTest(td, o, 2, "Attr4 Attr3 Attr2 Attr1", "Attr5", "[(C)]", "[]");
        }
        
      
        {
            XSComplexTypeDecl td = getElementType(model, "ExtTest2");
            runTest(td, o, 1, "Attr1", "Attr2", "[]", "[(D)]");
        }
        {
            XSComplexTypeDecl td = getElementType(model, "ExtTest1");
            runTest(td, o, 1, "Attr1", "Attr2", "[]", "[]");
        }
        {
            XSComplexTypeDecl td = getElementType(model, "ElContinue");
            runTest(td, o, 0, "", "DDDD CCCCC", "[]", "[(AA1,AA2), (BB)]");
        }
        
        {
            XSComplexTypeDecl td = getElementType(model, "ElContinue2");
            runTest(td, o, 0, "", "DDDD CCCCC", "[]", "[(AA1,AA2), (BB), (DDDD)]");
        }        

        {
            XSComplexTypeDecl td = getElementType(model, "ElContinue3");
            runTest(td, o, 0, "", "EEEE DDDD CCCCC", "[]", "[(AA1,AA2), (BB), (DDDD)]");
        }
        
        {
            XSComplexTypeDecl td = getElementType(model, "ElContinue4");
            runTest(td, o, 0, "", "EEEE DDDD CCCCC", "[]", "[(AA1,AA2), (BB), (DDDD)]");
        }        
        
        {
            XSComplexTypeDecl td = getType(model, "emptyType");
            runTest(td, o, 0, "", "", "[]", "[]");
        }
        
        {
            XSComplexTypeDecl td = getType(model, "GlobalAddrTypeExtType");
            runTest(td, o, 0, "", "CCCCCCCC CCCCC", "[]", "[(AA1,AA2), (BB), (C)]");
        }        
        
        {
            XSComplexTypeDecl td = getElementType(model, "BillingAddress");
            runTest(td, o, 1, "", "aaa", "[(Country,Street)]", "[(Department2|(addr2))]");
        }
        
        {
            XSComplexTypeDecl td = getElementType(model, "RootExtComplexTypeExtComplexType");
            runTest(td, o, 4, "CCCCCCCC CCCCC", "EEEE", "[(AA1,AA2), (BB), (C)]", "[(D)]");
        }
        
    }    
}
