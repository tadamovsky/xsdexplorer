package com.xsdexplorer;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public abstract class TreeNodeControl {

	abstract Region control();
	abstract boolean isOpen();
	abstract boolean isOptional();

	abstract void expand(boolean force);


	List<TreeNodeControl> childNodes = Collections.emptyList();
	final List<TreeNodeControl> childNodes() {
		return childNodes;
	}
	
	final boolean hasSingleChild() {
		return childNodes.size() == 1;
	}

	final Region firstChild() {
		return  childNodes.get(0).control();
	}
	final Region lastChild() {
		return  childNodes.get(childNodes.size() - 1).control();
	}
	//final Stream<Node> nodeStream() {
	//	return childNodes().stream().map(n -> n.control());
	//}
	
	//below wrap Region methods
	final double prefWidth() {
		return control().prefWidth(-1);
	}
	final double prefHeight() {
		return control().prefHeight(-1);
	}

	final double getWidth() {
		return control().getWidth();
	}
	final double getHeight() {
		return control().getHeight();
	}

	final void setVisible(boolean value) {
		control().setVisible(value);
	}
	final boolean isVisible() {
		return control().isVisible();
	}
	final Bounds getBoundsInParent() {
		return control().getBoundsInParent();
	}
	
	final void autosize() {
		 control().autosize();
	}

	final void relocate(double x, double y) {
		control().relocate(x, y);
	}
	
	Pane getTypeRectange() {
		return null;
	}
	
    public List<TreeNodeControl> getChildrenForLayout() {
        return childNodes;
    }	

    //used in XsdTreeNode
    public void layoutSubstEdges(double labelHeight) {
    }
    
    //used in XsdTreeNode
    public void layoutEdges(double labelHeight) {
    }
    
}
