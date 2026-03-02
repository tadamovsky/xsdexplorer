package com.xsdexplorer.tools.flatten;


import java.io.File;

import org.apache.xerces.xs.XSModel;

import com.xsdexplorer.loader.XsdInfoLoader;
import com.xsdexplorer.serialize.SchemaSerializer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FlattenDialog {

    private final TextField textEdit = new TextField();
    private XSModel model;
    private XsdInfoLoader xsdInfo;

    public FlattenDialog(XSModel model, XsdInfoLoader xsdInfo) {
        this.model = model;
        this.xsdInfo = xsdInfo;
    }

    public void showFlattenDialog(Stage primaryStage) {
        Dialog<Void> d = new Dialog<>();
        
        d.initStyle(StageStyle.UTILITY);
        d.setTitle("Flatten Schema");
        
        VBox root = new VBox();
        root.setPadding(new Insets(16));
        root.setSpacing(30);
        root.setPrefWidth(520);
        d.getDialogPane().setContent(root);
        
        Label lbl = new Label("This will create a flattened version of the current schema - for each namespace, a single file will be created containing all the components in that namespace.");
        lbl.setWrapText(true);
        lbl.setMaxWidth(500);
        root.getChildren().add(lbl);
        
        HBox hbox = createFileSelectPanel(primaryStage);
        root.getChildren().add(hbox);
        
        var okBtnType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        var closeBtnType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        d.getDialogPane().getButtonTypes().addAll(okBtnType, closeBtnType);
        d.setResultConverter(b -> {
            if (b == okBtnType) {
                String path = textEdit.getText();
                if (checkOutputDir(path)) {
                    new SchemaSerializer(model, path, xsdInfo).serialize();
                }
            }
            return null;
        });
        
        d.showAndWait();
        
    }
    
    private boolean checkOutputDir(String path) {
        if (path.isEmpty() || !new File(path).isDirectory()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Directory not valid");
            alert.setHeaderText("Please select valid target directory");
            alert.showAndWait();        
            return false;
        }
        return true;
    }

    private HBox createFileSelectPanel(Stage primaryStage) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        
        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER_LEFT);
        var editorLbl = new Label("Target directory:");
        Button button = new Button("Browse");
        button.setOnAction(e -> {
            File selectedFile = fileChooser.showDialog(primaryStage);
            if (selectedFile != null) {
                textEdit.setText(selectedFile.getAbsolutePath());
            }
        });
        //textEdit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textEdit, Priority.ALWAYS);
        hbox.getChildren().addAll(editorLbl, button, textEdit);
        
        return hbox;
    }
    
    
    
    
}