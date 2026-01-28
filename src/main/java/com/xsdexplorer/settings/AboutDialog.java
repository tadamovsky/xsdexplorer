package com.xsdexplorer.settings;


import com.xsdexplorer.XsdExplorer;
import com.xsdexplorer.uihelpers.Utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;

public class AboutDialog {

    public void showAboutDialog() {
        Dialog<Void> d = new Dialog<>();
        
        d.initStyle(StageStyle.UTILITY);
        //d.initOwner(app.g);
        d.setTitle("About Xsd Explorer");
        
        HBox root = new HBox(40);
        root.setAlignment(Pos.TOP_LEFT);
        //hbox.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(15, 25, 15, 2));
        ImageView pic = new ImageView(new Image(AboutDialog.class.getResourceAsStream("/icon1.png")));
        //HBox.setMargin(pic, new Insets(2, 0, 2, 0));
        
        VBox details = createDetails();
        root.getChildren().addAll(pic, details);
        //pic.setFitWidth(130);
        //pic.setFitHeight(130);
        d.getDialogPane().setContent(root);
        var okBtnType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);

        d.getDialogPane().getButtonTypes().addAll(okBtnType);
        d.showAndWait();
        
    }
    
    private VBox createDetails() {
        VBox details = new VBox(10);
        Text title = new Text("Xsd Explorer "+Utils.getVersion());
        //annot.setTextAlignment(TextAlignment.LEFT);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 1.5em");
        details.getChildren().add(title);
        //VBox.setMargin(annot, new Insets(6, 0, 0, 0));
        
        Hyperlink link = new Hyperlink("xsdexplorer.com");
        details.getChildren().add(link);
        link.setOnAction(e -> {
            XsdExplorer.getApp().getHostServices().showDocument("https://xsdexplorer.com");
        });
        link = new Hyperlink("Contact the author");
        details.getChildren().add(link);
        link.setOnAction(e -> {
            XsdExplorer.getApp().getHostServices().showDocument("mailto:tadamovsky@gmail.com");
        });
        
        
        return details;
    }
 
    
    
}