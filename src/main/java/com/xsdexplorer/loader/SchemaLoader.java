package com.xsdexplorer.loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.*;
import org.apache.xerces.impl.xs.util.XSGrammarPool;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;

import com.google.common.base.Preconditions;
import com.xsdexplorer.LogView;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class SchemaLoader {
    public static final String XML_SCHEMA_VERSION =
            Constants.XERCES_PROPERTY_PREFIX + Constants.XML_SCHEMA_VERSION_PROPERTY;

    private MyGrammarMerger pool;
    private final StringProperty messages = new SimpleStringProperty(this, "messages", "");

    private XsdInfoLoader xsdInfo = new XsdInfoLoader();
    private SymbolTable symTable;
    
    public SchemaLoader() {
        pool = new MyGrammarMerger();
    }
    
    public XSModel loadSchemaFile(File f) throws IOException  {
        loadSchema(f);
        return pool.toXSModel(xsdInfo.isXsd11() ? Constants.SCHEMA_VERSION_1_1 : Constants.SCHEMA_VERSION_1_0);
    }    
    
    public void loadSchema(File f) throws IOException  {
        XMLSchemaLoader loader = new XMLSchemaLoader();
        //XSLoaderImpl loader = new XSLoaderImpl();
        loader.setParameter(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING, true);
        loader.setParameter(Constants.DOM_ERROR_HANDLER, new DOMErrorHandlerImpl());
        EntityResolverImpl entityResolver = new EntityResolverImpl();
        loader.setParameter(XMLSchemaLoader.ENTITY_RESOLVER, entityResolver);
        loader.setParameter(XMLSchemaLoader.XMLGRAMMAR_POOL, pool);
        Preconditions.checkState(!pool.inXmlValidationState, "internal error - is loaded in xml validation state");
        //loader.setParameter(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true); //failing with dtd
        //loader.setParameter(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, false); //not supported 

        if (f.isFile()) {
            if  (f.getName().endsWith(".xsd")) {
                //xsd 1.1 validation - need to use xerces with schema1.1 edition - https://dlcdn.apache.org//xerces/j/binaries/Xerces-J-bin.2.12.2-xml-schema-1.1.zip
                xsdInfo.getSchemaInfo(f);
                setXsd11IfNeeded(loader);
                loader.loadURI(f.toURI().getRawPath());
        
                //validate xml docs with this xsd (from xerces faq)
                /*
                XMLGrammarPool pool = (XMLGrammarPool) loader.getParameter(XMLSchemaLoader.XMLGRAMMAR_POOL);
                XMLParserConfiguration config = new XIncludeAwareParserConfiguration();
                config.setProperty(XMLSchemaLoader.XMLGRAMMAR_POOL, pool);
                SAXParser parser = new SAXParser(config);
                */
            }
            else if (f.getName().endsWith(".zip")) {
                FromZip fromZip = new FromZip(f);
                entityResolver.setFileContentMap(fromZip.getMapOfXsdStrings());
                List<Path> topLevel = new FromDirectory(fromZip.getMapOfXsdStrings(), xsdInfo).extractTopLevel();
                addInfoMessage("top level files in arhive: "+fromZip.relativize(topLevel));
                setXsd11IfNeeded(loader);
                for (Path p : topLevel) {
                    loader.loadGrammar(entityResolver.getSourceFromPath(p));
                }
            }
        }
        else if (f.isDirectory()) {
            /*
            //"simple" sol of loading every file, see comment in FromDirectory
            try (Stream<Path> walk = Files.walk(f.toPath())) {
                walk.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".xsd")).forEach(p -> loader.loadURI(p.toUri().getPath()));
            }
            */
            Instant start = Instant.now();
            List<Path> topLevel = new FromDirectory(f, xsdInfo).extractTopLevel();
            long timeElapsed = Duration.between(start, Instant.now()).toMillis();
            System.out.println("Directory load top level xsd files: "+timeElapsed+"ms - "+topLevel);
            addInfoMessage("top level files in directory: "+topLevel);
            setXsd11IfNeeded(loader);
            for (Path p : topLevel) {
                loader.loadURI(p.toUri().getRawPath());
            }
            timeElapsed = Duration.between(start, Instant.now()).toMillis();
            System.out.println("Directory load total: "+timeElapsed+"ms");
        }
        else 
            throw new IOException("Cannot read file "+f);
        symTable = (SymbolTable) loader.getParameter(XMLSchemaLoader.SYMBOL_TABLE);
    }    

    private void setXsd11IfNeeded(XMLSchemaLoader loader) {
        if (xsdInfo.isXsd11()) {
            addInfoMessage("xsd version 1.1 is detected and will be used for validation");
            loader.setParameter(XML_SCHEMA_VERSION, Constants.W3C_XML_SCHEMA11_NS_URI);
        }
    }
    
    private void addInfoMessage(String message) {
        messages.set(LogView.Kind.INFO.formatMessage(message));
    }

    private void addErrorMessage(String message) {
        messages.set(LogView.Kind.ERROR.formatMessage(message));
    }
    
    public XSModel getXSModel() {
        return pool.toXSModel(xsdInfo.isXsd11() ? Constants.SCHEMA_VERSION_1_1 : Constants.SCHEMA_VERSION_1_0);
    }
    
    public XSGrammarPool getPool() {
        pool.enableXmlValidationSate();
        return pool;
    }

    public SymbolTable getSymbolTable() {
        return symTable;
    }
    
    public boolean isXsd11() {
        return xsdInfo.isXsd11();
    }
    
    public StringProperty messageProperty() {
        return messages;
    }
    
    private class DOMErrorHandlerImpl implements DOMErrorHandler {

        public boolean handleError(DOMError error) {
            boolean fatal = error.getSeverity() == DOMError.SEVERITY_FATAL_ERROR;
            DOMLocator l = error.getLocation();
            String locationStr = "";
            if (l != null) {
                String systemId = l.getUri();
                if (systemId != null) {
                    if (systemId.startsWith("file://"))
                        systemId = new File(systemId.substring(7)).getAbsolutePath();
                    systemId = systemId+": ";
                }
                else {
                    systemId = "";
                }
                locationStr = String.format("%sline %d, column :%d%s", systemId, l.getLineNumber(), l.getColumnNumber(), (l.getRelatedNode() != null ? " [" + l.getRelatedNode().getNodeName()+ "]" : ""));
            }
            String errorMsg = locationStr + ": " + error.getMessage(); 
            addErrorMessage(errorMsg);
            if (error.getRelatedException() instanceof XMLParseException e) {
                if (e.getException() != null) {
                    e.getException().printStackTrace();
                }
            }
            if (fatal)
                throw new RuntimeException(errorMsg);
     
            return true;
        }
    }
    
     /**
     * based on GrammarMerger in XSLoaderImpl
     * original impl cannot be used for validation of xml post xsd loading,
     * because getGrammar() always returns null (needed for merge functionality during xsd loading time)
     * hence the addition to original impl is "inXmlValidationState" var which is used in  getGrammar()
     */
    private static final class MyGrammarMerger extends XSGrammarPool {
        private boolean inXmlValidationState = false;
        
        MyGrammarMerger () {}
        
        public void enableXmlValidationSate() {
            inXmlValidationState = true;
        }
        
        public void putGrammar(Grammar grammar) {
            if (inXmlValidationState) {
                //System.out.println("putGrammar In xml validation: "+grammarKey(grammar.getGrammarDescription()));
                return;
            }
            SchemaGrammar cachedGrammar = 
                toSchemaGrammar(super.getGrammar(grammar.getGrammarDescription()));
            if (cachedGrammar != null) {
                SchemaGrammar newGrammar = toSchemaGrammar(grammar);
                if (newGrammar != null) {
                    mergeSchemaGrammars(cachedGrammar, newGrammar);
                }
            }
            else {
                super.putGrammar(grammar);
            }
        }

        private SchemaGrammar toSchemaGrammar (Grammar grammar) {
            return (grammar instanceof SchemaGrammar) ? (SchemaGrammar) grammar : null;
        }
        
        private void mergeSchemaGrammars(SchemaGrammar cachedGrammar, SchemaGrammar newGrammar) {

            /** Add new top-level element declarations. **/
            XSNamedMap map = newGrammar.getComponents(XSConstants.ELEMENT_DECLARATION);
            int length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSElementDecl decl = (XSElementDecl) map.item(i);
                if (cachedGrammar.getGlobalElementDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalElementDecl(decl);
                }
            }
            
            /** Add new top-level attribute declarations. **/
            map = newGrammar.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSAttributeDecl decl = (XSAttributeDecl) map.item(i);
                if (cachedGrammar.getGlobalAttributeDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalAttributeDecl(decl);
                }
            }
            
            /** Add new top-level type definitions. **/
            map = newGrammar.getComponents(XSConstants.TYPE_DEFINITION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSTypeDefinition decl = (XSTypeDefinition) map.item(i);
                if (cachedGrammar.getGlobalTypeDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalTypeDecl(decl);
                }
            }
            
            /** Add new top-level attribute group definitions. **/
            map = newGrammar.getComponents(XSConstants.ATTRIBUTE_GROUP);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSAttributeGroupDecl decl = (XSAttributeGroupDecl) map.item(i);
                if (cachedGrammar.getGlobalAttributeGroupDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalAttributeGroupDecl(decl);
                }
            }
            
            /** Add new top-level model group definitions. **/
            map = newGrammar.getComponents(XSConstants.MODEL_GROUP);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSGroupDecl decl = (XSGroupDecl) map.item(i);
                if (cachedGrammar.getGlobalGroupDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalGroupDecl(decl);
                }
            }
            
            /** Add new top-level notation declarations. **/
            map = newGrammar.getComponents(XSConstants.NOTATION_DECLARATION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSNotationDecl decl = (XSNotationDecl) map.item(i);
                if (cachedGrammar.getGlobalNotationDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalNotationDecl(decl);
                }
            }
            
            /** 
             * Add all annotations. Since these components are not named it's
             * possible we'll add duplicate components. There isn't much we can
             * do. It's no worse than XMLSchemaLoader when used as an XSLoader.
             */
            XSObjectList annotations = newGrammar.getAnnotations();
            length = annotations.getLength();
            for (int i = 0; i < length; ++i) {
                cachedGrammar.addAnnotation((XSAnnotationImpl) annotations.item(i));
            }
            
        }
        
        public boolean containsGrammar(XMLGrammarDescription desc) {
            return inXmlValidationState ? super.containsGrammar(desc) : null;
        }
        
        public Grammar getGrammar(XMLGrammarDescription desc) {
            return inXmlValidationState ? super.getGrammar(desc) : null;
        }
        
        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            return getGrammar(desc);
        }
        
        public Grammar [] retrieveInitialGrammarSet (String grammarType) {
            return new Grammar[0];
        }
    }    
    
    
}
