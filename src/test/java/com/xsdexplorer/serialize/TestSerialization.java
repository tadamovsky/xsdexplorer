package com.xsdexplorer.serialize;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.xerces.xs.XSModel;
import org.testng.annotations.Test;

import com.xsdexplorer.loader.SchemaLoader;
import com.xsdexplorer.loader.XsdInfoLoader;

public class TestSerialization {
    private XsdInfoLoader xsdInfo;

    private XSModel loadSchema(String file) throws IOException  {
        SchemaLoader schemaLoader = new SchemaLoader();
        XSModel model = schemaLoader.loadSchemaFile(new File(file));
        this.xsdInfo = schemaLoader.getXsdInfo();
        return model;
    }   

    private void runTest(String xsdPath) throws IOException  {
        XSModel model = loadSchema(xsdPath);
        String testName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(xsdPath));
        File outDir = new File("target/xsd-out/"+testName);
        if (outDir.exists()) {
            FileUtils.cleanDirectory(outDir);
        }
        SchemaSerializer serializer = new SchemaSerializer(model, outDir.getAbsolutePath(), xsdInfo);
        serializer.serialize();
        String expectedDir = "src/test/resources/serialization/"+testName;
        //compare directory content outDir vs expectedDir
        File[] outFiles = outDir.listFiles();
        assertTrue(outFiles != null && outFiles.length > 0, "output directory should exist");
        Arrays.sort(outDir.listFiles(), Comparator.comparing(File::getName));
        System.out.println("Comparing output files in "+outDir.getAbsolutePath()+" with expected files in "+expectedDir);
        File[] expectedFiles = new File(expectedDir).listFiles();
        Arrays.sort(expectedFiles, Comparator.comparing(File::getName));
        assertEquals(outFiles.length, expectedFiles.length, "number of output files");
        for (int i = 0; i < outFiles.length; i++) {
            String outContent = FileUtils.readFileToString(outFiles[i], "UTF-8").replace("\r", "");
            String expectedContent = FileUtils.readFileToString(expectedFiles[i], "UTF-8").replace("\r", "");
            assertEquals(outContent, expectedContent, "file content for "+outFiles[i].getName());
        }
    }

    @Test
    public void testGlobalRef() throws IOException {
        String xsdPath = "samples/simple/globalRefs3.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testAny() throws IOException {
        String xsdPath = "samples/simple/any.xsd";
        runTest(xsdPath);
    }

    @Test
    public void testAnyNoNs() throws IOException {
        String xsdPath = "samples/simple/anyNoNs.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testGroups() throws IOException {
        String xsdPath = "samples/simple/groups.xsd";
        runTest(xsdPath);
    }    
    
    @Test
    public void testXmlLang() throws IOException {
        String xsdPath = "samples/simple/xmlLang.xsd";
        runTest(xsdPath);
    }    
    
    @Test
    public void testSimpleTypesWithNoNsImport() throws IOException {
        String xsdPath = "samples/simple/nons_ref.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testCTSimpleContentRest() throws IOException {
        String xsdPath = "samples/complexContent/restr.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testCTSimpleContentRest2() throws IOException {
        String xsdPath = "samples/complexContent/restr2.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testCTComplexContentRest() throws IOException {
        String xsdPath = "samples/complexContent/restriction.xsd";
        runTest(xsdPath);
    }                
    
    @Test
    public void testCTComplexContentRest2() throws IOException {
        String xsdPath = "samples/complexContent/restriction2.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testCTComplexContentExt() throws IOException {
        String xsdPath = "samples/complexContent/anonExtension2.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testElementForm() throws IOException {
        String xsdPath = "samples/elementForm/elUnqualifiedNoDefaultNs.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testAttrQuilified() throws IOException {
        String xsdPath = "samples/elementForm/attrQuilified.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testAbstractElement() throws IOException {
        String xsdPath = "samples/abstract/fishNS.xsd";
        runTest(xsdPath);
    }
    
    @Test
    public void testAbstractElement2() throws IOException {
        String xsdPath = "samples/abstract/pub.xsd";
        runTest(xsdPath);
    }
    
    //@Test
    public void testNamespaceInclude() throws IOException {
        String xsdPath = "c:\\projects\\xsdexplorerOld\\info\\samples\\Vehicle_xsd\\HardIndividualProductTruckBus_3_0_0.xsd";
        runTest(xsdPath);
    }        
}
