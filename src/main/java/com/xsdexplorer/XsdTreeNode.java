package com.xsdexplorer;

import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.WILDCARD;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.*;

import com.google.common.base.Preconditions;
import com.xsdexplorer.XsdTreeView.Options;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


public class XsdTreeNode extends TreeNodeControl {
	static final double ANNOTATION_WRAP = 140;

	private XSObject term;
    private NodeControl control = new NodeControl();
	private XsdTreeView parentLayout;
	
	private boolean isOpen = false;
    private int minOccurs = 1;
	private int maxOccurs = 1;
	private XsdTreeNode parentTreeNode;
	
	private XSParticle particle; //used for recursion detection
	
    private NodeLabel label;
	
	//not part of the control but of the parent tree view
	private ConnEdges connEdges; 
	private TypeRectangle typeRectangle;
	private SubstEdges substEdges;
	private List<XsdTreeNode> substGroup;
	
    public XsdTreeNode(XSParticle p, XsdTreeNode parentTreeNode) {
		this.minOccurs = p.getMinOccurs();
		this.maxOccurs = p.getMaxOccurs();
		this.parentLayout = parentTreeNode.parentLayout;
		this.parentTreeNode = parentTreeNode;
		this.particle = p;
		init(p.getTerm());
	}
	
    //used for substitution group element only
    private XsdTreeNode(XSObject term, XsdTreeNode parent) {
        this.minOccurs = parent.minOccurs;
        this.maxOccurs = parent.maxOccurs;
        this.parentLayout = parent.parentLayout;
        this.parentTreeNode = parent;
        init(term);
    }
	
	//used for root element only
	public XsdTreeNode(XSObject term, XsdTreeView parentLayout) {
		this.parentLayout = parentLayout;
		init(term);
	}
	
