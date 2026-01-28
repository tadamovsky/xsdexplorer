package com.xsdexplorer;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.xsdexplorer.loader.SchemaLoader;
import com.xsdexplorer.tools.gensample.GenSample;

public class TestGenSample {
    
    int repCount = 1;
    boolean repChoiceAll = true;

    @BeforeMethod
    void reset() {
        repCount = 1;
        repChoiceAll = true;
    }
    
    @Test
    public void testAllSimpleTypes() {
        String expected = "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<root>\r\n"
                + "  <myString>String</myString>\r\n"
                + "  <myNormalizedString>String</myNormalizedString>\r\n"
                + "  <myToken>String</myToken>\r\n"
                + "  <myLanguage>s</myLanguage>\r\n"
                + "  <myNMTOKEN>c</myNMTOKEN>\r\n"
                + "  <myNMTOKENS>c</myNMTOKENS>\r\n"
                + "  <myName>i</myName>\r\n"
                + "  <myNCName>i</myNCName>\r\n"
                + "  <myID>String</myID>\r\n"
                + "  <myIDREF>String</myIDREF>\r\n"
                + "  <myIDREFS>String</myIDREFS>\r\n"
                + "  <myInteger>0</myInteger>\r\n"
                + "  <myNonPositiveInteger>0</myNonPositiveInteger>\r\n"
                + "  <myNegativeInteger>-1</myNegativeInteger>\r\n"
                + "  <myLong>0</myLong>\r\n"
                + "  <myInt>0</myInt>\r\n"
                + "  <myShort>0</myShort>\r\n"
                + "  <myByte>0</myByte>\r\n"
                + "  <myNonNegativeInteger>0</myNonNegativeInteger>\r\n"
                + "  <myUnsignedLong>0</myUnsignedLong>\r\n"
                + "  <myUnsignedInt>0</myUnsignedInt>\r\n"
                + "  <myUnsignedShort>0</myUnsignedShort>\r\n"
                + "  <myUnsignedByte>0</myUnsignedByte>\r\n"
                + "  <myPositiveInteger>1</myPositiveInteger>\r\n"
                + "  <myDecimal>0.0</myDecimal>\r\n"
                + "  <myFloat>0.0</myFloat>\r\n"
                + "  <myDouble>0.0</myDouble>\r\n"
                + "  <myDuration>PT1004199059S</myDuration>\r\n"
                + "  <myDateTime>2001-10-26T21:32:52+02:00</myDateTime>\r\n"
                + "  <myTime>21:32:52</myTime>\r\n"
                + "  <myDate>2001-10-26</myDate>\r\n"
                + "  <myGYearMonth>2001-10</myGYearMonth>\r\n"
                + "  <myGYear>2001</myGYear>\r\n"
                + "  <myGMonthDay>--05-01</myGMonthDay>\r\n"
                + "  <myGDay>---17</myGDay>\r\n"
                + "  <myGMonth>--05</myGMonth>\r\n"
                + "  <myBoolean>false</myBoolean>\r\n"
                + "  <myBase64Binary>++++</myBase64Binary>\r\n"
                + "  <myHexBinary>3f3c</myHexBinary>\r\n"
                + "  <myAnyURI>String</myAnyURI>\r\n"
                + "  <myQName>String</myQName>\r\n"
                + "  <myNOTATION>jpeg</myNOTATION>\r\n"
                + "  <noContent>\r\n"
                + "  </noContent>\r\n"
                + "</root>";
        runTestForRoot("samples/simple/allSimpleTypes.xsd", "root", expected); 
    }           
  
