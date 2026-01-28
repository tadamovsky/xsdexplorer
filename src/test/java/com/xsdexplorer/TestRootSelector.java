package com.xsdexplorer;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.net.URI;

import org.apache.xerces.impl.xs.XSLoaderImpl;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.testng.annotations.Test;

import com.xsdexplorer.loader.RootSelector;

public class TestRootSelector {


	@Test
	public void testSelectorFHIR() {
		runTest("samples/FHIR/big/bundle.xsd", "StructureDefinition"); //1115
	}
	
	@Test
	//all top nodes in cycle so many other correct answers 
	public void testSelectorFHIRSingle() {
		runTest("samples/FHIR/single/fhir-single.xsd", "List"); //5838
	}
	
	
	@Test
	public void testSelectorRecType() {
		runTest("samples/ddex/release-notification.xsd", "NewReleaseMessage"); //703
	}

    @Test
    public void testSTAR5() {
        runTest("samples/big_STAR5/ProcessRepairOrder.xsd", "ProcessRepairOrder"); //1105
    }
    
	@Test
	public void testSelectorComplexContent() {
		XSModel model = loadSchema("samples/complexContent/anonExtension.xsd");
		RootSelector rs = new RootSelector(model);
		XSElementDeclaration el = rs.selectRoot();
		System.out.println(el);
	}
	
	
	
	private void runTest(String file, String expected) {
		XSModel model = loadSchema(file);
		RootSelector rs = new RootSelector(model);
		XSElementDeclaration el = rs.selectRoot();
		System.out.println(el);
		assertEquals(el.getName(), expected);
	}
	
    private XSModel loadSchema(String file)  {
    	URI uri = new File(file).getAbsoluteFile().toURI();
		XSLoaderImpl loader = new XSLoaderImpl();
		XSModel model = loader.loadURI(uri.getRawPath());
		return model;
    }	
}
