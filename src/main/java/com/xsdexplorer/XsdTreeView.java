package com.xsdexplorer;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSObject;

import com.xsdexplorer.uihelpers.Utils;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class XsdTreeView extends Pane  {
    public static final double VSPACE = 18;
    
    //type rect:
    //for single child node:
    //starts on edge end, add TYPE_H_ADD/2, child,  add TYPE_H_ADD/2
    //for multi child:
    //starts on edge end, add TYPE_H_ADD/2, vLine, edge, children, add TYPE_H_ADD/2
    private static final double TYPE_H_ADD = 18; //add 
    
	private boolean metricsDirty = true;
	private XSModel model;
	private TreeNodeControl root;
	private XsdTreeNode rootNode;
	private Options options = new Options();
	private int nodeCount = 0;
	private ContextMenu contextMenu = new ContextMenu();
	private Consumer<Object> onNodeFocused;
	private Consumer<TreeNodeControl> onNodeExpanded;
	private LinkedList<XSObject> navQueue = new LinkedList<>();
	
    public static class Options {
		BooleanProperty showAttributes = new SimpleBooleanProperty(true);
		BooleanProperty showAnnotations = new SimpleBooleanProperty(true);
		BooleanProperty showTypes = new SimpleBooleanProperty(true);
	}

	public XsdTreeView() {
		options.showAnnotations.addListener((o, oldV, newV) -> reset());
		options.showTypes.addListener((o, oldV, newV) -> reset());
		options.showAttributes.addListener((o, oldV, newV) -> reset());
		
		MenuItem menuItem1 = new MenuItem("Open with external editor");
		menuItem1.setOnAction(e -> {
		      MenuItem source = (MenuItem) e.getTarget();
		      ContextMenu popup = source.getParentPopup();
		      if (popup != null) {
		          Node ownerNode = popup.getOwnerNode();
		          if (ownerNode instanceof NodeLabel nodeLabel) {
		              nodeLabel.getXsdTreeNode().onOpenExternalEditor();
		          }
		      }
		});
		MenuItem copyXpathItem = new MenuItem("Copy XPath");
		copyXpathItem.setOnAction(e -> {
            MenuItem source = (MenuItem) e.getTarget();
            ContextMenu popup = source.getParentPopup();
            if (popup != null) {
                Node ownerNode = popup.getOwnerNode();
                if (ownerNode instanceof NodeLabel nodeLabel) {
                    String xpath = nodeLabel.getXsdTreeNode().getXpath();
                    Utils.clipboardCopy(xpath);
                }
            }
		});
	      
		contextMenu.getItems().addAll(menuItem1, copyXpathItem);
		
		Platform.runLater(() -> { //parent is not set yet
		    Parent parent = getParent();
		    while (parent.getParent() != null) {
		        parent = parent.getParent();
		    }
		    parent.setOnKeyPressed(e -> {
	            if (e.getCode() == KeyCode.BACK_SPACE) {
	                e.consume();
	                if (!navQueue.isEmpty()) {
	                    XSObject term = navQueue.getLast();
	                    setRootTerm(term, model);
	                }
	            }
	        });
		});
		
	}
	
	public double getElementNodeHeight() {
		return rootNode.getLabelHeight();
	}
	
	public void setRootTerm(XSObject term, XSModel model) {
	    if (this.model != model) {
	        navQueue.clear();
	        this.model = model;
	    }
	    else if (rootNode != null && term != null) {
	        if (!navQueue.isEmpty() && term == navQueue.getLast())
	            navQueue.removeLast();
	        else if (term != rootNode.getTerm()) {
	            if (navQueue.size() > 30)
	                navQueue.removeFirst();
	            navQueue.add(rootNode.getTerm());
	        }
	    }
	    getChildren().clear();
	    nodeCount = 0;
	    if (term != null) {
	        rootNode = new XsdTreeNode(term, this);
	        if (!rootNode.getSubstGroup().isEmpty()) {
	            root = new DummyRootNode(rootNode);
	        }
	        else {
	            root = rootNode;
	        }
	        rootNode.getLabel().requestFocus();
	    } else {
	        root = rootNode = null;
	    }
	}
	
	public XSElementDeclaration getRootElementDecl() {
	    if (rootNode == null || !rootNode.isElement())
	        return null;
	    return rootNode.element();
	}
	
	private TreeNodeControl getRoot() {
		return root;
	}
	
	public int getNodeCount() {
	    return nodeCount;
	}
	
	public void addNode(TreeNodeControl node) {
	    getChildren().add(node.control());
	    ++nodeCount;
	}
	
	@Override
	public void requestLayout() {
        metricsDirty = true;
		super.requestLayout();
	}
	
	private void reset() {
	    if (root != null) {
	        rootNode.reset();
	        getChildren().clear();
	        nodeCount = 0;
	        addNode(rootNode);
	    }
	}

	@Override
	protected void layoutChildren() {
		TreeNodeControl root = getRoot();
		if (root != null) {
		    autoSizeNodes(root);
		    layoutEdges(root);
		}
	}
	
	private void autoSizeNodes(TreeNodeControl node) {
		if (node.isVisible()) {
			node.autosize();
	        Pane r = node.getTypeRectange();
	        if (r != null && node.isOpen()) {
	            r.autosize();
	        }			
			for (TreeNodeControl n : node.getChildrenForLayout()) {
				autoSizeNodes(n);
			}
		}
	}
	
	@Override
	protected double computeMinHeight(double width) {
		return computePrefHeight(width);
	}
	
	//the way it works,
	//first min/computePrefHeight/max are called,
	//then set bounds are are set on this
	//then layoutChildren is called (after this has bounds)
	@Override
	protected double computePrefHeight(double width) {
        if (root == null) {
            return super.computePrefHeight(width);
        }
		return setupNodeHeightRecMap().totalHeight;
	}
	
	@Override
	protected double computeMaxHeight(double width) {
		return super.computeMaxHeight(width);
	}
	
	@Override
	protected double computePrefWidth(double height) {
	    if (root == null) {
	        return super.computePrefWidth(height);
	    }
		return setupNodeHeightRecMap().totalWidth;
	}
	
	private NodeRec setupNodeHeightRecMap() {
		if (metricsDirty) {
		    rootRec = setupNodes(getRoot(), root == rootNode ? 0 : -ConnEdges.EDGE_LEN, 0);
			metricsDirty = false;
		}
		return rootRec;
	}
	
	private static class NodeRec {
		double childCut;
		double totalHeight;
		double totalWidth;
		NodeRec(double childCut, double totalWidth, double totalHeight) {
			this.childCut = childCut;
			this.totalWidth = totalWidth;
			this.totalHeight = totalHeight;
		}
	}
	private NodeRec rootRec = null;
	
    private NodeRec setupNodes(TreeNodeControl node, double offsetX, double offsetY) {
        node.control().setTranslateX(offsetX);
        if (!node.isOpen()) {
            node.control().setTranslateY(offsetY);
            NodeRec rec = new NodeRec(0, offsetX + node.prefWidth(), node.prefHeight() + VSPACE);
            return rec;
        }
        offsetX += node.prefWidth() + ConnEdges.EDGE_LEN;

        double childHeight = 0;
        TreeNodeControl lastNodeInType = null;
        double chCut = 0;
        Pane r = node.getTypeRectange();
        if (r != null) {
            r.setTranslateY(offsetY);
            r.setTranslateX(offsetX);                
            offsetX += TYPE_H_ADD/2;
            int lastChildInType = ((XsdTreeNode.TypeRectangle) r).childrenInType;
            lastNodeInType = node.childNodes.get(lastChildInType);
            childHeight = 40; //space for type text...
            chCut = 20;
        }
        if (!node.hasSingleChild()) {
            offsetX += ConnEdges.EDGE_LEN;
        }
        
        double prevChildHeight = 0;
        NodeRec firstRec = null;
        NodeRec lastRec = null;
        double maxWidth = offsetX;

        List<TreeNodeControl> childNodes = node.getChildrenForLayout();
        Function<TreeNodeControl, Double> offsetFunc = n -> node.childNodes.indexOf(n) == -1 ? SubstEdges.SUBST_OFFSET : 0.0; //offset for subst nodes
        TreeNodeControl lastNode = node.childNodes.get(node.childNodes.size() - 1);
        for (TreeNodeControl n : childNodes) {
            NodeRec rec = setupNodes(n, offsetX + offsetFunc.apply(n), childHeight + offsetY);
            if (firstRec == null) {
                firstRec = rec;
            }
            if (n == lastNode) {
                prevChildHeight = childHeight;
                lastRec = rec;
            }
            
            if (rec.totalWidth > maxWidth)
                maxWidth = rec.totalWidth;
            childHeight += rec.totalHeight;
            if (n == lastNodeInType) {
                r.setPrefHeight(childHeight);
                childHeight += 10;
                
                r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                double prefWidth = r.prefWidth(-1) + 5;
                maxWidth = Double.max(offsetX + prefWidth, maxWidth + TYPE_H_ADD/2);
                r.setPrefWidth(maxWidth - r.getTranslateX());
            }
        }

        //for single child: child cut of this is the same as of child
        chCut += (prevChildHeight + firstRec.childCut + lastRec.childCut)/2;
        node.control().setTranslateY(offsetY + chCut);
        
        double totalHeight = Double.max(childHeight, chCut + node.prefHeight() + VSPACE);
        NodeRec rec = new NodeRec(chCut, maxWidth, totalHeight);
        return rec;
    }       
    
    private void layoutEdges(TreeNodeControl tn) {
        tn.layoutSubstEdges(getElementNodeHeight());
        if (tn.isOpen()) {
            tn.layoutEdges(getElementNodeHeight());
            for (TreeNodeControl n : tn.getChildrenForLayout()) {
                layoutEdges(n);
            }
        }
    }
	

	public Options getOptions() {
		return options;
	}

	public ContextMenu getContextMenu(XsdTreeNode node) {
	    contextMenu.getItems().get(1).setDisable(!node.isElement()); //disable copy xpath for non-element
	    return contextMenu;
	}

    public XSModel getModel() {
        return model;
    }
    
    public void onNodeFocused(Object node) {
        if (onNodeFocused != null) {
            onNodeFocused.accept(node);
        }
    }

    public void setOnNodeFocusConsumer(Consumer<Object> onFocusedConsumer) {
        this.onNodeFocused = onFocusedConsumer;
    }
   
    public void onNodeExpanded(TreeNodeControl node) {
        if (onNodeExpanded != null) {
            onNodeExpanded.accept(node);
        }
    }
    
    public void setOnNodeExpandedConsumer(Consumer<TreeNodeControl> onNodeExpanded) {
        this.onNodeExpanded = onNodeExpanded;
    }    
    
}
