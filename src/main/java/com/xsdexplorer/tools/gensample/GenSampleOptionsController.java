package com.xsdexplorer.tools.gensample;

import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSObject;

import com.xsdexplorer.tools.gensample.GenSample.GenSampleBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;

public class GenSampleOptionsController {
    
    @FXML
    private TextField repeatedCountText;

    @FXML
    private CheckBox optCheckBox;
    
    @FXML
    private CheckBox repeatedChoiceCreateAllBox;

    @FXML
    private TreeView<TreeViewRecord> treeView;
    
    @FXML
    private Button browseBtn;
    
    @FXML
    private TextField outputFilename;
    
    private XSModel model;
    
    @FXML
    private void initialize() {
        TextFormatter<Integer> formatter = new TextFormatter<>(
                new IntegerStringConverter(), 
                1,  
                c -> Pattern.matches("\\d*", c.getText()) ? c : null );
        repeatedCountText.setTextFormatter(formatter);
        
        /*
         final Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
         okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            if (!isValid()) {
                ae.consume(); //not valid
            }
        });
         */
    }
    
    @FXML
    private void onBrowserBtn() {
        FileChooser fileChooser = new FileChooser();
        //if (lastFile != null)
          //  fileChooser.setInitialDirectory(lastFile.getParentFile());
        //fileChooser.setInitialFileName("myfile.txt");
        
        fileChooser.getExtensionFilters().addAll(
                 new FileChooser.ExtensionFilter("XSD Files", "*.xsd")
                ,new FileChooser.ExtensionFilter("Zip Files", "*.zip")
        );
        //fileChooser.se
        
        File selectedFile = fileChooser.showSaveDialog(browseBtn.getScene().getWindow());
        if (selectedFile != null) {
            outputFilename.setText(selectedFile.getAbsolutePath());
        }        
    }
    
    public GenSample.GenSampleBuilder getOptions() {
        GenSampleBuilder options = new GenSample.GenSampleBuilder();
        options.setRepeatedCount(Integer.valueOf(repeatedCountText.getText()))
            .setCreateOptional(optCheckBox.isSelected())
            .setRepeatedChoiceCreateAll(repeatedChoiceCreateAllBox.isSelected());
        
        TreeItem<TreeViewRecord> item = treeView.getSelectionModel().getSelectedItem();
        options.setRoot((XSElementDeclaration) item.getValue().xsObject);
        return options;
    }
    
    public String getOutputFilename() {
        return outputFilename.getText();
    }
    
    public void createGlobalsSelectTree(XSModel model, XSElementDeclaration root) {
        this.model = model;
        TreeView<TreeViewRecord> treeView = this.treeView;// = new TreeView<>();
        //initCellFactloty();
        TreeItem<TreeViewRecord> rootItem = new TreeItem<>(new TreeViewRecord("Globals"));
        treeView.setRoot(rootItem);
        rootItem.setExpanded(true);
        //treeView.setShowRoot(false);
        //treeView.setPrefHeight(200);

        //treeView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked); 
        StringList namespaces = model.getNamespaces();
        //boolean includeSchemaNs = namespaces.size() == 1; //show xsd namespace for schema of schema only
        for (int i = 0; i < namespaces.size(); ++i) {
            String ns = (String) namespaces.get(i);
            //if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(ns) && !includeSchemaNs) {
            //    continue;
            //}            
            List<XSObject> elements = getComponentsByNamespace(ELEMENT_DECLARATION, ns).stream()
                    .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).toList();
            
            if (elements.isEmpty()) {
                continue;
            }            
            TreeItem<TreeViewRecord> nsItem = new TreeItem<>(new TreeViewRecord(ns == null || ns.isEmpty() ? "<no namespace>" : ns));
            nsItem.setExpanded(true);
            rootItem.getChildren().add(nsItem);
            
            for (XSObject o : elements) {
                TreeItem<TreeViewRecord> treeItem = new TreeItem<>(new TreeViewRecord(o.getName(), o));
                nsItem.getChildren().add(treeItem);
                if (o == root) {
                    treeView.getSelectionModel().select(treeItem);
                }
            }
        }
        outputFilename.setText(root.getName()+".xml");
    
        //TreeItemSelectionFilter<Object> filter = TreeItem::isLeaf;
        //FilteredTreeViewSelectionModel<Object> filteredSelectionModel = new 
        //FilteredTreeViewSelectionModel<>(tree, selectionModel, filter);
        //tree.setSelectionModel(filteredSelectionModel);        
        
    }

    @SuppressWarnings("unchecked")
    private Collection<XSObject> getComponentsByNamespace(short ctype, String ns) {
        return model.getComponentsByNamespace(ctype, ns).values();
    }
    
    private static class TreeViewRecord {
        String name;
        XSObject xsObject;
        
        public TreeViewRecord(String name) {
            this.name = name;
        }
        public TreeViewRecord(String name, XSObject xsObject) {
            this.name = name;
            this.xsObject = xsObject;
        }
        @Override
        public String toString() {
            return name;
        }
    }    
}
