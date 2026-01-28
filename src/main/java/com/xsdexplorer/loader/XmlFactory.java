package com.xsdexplorer.loader;

import javax.xml.stream.XMLInputFactory;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

public class XmlFactory {
    //https://docs.oracle.com/javase/8/docs/api/javax/xml/stream/XMLInputFactory.html
/*  
    public static final XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
    static {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }
*/
    public static final WstxInputFactory factory = new WstxInputFactory();
    public static final WstxOutputFactory outputFactory = new WstxOutputFactory();

    static {
        factory.configureForSpeed();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        
        outputFactory.configureForSpeed();
        outputFactory.setProperty(WstxInputProperties.P_RETURN_NULL_FOR_DEFAULT_NAMESPACE, true);
        //outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true); 
    }
    
}