	private void init(XSObject term) {
		this.term = term;
		label = NodeLabel.createNodeLabel(this);
        if (label.getOnClickControl() != null) {
            label.getOnClickControl().setOnMouseClicked(this::onPlusClicked);
        }
        label.setOnMouseClicked(this::onMouseClicked);
        label.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue)
                onFocused();
        });   
    	label.setOnContextMenuRequested((e) -> getParentLayout().getContextMenu(this).show(label, e.getScreenX(), e.getScreenY()));
		control.getChildren().add(label);
		addOccursLabel();
		addAnnotation();
		//setBackground(new Background(new BackgroundFill(Color.CORNSILK, new CornerRadii(0), Insets.EMPTY)));
		parentLayout.addNode(this);
		
		substGroup = getSubstitutionGroup().stream().map(e -> new XsdTreeNode(e, this)).sorted((n1, n2) -> n1.getTerm().getName().compareTo(n2.getTerm().getName())).toList();
		substEdges = SubstEdges.createIfNeeded(this);
		
		if (isLoop()) {
		    //start of recursive seq., add single child "..." and mark open
		    isOpen = true;
		    childNodes = List.of(new RecursionNode(particle.toString(), getParentLayout()));
            connEdges = new ConnEdges(this);
        }
	}

    public List<TreeNodeControl> getChildrenForLayout() {
        return childNodes.stream().flatMap(e -> e instanceof XsdTreeNode xn ? Stream.concat(Stream.of(e), xn.getSubstGroup().stream()) : Stream.of(e)).toList();
    }
	
	void onOpenExternalEditor() {
	    XsdTreeNode node = this;
	    while (!node.isGlobal()) {
	        node = node.parentTreeNode;
	    }
	    ExternalEditor extEditor = new ExternalEditor();
	    extEditor.openInExternalEditor(getParentLayout().getModel(), node.term);
	}
	
	public boolean isGroup() {
		return term.getType() == MODEL_GROUP;
	}
	
	public boolean isAny() {
		return term.getType() == WILDCARD;
	}
	
	public boolean isElement() {
		return term.getType() == XSConstants.ELEMENT_DECLARATION;
	}
	
	public boolean isGlobalRef() {
	    return parentTreeNode != null && isElement() && element().getScope() == XSElementDecl.SCOPE_GLOBAL;
	}
	
	public boolean isGlobal() {
	    return parentTreeNode == null || isGlobalRef();
	}
	
    public boolean isType() {
        return term.getType() == XSConstants.TYPE_DEFINITION;
    }
	
	
	public XSModelGroup group() {
		return (XSModelGroup) term;
	}
	
	public XSElementDeclaration element() {
		return (XSElementDeclaration) term;
	}
	
	public XSWildcard any() {
		return (XSWildcard) term;
	}
	
    public XSTypeDefinition type() {
        return (XSTypeDefinition) term;
    }

	public String anyOrGroupCompositor() {
		return isAny() ? "any" : groupCompositor();
	}
	
	public String groupCompositor() {
		final String[] values = { "sequence", "choice", "all" };
		return values[group().getCompositor() - 1];
	}
	
	@SuppressWarnings("unchecked")
	public List<XSParticle> groupParticles() {
		return group().getParticles();
	}
	
	@SuppressWarnings("unchecked")
    static List<XSParticle> groupParticles(XSTerm term) {
	    return ((XSModelGroup) term).getParticles();
	}
	
    @SuppressWarnings("unchecked")
    private List<XSElementDeclaration> getSubstitutionGroup() {
        if (isElement()) {
            try {
                List<XSElementDeclaration> ret = getParentLayout().getModel().getSubstitutionGroup((XSElementDeclaration) term);
                return ret == null ? Collections.emptyList() : ret;
            } catch (Exception e) {
                //happens in case of some xsd validation error...
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
	
    public List<XsdTreeNode> getSubstGroup() {
        return substGroup;
    }

    private void onMouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            label.requestFocus();
            event.consume();
        }
    }
    
    private void onFocused() {
        getParentLayout().onNodeFocused(this);
    }    
    
	private void onPlusClicked(MouseEvent event) {
	    if (event.getButton() == MouseButton.PRIMARY) {
            event.consume();
	        toggleExpand();
	    }
	}
	
	public void toggleExpand() {
        label.toggleOpenControl();
		if (isOpen) {
			isOpen = false;
			hideSubtree(this);
			return;
		}
		isOpen = true;
		if (childNodes.size() > 0) {
			showSubtree(this);
		}
		else {
    		populateChildren();
    		connEdges = new ConnEdges(this);
    		//fireNodeOpenEvent();
    		
    		autoexpand();
		}
        getParentLayout().onNodeExpanded(this);
	}
	
	private void autoexpand() {
	    if (hasSingleChild() ||
	            //single non-attr node
	            childNodes.stream().filter(e -> e instanceof XsdTreeNode).skip(1).findAny().isEmpty()) {
            for (TreeNodeControl c : childNodes) {
                c.expand(false);
            }
	    }
    }

    void expand(boolean force) {
	    if ((isGroup() || force) && label.getOnClickControl() != null) { 
	        toggleExpand();
            /*
            if (parentLayout.getNodeCount() < 300) {
                Platform.runLater(() -> {            
                    label.toggleOpenControl(null);
                    toggleExpand(null);
                });
            }
            */
	    }
	}
	
	public Pane getTypeRectange() {
		return typeRectangle;
	}
	
	boolean allChildrenOptional() {
		for (TreeNodeControl child : childNodes) {
			if (!child.isOptional())
				return false;
		}
		return true;
	}

	private static void hideSubtree(TreeNodeControl node) {
		for (TreeNodeControl child : node.getChildrenForLayout()) {
			child.setVisible(false);
			hideSubtree(child);
		}
	}
	
	private static void showSubtree(TreeNodeControl node) {
		for (TreeNodeControl child : node.getChildrenForLayout()) {
			child.setVisible(true);
			if (child.isOpen()) {
				showSubtree(child);
			}
		}
	}
	
	private boolean isLoop() {
	    if (!isGroup())
            return false;
        XsdTreeNode node = parentTreeNode;
        while (node != null) {
            if (node.particle == particle)
                return true;
            node = node.parentTreeNode;
        }
        return false;
	}
	
	private void populateChildren() {
       if (isGroup()) {
            childNodes = groupParticles().stream().map(p -> (TreeNodeControl) new XsdTreeNode(p, this)).toList();
        }
        else if (!isAny()) {
            XSComplexTypeDecl t =  getComplexType();
            if (t == null)
                return;
            int typeRectIndex = getParentLayout().getChildren().size();
            String typeName = getTypeName();
            boolean requestSplit = typeName != null && typeName.endsWith("(ext)"); 
            ComplexTypeExtractor ct = new ComplexTypeExtractor(requestSplit);
            childNodes = ct.extractComplexTypeChildren(t, this, options());
            
            if (typeName != null) {
                int typeCount;
                if (requestSplit) {
                    typeCount = ct.getBaseTypeSize();
                }
                else {
                    Preconditions.checkState(ct.getBaseTypeSize() == 0);
                    typeCount =  childNodes.size();
                }
                if (typeCount > 0) { //don't show empty type rect, e.g. empty base?
                    typeRectangle = new TypeRectangle(typeName, typeCount - 1);
                    //must be added before children of this, as they are layered over type rect
                    getParentLayout().getChildren().add(typeRectIndex, typeRectangle);
                    typeRectangle.visibleProperty().bind(firstChild().visibleProperty());
                }
            }
        }
	}
	
    private List<XSAttributeUse> getAttributes() {
		XSComplexTypeDecl t =  options().showAttributes.get() ? getComplexType() : null;
		return t != null ? castAttrList(t.getAttributeUses()) : Collections.emptyList();
	}
	
	@SuppressWarnings("unchecked")
    private static List<XSAttributeUse> castAttrList(XSObjectList l) {
	    return l;
	}
	
	private String getTypeName() {
	    if (!options().showTypes.get())
	        return null;
	    XSComplexTypeDecl t = getComplexType();
	    if (t == null)
	        return null;
		if (t.getContentType() >= XSComplexTypeDefinition.CONTENTTYPE_ELEMENT || !t.getAttributeUses().isEmpty()) {
		    if (!t.getAnonymous() && !isType())
                return t.getName();
		    if (t.getBaseType() instanceof XSComplexTypeDecl && t.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) 
		        return t.getBaseType().getName()+" (ext)";
		}
		return null;
	}
	
	boolean hasXsdChildren() {
	    if (isLoop())
	        return false;
	    if (!getAttributes().isEmpty())
	        return true;
	    if (isGroup()) {
	        return !groupParticles().isEmpty();
        }
        else if (isAny()) {
            return false;
        } else if (isElement()) {
            XSTypeDefinition td = element().getTypeDefinition();
            if (!SchemaGrammar.isAnyType(td)) {
                XSParticle p = getComplexContent(td);
                return (p != null);
            }
            return false;
        } else {
            if (term instanceof XSComplexTypeDecl t) {
                return t.getParticle() != null;
            }
            return false;
        }
	}
	
	
	public boolean isSimpleContent() {
        if (isElement()) {
            XSComplexTypeDecl ct = getComplexType();
            if (ct == null)
                return true;
            return ct.getContentType() <= XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
        }
        return false;
	}
	
	public boolean hasComplexContent(XSTypeDefinition td) {
		return td instanceof XSComplexTypeDecl t && t.getContentType() >= XSComplexTypeDefinition.CONTENTTYPE_ELEMENT;
	}
	
	private XSComplexTypeDecl getComplexType() {
		if (isElement()) {
			XSTypeDefinition td = element().getTypeDefinition();
			if (td instanceof XSComplexTypeDecl t) {
				return t;
			}
		}
		else if (isType() && term instanceof XSComplexTypeDecl t) {
		    return t;
		}
		return null;
	}
	
	private XSParticle getComplexContent(XSTypeDefinition td) {
		if (td instanceof XSComplexTypeDecl t) {
			return t.getParticle();
		}
		return null;
	}
	
	public boolean isRepeating() {
	    return maxOccurs > 1 || maxOccurs == -1;
	}
	
	private void addOccursLabel() {
		if (maxOccurs == 0 || maxOccurs == 1) {
			return;
		}
		Text occLabel = new Text(String.format("%d..%s", minOccurs, maxOccurs > 0 ? String.valueOf(maxOccurs): "\u221E"));
		control.addOccursLabel(occLabel);
	}
	
	private void addAnnotation() {
		if (!options().showAnnotations.get())
			return;
		XSAnnotation xsAnnot = isElement() ?  element().getAnnotation() : 
		        isGroup() ? group().getAnnotation() : (isAny() ? any().getAnnotation() : getTypeAnnotation());
		String text;
		if (xsAnnot != null && (text = AnnotationExtractor.extract(xsAnnot.getAnnotationString()))!= null) {
			control.addAnnot(text);
		}
	}
	
	private XSAnnotation getTypeAnnotation() {
	    if (term instanceof XSComplexTypeDecl t) {
	        XSObjectList annotations = t.getAnnotations();
	        if (!annotations.isEmpty())
	            return (XSAnnotation) annotations.get(0);
	    }
        return null;
    }

    public void reset() {
		control.getChildren().clear();
		childNodes.clear();
		isOpen = false;
		connEdges = null;
		typeRectangle = null;
		control.getChildren().add(label);
		addOccursLabel();
		addAnnotation();
		label.resetOpenControl(hasXsdChildren());
        if (label.getOnClickControl() != null) {
            label.getOnClickControl().setOnMouseClicked(this::onPlusClicked);
        }
	}
	
	

	public XsdTreeView getParentLayout() {
		return parentLayout;
	}

	public NodeLabel getLabel() {
		return label;
	}
	
	public double getLabelHeight() {
		return label.getHeight();
	}

	private Options options() {
		return getParentLayout().getOptions();
	}

	@Override
	public String toString() {
		return String.format("'%s', open: %b, children num: %d", label.getText(), isOpen, childNodes.size());
	}
	
	private class NodeControl extends VBox {
    	//vbox content:
    	//label
    	//occurs
    	//annotation
	    private Text annot;
	    
    	public NodeControl() {
            setFillWidth(false);
        }
    	//void addLabel() {
    	//    getChildren().add(label);
    	//}
    	
		void addOccursLabel(Text occLabel) {
    		getChildren().add(occLabel);
    		VBox.setMargin(occLabel, new Insets(0, 0, 0, 6));
    	}
    	
    	public void addAnnot(String text) {
            annot = new Text(text);
            //annot.setTextAlignment(TextAlignment.LEFT);
            annot.getStyleClass().add("annotation");
			getChildren().add(annot);
			VBox.setMargin(annot, new Insets(6, 0, 0, 0));
		}

    	@Override
    	protected double computeMinWidth(double height) {
    	    return computePrefWidth(height);
    	}
    	
    	@Override
    	protected double computePrefWidth(double height) {
    		fixAnnotationWrap();
    		double ret = super.computePrefWidth(height);
    		return ret;
    		
    	}
    	
    	//can only be called during layout pass
    	private void fixAnnotationWrap() {
    		if (annot != null) {
    			double prefWidth = Double.max(ANNOTATION_WRAP, label.prefWidth(-1));
    			annot.setWrappingWidth(prefWidth);
    		}
    	}

    }

    static class TypeRectangle extends Pane {
        final int childrenInType;
        TypeRectangle(String typeName, int childrenInType) {
            this.childrenInType = childrenInType;
            
            Text t = new Text(typeName);
            t.relocate(10, 8);
            getChildren().add(t);
            this.getStyleClass().add("TypeRectangle");
        }

    }

    public XSObject getTerm() {
        return term;
    }

    public XsdTreeNode getParentTreeNode() {
        return parentTreeNode;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }
    
    ////////////////////
    //TreeNodeControl
    /////////////////////
    @Override
	public boolean isOpen() {
		return isOpen;
	}


	@Override
	Region control() {
		return control;
	}

	@Override
	boolean isOptional() {
		return minOccurs == 0;
	}
	
    @Override
    public void layoutSubstEdges(double labelHeight) {
        if (substEdges != null) {
            substEdges.layoutSubstElements(this, labelHeight);
        }
    }
    
    @Override
    public void layoutEdges(double labelHeight) {
        connEdges.layoutEdges(this, labelHeight);
    }

    public String getXpath() {
        if (!isElement())
            return "";
        LinkedList<String> paths = new LinkedList<>();
        XsdTreeNode n = this;
        paths.add(term.getName());
        while (n.getParentTreeNode() != null) {
            n = n.parentTreeNode;
            if (n.isElement())
                paths.add(0, n.term.getName());
        }
        return paths.stream().collect(Collectors.joining("/", "/", ""));
    }
}
