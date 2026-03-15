package com.xsdexplorer.settings;

import com.xsdexplorer.VersionChecker;
import com.xsdexplorer.XsdExplorer;
import com.xsdexplorer.uihelpers.Utils;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog to notify user about available version updates.
 */
public class VersionUpdateDialog {
    
    public void showVersionUpdateDialog(String newVersion) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("XSD Explorer Update Available");
        
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(15));
        
        Label titleLabel = new Label("New Version Available");
        titleLabel.setStyle("-fx-font-size: larger; -fx-font-weight: bold;");
        
        Label messageLabel = new Label("A new version of XSD Explorer is available:");
        
        Label versionLabel = new Label("Version: " + newVersion);
        versionLabel.setStyle("-fx-font-size: larger; -fx-font-weight: bold; -fx-text-fill: #0066cc;");
        
        Label descriptionLabel = new Label(
            "You are currently running version " + Utils.getVersion() + ".\n" +
            "Update to the latest version to get the latest features and bug fixes."
        );
        descriptionLabel.setWrapText(true);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button downloadButton = new Button("Download Now");
        downloadButton.setPrefWidth(120);
        downloadButton.setOnAction(e -> {
            openDownloadPage();
            dialogStage.close();
        });
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> dialogStage.close());
        
        Button dontShowButton = new Button("Don't Show Again");
        dontShowButton.setPrefWidth(120);
        dontShowButton.setOnAction(e -> {
            VersionChecker.setStartupCheckEnabled(false);
            dialogStage.close();
        });
        
        buttonBox.getChildren().addAll(downloadButton, dontShowButton, cancelBtn);
        
        mainBox.getChildren().addAll(
            titleLabel,
            new Separator(),
            messageLabel,
            versionLabel,
            descriptionLabel,
            buttonBox
        );
        
        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        
        Scene scene = new Scene(scrollPane, 400, 250);
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.show();
    }
    
    private void openDownloadPage() {
        String url = "https://github.com/tadamovsky/xsdexplorer/releases";
        XsdExplorer.getApp().getHostServices().showDocument(url);
    }
    
}