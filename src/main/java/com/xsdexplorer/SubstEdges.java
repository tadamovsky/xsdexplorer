package com.xsdexplorer;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.VLineTo;

public class SubstEdges {
    public static final double SUBST_OFFSET = 7;
    
    private Path vLine; //need height and relocate to left bottom of abstract el node
    private MoveTo[] childHLines; //need y relative to 0
    
    private SubstEdges(XsdTreeNode n) {
        ObservableList<Node> children = n.getParentLayout().getChildren();
        children.add(createPath());
        
        List<XsdTreeNode> substGroup = n.getSubstGroup();
        final int childNum = substGroup.size(); //should be > 0
        childHLines = new MoveTo[childNum];
        
        for (int i = 0; i < childNum; ++i) {
            MoveTo m = childHLines[i] = new MoveTo(0, 0);
            LineTo l = new LineTo(SUBST_OFFSET, SUBST_OFFSET);
            l.setAbsolute(false);
            vLine.getElements().addAll(m, l);
        }
        vLine.visibleProperty().bind(n.control().visibleProperty());
    }
    
    public static SubstEdges createIfNeeded(XsdTreeNode n) {
        if (n.getSubstGroup().isEmpty())
            return null;
        return new SubstEdges(n);
    }
    
    private Path createPath() {
        MoveTo moveTo1 = new MoveTo(0, 0);
        VLineTo vLineTo = new VLineTo(100);
        MoveTo moveTo2 = new MoveTo(0, 0);
        LineTo l1 = new LineTo(-4, 4);
        MoveTo moveTo3 = new MoveTo(0, 0);
        LineTo l2 = new LineTo(4, 4);
        vLine = new Path(moveTo1, vLineTo, moveTo2, l1, moveTo3, l2);
        return vLine;
    }
    
    private void updatePathHeight(double height) {
        VLineTo vline = (VLineTo) vLine.getElements().get(1);
        vline.setY(height);
    }
    
    public void layoutSubstElements(XsdTreeNode node, double labelHeight) {
        List<XsdTreeNode> substGroup = node.getSubstGroup();
        final int childNum = substGroup.size();
        if (childNum == 0) {
            return;
        }
        Bounds parent = node.getBoundsInParent();
        vLine.relocate(parent.getMinX() - 5, parent.getMaxY() + 1);
        double yOffset = parent.getMaxY() + SUBST_OFFSET - labelHeight/2;
        for (int i = 0; i<childHLines.length; ++i) {
            XsdTreeNode subst = substGroup.get(i);
            Bounds b = subst.getBoundsInParent();
            childHLines[i].setY(b.getMinY() - yOffset);
        }
        
        Bounds last = substGroup.get(childNum - 1).getBoundsInParent();
        updatePathHeight(last.getMinY() - yOffset);
    }
    
}
