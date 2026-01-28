package com.xsdexplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSObjectList;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


public class AttributeNode extends TreeNodeControl {
	private TitledPane attPane = new TitledPane();
	private MyVBox content = new MyVBox();
	
	//private MyTimerTask lastTask = new MyTimerTask();
	
	public AttributeNode(List<XSAttributeUse> attrs, XsdTreeView xsdTreeView) {
		attPane.setText("attributes");
		attPane.setExpanded(false);
		for (XSAttributeUse attr : attrs) {
			Label label = new Label(attr.getAttrDeclaration().getName());
			label.getStyleClass().addAll("attrNodeLabel", attr.getRequired() ? "requiredShape" : "optionalShape");
			content.addAttr(label);
            label.setFocusTraversable(true);
            label.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    label.requestFocus();
                    event.consume();
                }
            });
            
            
            label.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (newValue) {
                    xsdTreeView.onNodeFocused(attr);
                }
            });   

            
			XSObjectList annotations = attr.getAnnotations();
			if (!annotations.isEmpty()) {
				XSAnnotation xsAnnot = (XSAnnotation) annotations.get(0);
				String text = AnnotationExtractor.extract(xsAnnot.getAnnotationString());
				if (text != null) {
					Text annot = new Text(text);
					//annot.setStyle("-fx-font-size: 0.8em");
					annot.getStyleClass().add("annotation");
					content.addAnnotation(annot);
				}
			}
		}
		attPane.setContent(content);
		attPane.setAnimated(false);
		//make label width take its pref width on collapse
		content.setManaged(false);
		content.managedProperty().bind(attPane.expandedProperty()); //works but cancels transition effect
		xsdTreeView.addNode(this);
		//to support transition effect:
		/*
		Timer t = new Timer();
		attPane.expandedProperty().addListener((o, old, newV) -> {
			if (newV) {
				content.setManaged(true);
				lastTask.cancel();
			} else {
				lastTask = new MyTimerTask();
				t.schedule(lastTask, 350); //TitledPaneSkin.TRANSITION_DURATION=350
			}
		});
		*/
	}
	
	@Override
	Region control() {
		return attPane;
	}

	@Override
	boolean isOpen() {
		return false;
	}

	@Override
	boolean isOptional() {
		return false;
	}

	@SuppressWarnings("unused")
    private class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			content.setManaged(false);
		}
	}
	
    private static class MyVBox extends VBox {
    	
    	List<Node> attributes = new ArrayList<>();
    	List<Text> annotations = new ArrayList<>();

    	MyVBox() {
    		super(8);
    		//default alignment is TOP_LEFT
    		setPadding(new Insets(8, 8, 8, 8));
    	}
    	
    	
    	void addAttr(Label label) {
    		getChildren().add(label);
    		attributes.add(label);
    	}
    	
		void addAnnotation(Text annot) {
			getChildren().add(annot);
    		annotations.add(annot);
    	}
    	

    	@Override
    	protected double computeMinWidth(double height) {
    		fixAnnotationWrap();
    		return super.computeMinWidth(height);
    	}
    	
    	@Override
    	protected double computePrefWidth(double height) {
    		fixAnnotationWrap();
    		return super.computePrefWidth(height);
    	}
    	
    	//can only be called during layout pass
    	private void fixAnnotationWrap() {
    		if (!annotations.isEmpty()) {
    			double maxWidth = attributes.stream().mapToDouble(n -> n.prefWidth(-1)).max().getAsDouble();
    			double prefWidth = Double.max(XsdTreeNode.ANNOTATION_WRAP, maxWidth);
    			for (Text annot : annotations) {
    				annot.setWrappingWidth(prefWidth);
    			}
    		}
    	}

    }

    @Override
    void expand(boolean force) {
        
    }	
	
}
