package com.xsdexplorer.tools.gensample;

import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;

import com.xsdexplorer.ExternalEditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.StageStyle;

public class GenSampleRunner { 

    public void run(XSModel model, XSElementDeclaration root) {
        if (root == null) { //no element is currently selected
            XSNamedMap map = model.getComponents(ELEMENT_DECLARATION);
            if (map.isEmpty()) {
                showNoElementsError();
                return;
            }
            root =  (XSElementDeclaration) map.item(0);
        }
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("GenSampleOptions.fxml"));
        try {
            DialogPane pane = loader.load();
            Dialog<ButtonType> d = new Dialog<>();
            d.initStyle(StageStyle.UTILITY);
            d.setDialogPane(pane);
            d.setTitle("Generate sample");
            GenSampleOptionsController c = loader.getController();
            c.createGlobalsSelectTree(model, root);
            
            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, ae -> {
                if (!isValid(c)) {
                    ae.consume(); //not valid
                }
            });
            
            Optional<ButtonType> ret = d.showAndWait();
            if (ret.isPresent() && ret.get() == ButtonType.OK) {
                FileWriter writer = createWriter(c.getOutputFilename());
                if (writer == null)
                    return;
                try {
                    GenSample gen = c.getOptions().build(model, writer);
                    gen.generate();
                } finally {
                    writer.close();
                }
                new ExternalEditor().openEditor(new File(c.getOutputFilename()), 0);
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean isValid(GenSampleOptionsController c) {
        if (c.getOptions().root == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No root element selected");
            alert.setHeaderText("Please select root element");
            alert.showAndWait();        
            return false;
        }
        return true;
    }
    
    private void showNoElementsError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No global elements");
        alert.setHeaderText("Cannot generate sample file");
        alert.setContentText("Schema has no global elements defined");
        alert.showAndWait();        
    }

    FileWriter createWriter(String path) {
        try {
            return new FileWriter(path);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid output file");
            alert.setHeaderText("Error creating file "+path);
            //alert.setContentText(path);
            alert.showAndWait();        
            return null;
        }
        
    }
}
