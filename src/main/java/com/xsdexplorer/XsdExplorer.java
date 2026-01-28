package com.xsdexplorer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.XMLSchemaValidator;
import org.apache.xerces.parsers.XML11Configuration;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;

import com.xsdexplorer.LogView.Kind;
import com.xsdexplorer.XsdTreeView.Options;
import com.xsdexplorer.loader.EntityResolverImpl;
import com.xsdexplorer.loader.RootSelector;
import com.xsdexplorer.loader.SchemaLoader;
import com.xsdexplorer.loader.SchemaLoaderTask;
import com.xsdexplorer.settings.AboutDialog;
import com.xsdexplorer.settings.SettingsDialog;
import com.xsdexplorer.tools.gensample.GenSampleRunner;
import com.xsdexplorer.uihelpers.ZoomableScrollPane;

import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class XsdExplorer extends Application {

    private XSModel model;
    private BorderPane root;
    private XsdTreeView xsdTreeView;

    private Preferences prefs = Preferences.userNodeForPackage(XsdExplorer.class);
    private RecentItems recentItems = new RecentItems(10, prefs);
    private File lastFile;
    
    //extracted from schema loader
    private XMLGrammarPool pool;
    private SymbolTable symbolTable; 
    private boolean isXsd11;
    
    private LogView logView;
    
    private SimpleBooleanProperty schemaLoaded = new SimpleBooleanProperty(false);
    
    private static XsdExplorer app;
    
    
    public LogView getLogView() {
        return logView;
    }

    public Preferences getPrefs() {
        return prefs;
    }

    @Override
    public void start(Stage stage) {
        app = this;
        stage.getIcons().add(new Image(XsdExplorer.class.getResourceAsStream("/icon.png")));        
        
        xsdTreeView = new XsdTreeView();

        ZoomableScrollPane scrollPane = new ZoomableScrollPane(xsdTreeView);
        scrollPane.setPadding(new Insets(XsdTreeView.VSPACE, 16, 0, 16));
        scrollPane.fitToHeightProperty().set(true);
        //scrollPane.fitToWidthProperty().set(true);
        scrollPane.pannableProperty().set(true);
        scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        //scrollPane.setContent(xsdTreeView); //root
        
        scrollPane.setOnDragOver(event -> {
            if (event.getGestureSource() != scrollPane
                    && event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                if (acceptFiles(files))
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();        
        });
        scrollPane.setOnDragDropped(this::onDragDropped);
        
        //xsdTreeView.setOnNodeExpandedConsumer(n -> Platform.runLater(() -> scrollPane.centerNodeInScrollPane(n.control())));
        
 
        root = new BorderPane();
        root.setTop(createMenuBar(xsdTreeView.getOptions(), stage));
        root.setCenter(scrollPane);
        root.setBottom(createLogView());
        
        var scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Xsd Explorer");
        stage.setScene(scene);
        stage.show();
    }

    //accept xsd/xml/zip/directory
    private static final Pattern acceptFilesPattern = Pattern.compile("\\.(xsd|xml|zip)$");
    private static boolean acceptFiles(List<File> files) {
        return files.stream().allMatch(f -> f.isDirectory() || acceptFilesPattern.matcher(f.getName()).find());
    }
    
    private Node createLogView() {
        logView = new LogView();
        return logView.createLogView();
    }

	private MenuBar createMenuBar(Options o, Stage stage) {
		Menu fileMenu = new Menu("File");
		Menu viewMenu = new Menu("View");
        Menu toolsMenu = new Menu("Tools");
		Menu helpMenu = new Menu("Help");

		// Create MenuItems
		// MenuItem newItem = new MenuItem("New");
		MenuItem openFileItem = new MenuItem("_Open...");
		openFileItem.setOnAction(value -> onOpenFile());
		openFileItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+O"));		
		
        MenuItem openDirItem = new MenuItem("_Open Directory...");
        openDirItem.setOnAction(value -> onOpenDir());
		
		
		MenuItem exitItem = new MenuItem("_Exit");
		exitItem.setOnAction(value -> stage.close());

		Menu subMenu = new Menu("Show in diagram");
		CheckMenuItem attItem = new CheckMenuItem("attributes");
		attItem.setSelected(o.showAttributes.get());
		attItem.setOnAction(value -> o.showAttributes.set(attItem.isSelected()));
		CheckMenuItem annotItem = new CheckMenuItem("annotations");
		annotItem.setSelected(o.showAnnotations.get());
		annotItem.setOnAction(value -> o.showAnnotations.set(annotItem.isSelected()));
		CheckMenuItem typeItem = new CheckMenuItem("types");
		typeItem.setSelected(o.showTypes.get());
		typeItem.setOnAction(value -> o.showTypes.set(typeItem.isSelected()));
		subMenu.getItems().addAll(attItem, annotItem, typeItem);

		// Add menuItems to the Menus
		fileMenu.getItems().addAll(openFileItem, openDirItem, new SeparatorMenuItem(), exitItem);
		recentItems.addObserver(val -> addMRecentFiles(fileMenu, 3));
		addMRecentFiles(fileMenu, 3);
		viewMenu.getItems().addAll(subMenu);

        MenuItem settingsDialog = new MenuItem("Settings");
        settingsDialog.setOnAction(value -> new SettingsDialog().showSettingsDialog(stage));
        
        MenuItem genSample = new MenuItem("Generate Sample XML");
        genSample.setOnAction(valuue -> new GenSampleRunner().run(model, xsdTreeView.getRootElementDecl()));
        genSample.disableProperty().bind(schemaLoaded.not());        
        
        toolsMenu.getItems().addAll(genSample, new SeparatorMenuItem(), settingsDialog);
		
        MenuItem helpItem = new MenuItem("_Help");
        helpItem.setOnAction(value -> onOpenHelpFile());
        helpItem.setAccelerator(KeyCombination.keyCombination("F1"));  
        
        MenuItem aboutItem = new MenuItem("About ...");
        aboutItem.setOnAction(value -> new AboutDialog().showAboutDialog());
        helpMenu.getItems().addAll(helpItem, aboutItem);
        
		MenuBar menuBar = new MenuBar(fileMenu, viewMenu, toolsMenu, helpMenu);
		return menuBar;
	}
	
	private void onOpenHelpFile() {
        String url = "https://www.xsdexplorer.com/";
        getHostServices().showDocument(url);
        /*
	    WebView webView = new WebView();
	    WebEngine webEngine = webView.getEngine();
	    webEngine.load(url);
	    
	     Scene scene = new Scene(webView, 900, 700);
	     Stage stage = new Stage();
	     stage.setTitle("Help");
	     stage.setScene(scene);
	     stage.show();
	     */	    
	}

    private void addMRecentFiles(Menu fileMenu, int recentIndex) {
	    fileMenu.getItems().subList(recentIndex, fileMenu.getItems().size()).clear();
	    List<String> items = recentItems.getItems();
	    if (items.isEmpty())
	        return;
	    fileMenu.getItems().add(new SeparatorMenuItem());
	    for (int i = 0; i < items.size(); i++) {
            String s = items.get(i);
            final File f = new File(s);
            if (f.exists()) {
                MenuItem fItem = new MenuItem(StringUtils.abbreviateMiddle(String.format("%d: %s", i+1, s), "\u2026", 40)); //ellipsis ...
                fileMenu.getItems().add(fItem);
                fItem.setOnAction(value -> onLastFileUpdated(f));
            }
        }
	}
	
    public void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            Map<Boolean, List<File>> filesMap = db.getFiles().stream().collect(Collectors.groupingBy(f -> f.getName().endsWith(".xml")));
            List<File> xsdFiles = filesMap.get(false);
            if (xsdFiles != null && !xsdFiles.isEmpty())
                onLastFileUpdated(xsdFiles.toArray(File[]::new));
            List<File> xmlFiles = filesMap.get(true);
            if (xmlFiles != null) {
                for (File f :  xmlFiles) {
                    validateXml(f);
                }
            }
            success = true;
        }
        /* let the source know whether the string was successfully 
         * transferred and used */
        event.setDropCompleted(success);

        event.consume();    
    }	
	
    private void validateXml(File f) {
        
        XMLParserConfiguration config = new MyXML11Configuration(symbolTable, pool);//new XIncludeAwareParserConfiguration();
        //String USE_GRAMMAR_POOL_ONLY =
        //        Constants.XERCES_FEATURE_PREFIX + Constants.USE_GRAMMAR_POOL_ONLY_FEATURE;
        //config.setFeature(USE_GRAMMAR_POOL_ONLY, true);
        config.setEntityResolver(new EntityResolverImpl());
        config.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE, true);
        config.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING, true);
        config.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE, true);
        MyErrorHandler errorHandler = new MyErrorHandler();
        config.setErrorHandler(errorHandler);
        System.out.println("Validating "+f);
        try {
            config.parse(new XMLInputSource(null, f.getAbsolutePath(), null));
            //System.out.println("Validation successfull!");
            if (!errorHandler.hasErrors)
                logView.addMessage(Kind.SUCCESS, f+": validation successfull!");
        } catch (XNIException e) {
            //e.printStackTrace();
            if (!errorHandler.fatalError) //if fatal, already added
                logView.addMessage(Kind.ERROR, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
     }

    //has to extend XML11Configuration to set schema version,
    //without it, xsd1.1. assertions are not executed
    private class MyXML11Configuration extends XML11Configuration {
        public MyXML11Configuration(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
            super(symbolTable, grammarPool, null);
        } 

        @Override
        public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
            if (isXsd11 && propertyId.equals(SCHEMA_VALIDATOR)) {
                XMLSchemaValidator val = (XMLSchemaValidator) value;
                val.setProperty(SchemaLoader.XML_SCHEMA_VERSION, Constants.W3C_XML_SCHEMA11_NS_URI);
            }
            super.setProperty(propertyId, value);
        }
    }
    
    private class MyErrorHandler implements XMLErrorHandler {
        boolean hasErrors;
        boolean fatalError;

        @Override
        public void warning(String domain, String key, XMLParseException e) throws XNIException {
            System.out.println("Warning: "); 
            printInfo(e);
        }

        @Override
        public void error(String domain, String key, XMLParseException e) throws XNIException {
            System.err.println("Error: "); 
            printInfo(e);
        }

        @Override
        public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
            System.err.println("Fatal error: "); 
            fatalError = true;
            printInfo(e);
        }
        private void printInfo(XMLParseException e) {
            hasErrors = true;
            System.err.println("   Public ID: "+e.getPublicId());
            System.err.println("   System ID: "+e.getExpandedSystemId());
            System.err.println("   Line number: "+e.getLineNumber());
            System.err.println("   Column number: "+e.getColumnNumber());
            System.err.println("   Message: "+e.getMessage());
            String systemId = e.getExpandedSystemId();
            if (systemId.startsWith("file://"))
                systemId = new File(systemId.substring(7)).getAbsolutePath();
            logView.addMessage(Kind.ERROR, systemId + ": line "+e.getLineNumber()+", column "+e.getColumnNumber()+": "+e.getMessage());
        }        
    }
    
    private void onOpenFile() {

        FileChooser fileChooser = new FileChooser();
        if (lastFile != null)
            fileChooser.setInitialDirectory(lastFile.getParentFile());
        //fileChooser.setInitialFileName("myfile.txt");
        
        fileChooser.getExtensionFilters().addAll(
                 new FileChooser.ExtensionFilter("XSD Files", "*.xsd")
                ,new FileChooser.ExtensionFilter("Zip Files", "*.zip")
        );
        
        File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (selectedFile != null) {
            onLastFileUpdated(selectedFile);
        }
    }
    
    private void onOpenDir() {
        DirectoryChooser fileChooser = new DirectoryChooser();
        if (lastFile != null)
            fileChooser.setInitialDirectory(lastFile.getParentFile());

        File selectedFile = fileChooser.showDialog(root.getScene().getWindow());
        if (selectedFile != null) {
            onLastFileUpdated(selectedFile);
        }
    }    
    
    private void onLastFileUpdated(File... files) {
        logView.clear();
        root.getScene().setCursor(Cursor.WAIT);
        SchemaLoaderTask loaderTask = new SchemaLoaderTask(files);
        loaderTask.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String msg) -> { 
            int k = msg.indexOf(']');
            String kind = msg.substring(1, k);
            msg = msg.substring(k + 1);
            logView.addMessage(Kind.valueOf(kind), msg);
        });
        loaderTask.setOnSucceeded((e) -> {
            for (int i = 0; i < files.length; i++) {
                recentItems.push(files[i].getAbsolutePath());
                lastFile = files[i];
            }
            SchemaLoader schemaLoader = loaderTask.getValue();
            model = schemaLoader.getXSModel();
            pool = schemaLoader.getPool();
            symbolTable = schemaLoader.getSymbolTable();
            isXsd11 = schemaLoader.isXsd11();
            RootSelector r = new RootSelector(model);
            XSElementDeclaration rootDecl = r.selectRoot();
            GlobalsToolbar globalsToolbar = new GlobalsToolbar(model, (xsterm -> xsdTreeView.setRootTerm(xsterm, model) ));
            root.setRight(globalsToolbar.createToolBar());
            xsdTreeView.setOnNodeFocusConsumer(node -> globalsToolbar.populatePropsFor(node));

            xsdTreeView.setRootTerm(rootDecl, model);

            schemaLoaded.set(true);
            root.getScene().setCursor(Cursor.DEFAULT);

        });
        loaderTask.setOnFailed((event) -> {
            root.getScene().setCursor(Cursor.DEFAULT);
            Throwable e = loaderTask.getException();
            String message = fixMessage(e);
            logView.addMessage(Kind.ERROR, message);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid xsd file");
            alert.setHeaderText("Error loading file");
            alert.setContentText(message);
            alert.showAndWait();
            schemaLoaded.set(false);            
        });
        Thread th = new Thread(loaderTask);
        th.setDaemon(true);
        th.start();
    }

    private static String fixMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            e.printStackTrace();
            return e.toString();
        }
        int i = message.indexOf("[Fatal Error]", 1); //fix chaining of [Fatal Error] somewhere is xerces, getting errors like this: [Fatal Error]: -1:-1:-1, -1: [Fatal Error]: 23:3:-1, 571:...
        return i != -1 ? message.substring(i) : message;
    }

    public static XsdExplorer getApp() {
        return app;
    }
    

    public static void addLogViewMessage(Kind kind, String message) {
        if (app != null) {
            app.getLogView().addMessage(kind, message);
        }
    }
    
    public static void main(String[] args) {
       launch();
    }


}

