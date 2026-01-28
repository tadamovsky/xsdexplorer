package com.xsdexplorer.settings;


import java.io.File;

import com.xsdexplorer.ExternalEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SettingsDialog {

    private final TextField textEdit = new TextField();

    public void showSettingsDialog(Stage primaryStage) {
        Dialog<Void> d = new Dialog<>();
        
        d.initStyle(StageStyle.UTILITY);
        d.setTitle("Settings");
        
        VBox root = new VBox();
        root.setPadding(new Insets(16));
        d.getDialogPane().setContent(root);
        
        HBox hbox = createEditorPanel(primaryStage);
        root.getChildren().add(hbox);
        
        var okBtnType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        var closeBtnType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        d.getDialogPane().getButtonTypes().addAll(okBtnType, closeBtnType);
        d.setResultConverter(b -> {
            if (b == okBtnType) {
                ExternalEditor.setCurrentEditor(textEdit.getText());
            }
            return null;
        });
        
        d.showAndWait();
        
    }
    
    private HBox createEditorPanel(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        String currString = ExternalEditor.getCurrentEditor();
        File curr = new File(currString);
        File parent = curr.getParentFile();
        if (parent != null)
            fileChooser.setInitialDirectory(parent);
        fileChooser.setInitialFileName(curr.getName());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable", "*.exe"));
        
        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.BASELINE_CENTER);
        var editorLbl = new Label("External editor:");
        textEdit.setText(currString);
        Button button = new Button("Browse");
        button.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                textEdit.setText(selectedFile.getAbsolutePath());
            }
        });
        hbox.getChildren().addAll(editorLbl, button, textEdit);
        
        return hbox;
    }
    
    
    
    
}