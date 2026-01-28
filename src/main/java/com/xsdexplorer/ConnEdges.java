package com.xsdexplorer;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class ConnEdges {
    public static final double EDGE_LEN = 15;
    private Line hLine;
    private Line vLine;
    private Line[] childHLines;
    
    public ConnEdges(XsdTreeNode n) {
        ObservableList<Node> children = n.getParentLayout().getChildren();
        BooleanProperty visibleProperty = n.firstChild().visibleProperty();        
        
        hLine = confLine(n.isOptional(), visibleProperty, children);
        
        final int childNum = n.childNodes().size();
        if (childNum > 1) {
            vLine = confLine(n.allChildrenOptional(), visibleProperty, children);
            
            childHLines = new Line[childNum];
            int i = 0;
            for (TreeNodeControl ch : n.childNodes()) {
                Line l = childHLines[i++] = confLine(ch.isOptional(), visibleProperty, children);
                l.setEndX(EDGE_LEN);
            }
        }
    }
    
    private static Line confLine(boolean dashed, BooleanProperty visibleProperty, ObservableList<Node> children) {
        Line l = new Line();
        l.setStroke(Color.BLACK);
        if (dashed) {
            l.getStrokeDashArray().addAll(2d, 4d);
        }
        l.visibleProperty().bind(visibleProperty);
        children.add(l);
        return l;
    }
    
    public void layoutEdges(XsdTreeNode node, double labelHeight) {
        double hLineParentEndX; //vLine in multi-child or label start single child 
        if (vLine != null) {
            //only node with multi child nodes ( > 1) has vLine 
            Bounds chFirst = node.firstChild().getBoundsInParent();
            Bounds chLast = node.lastChild().getBoundsInParent();
            double height = chLast.getMinY() - chFirst.getMinY();
            vLine.setEndY(height);
            double vLineX = chFirst.getMinX() - EDGE_LEN; 
            vLine.relocate(vLineX, chFirst.getMinY() + labelHeight/2);
            for (int i = 0; i<childHLines.length; ++i) {
                Bounds b = node.childNodes.get(i).getBoundsInParent();
                childHLines[i].relocate(vLineX, b.getMinY() + labelHeight/2);
            }
            
            hLineParentEndX = vLineX;
        }
        else {
            Bounds b = node.firstChild().getBoundsInParent();
            hLineParentEndX = b.getMinX();
        }
        //hLine up to vLine or single child
        Bounds b = node.getBoundsInParent();
        double startX = b.getMinX() + node.getLabel().getWidth();
        hLine.setEndX(hLineParentEndX - startX);
        hLine.relocate(startX, b.getMinY() + labelHeight/2);

    }
    
}