    @Test
    public void testRestictedBool() {
        runTestForRoot("samples/simple/s.xsd", "restricted_bool", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<restricted_bool>false</restricted_bool>"); 
    }       
    
    @Test
    public void testSampleNamespaces() {
        runTestForRoot("samples/elementForm/elUnqualifiedNoDefaultNs.xsd", "Root", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<n1:Root xmlns:n1=\"http://aaa.com/formTest\" Attribute1=\"String\" Attribute2=\"0\">\r\n"
                + "  <Element1>String</Element1>\r\n"
                + "  <n1:globalEl aaa=\"String\">String</n1:globalEl>\r\n"
                + "  <n1:Root2 Attribute1=\"String\" Attribute2=\"0\">\r\n"
                + "    <n1:Element2>String</n1:Element2>\r\n"
                + "    <n1:globalEl2 aaa=\"String\">String</n1:globalEl2>\r\n"
                + "  </n1:Root2>\r\n"
                + "</n1:Root>"); 
    }
    
    @Test
    public void testAbsType() {
        String expected = "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<n1:Catalogue xmlns:n1=\"http://test.com/source/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <n1:Publication xsi:type=\"BookType\">\r\n"
                + "    <n1:Title>String</n1:Title>\r\n"
                + "    <n1:Author>String</n1:Author>\r\n"
                + "    <n1:Date>String</n1:Date>\r\n"
                + "    <n1:ISBN>String</n1:ISBN>\r\n"
                + "  </n1:Publication>\r\n"
                + "</n1:Catalogue>";
        runTestForRoot("samples/abstract/absType.xsd", "Catalogue", expected); 
    }    
    
    @Test
    public void testAbsElement() {
        runTestForRoot("samples/abstract/fish.xsd", "Sea", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<Sea name=\"String\">\r\n"
                + "  <Tuna name=\"String\"/>\r\n"
                + "</Sea>"); 
    }        
    
    @Test
    public void simpleRec() {
        runTestForRoot("samples/rec/simpleRec.xsd", "locale", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<n1:locale xmlns:n1=\"http://test.com/source/\">\r\n"
                + "  <n1:name>String</n1:name>\r\n"
                + "  <n1:locales>\r\n"
                + "  </n1:locales>\r\n"
                + "</n1:locale>"); 
    }

    @Test
    public void unbChoice() {
        runTestForRoot("samples/testChoice/unbChoice1.xsd", "seq3", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<seq3>\r\n"
                + "  <child chAttr=\"String\">\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el1>String</el1>\r\n"
                + "    <el2>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "    </el2>\r\n"
                + "  </child>\r\n"
                + "</seq3>"); 

        repCount = 2;
        runTestForRoot("samples/testChoice/unbChoice1.xsd", "seq3", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<seq3>\r\n"
                + "  <child chAttr=\"String\">\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el1>String</el1>\r\n"
                + "    <el2>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "    </el2>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el1>String</el1>\r\n"
                + "    <el2>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "    </el2>\r\n"
                + "  </child>\r\n"
                + "  <child chAttr=\"String\">\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el1>String</el1>\r\n"
                + "    <el2>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "    </el2>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el1>String</el1>\r\n"
                + "    <el2>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "      <el2child>String</el2child>\r\n"
                + "    </el2>\r\n"
                + "  </child>\r\n"
                + "</seq3>"); 
        
        repChoiceAll = false;
        runTestForRoot("samples/testChoice/unbChoice1.xsd", "seq3", "<?xml version='1.0' encoding='UTF-8'?>\r\n"
                + "<seq3>\r\n"
                + "  <child chAttr=\"String\">\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "  </child>\r\n"
                + "  <child chAttr=\"String\">\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "    <el11>String</el11>\r\n"
                + "  </child>\r\n"
                + "</seq3>"); 
        
    }    
    
    private void runTestForRoot(String file, String root, String expected) {
        XSModel model = loadSchema(file);
        StringList namespaces = model.getNamespaces();
        final int len = namespaces.getLength();
        XSElementDeclaration el = null;
        for (int i = 0; i < len; ++i) {
            String ns = namespaces.item(i);
            el = model.getElementDeclaration(root, ns);
            if (el != null) {
                StringBuilderWriter stringWriter = new StringBuilderWriter();                
                GenSample gen = new GenSample.GenSampleBuilder().setRoot(el)
                        .setRepeatedChoiceCreateAll(repChoiceAll)
                        .setRepeatedCount(repCount)
                        .build(model, stringWriter);
                gen.generate();
                String actual = stringWriter.getBuilder().toString().replaceAll("\\r", "");
                System.out.println(actual);
                assertEquals(actual.trim(), expected.replaceAll("\\r", "").trim());
                return;
            }
        }
        throw new RuntimeException("cannot find element "+root);
    }
   
    private XSModel loadSchema(String file)  {
        SchemaLoader loader = new SchemaLoader();
        try {
            XSModel model = loader.loadSchemaFile(new File(file));
            return model;
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }   
}
