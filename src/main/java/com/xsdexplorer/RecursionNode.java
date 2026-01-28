package com.xsdexplorer;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.util.Duration;

//placeholder for recursion nodes
public class RecursionNode extends TreeNodeControl {
    
    private Label control = new Label(" ... "); //...

    public RecursionNode(String descr, XsdTreeView xsdTreeView) {
        control.setPrefSize(10, xsdTreeView.getElementNodeHeight());
        control.setFocusTraversable(false);
        Tooltip tooltip = new Tooltip("Recusrion:\n"+descr);
        tooltip.setShowDelay(Duration.millis(10));
        tooltip.setShowDuration(Duration.millis(20000));
        control.setTooltip(tooltip);
        control.setPadding(new Insets(10, 0, 0, 0));
        control.setFont(NodeLabel.getFont());
        xsdTreeView.addNode(this);
    }
    
    @Override
    Region control() {
        return control;
    }

    @Override
    boolean isOpen() {
        return false;
    }

    @Override
    boolean isOptional() {
        return false;
    }

    @Override
    void expand(boolean force) {
    }
    

}
