package com.xsdexplorer;

import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.WILDCARD;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.*;

import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class NodeLabel extends Pane {
    
    private static String SVG_MODEL_GROUP = "M 10 20 v -10 l 5 -5 h 25 L 45 10 V 20 L 40 25 H 15 Z";
    private static String SVG_TYPE_DEF = "m 15 25 c -4 -6 -4 -14 0 -20 H 75 V 25 H 15 Z";//"M 10 20 v -10 l 5 -5 H 40 V 25 h -25 Z";
    private static String SVG_ELEMENT = "M 0 0 V 10 H 20 V 0 Z";
    private static String SVG_SEQ_INSIDE = "M 14 15 H 41 M 20.948 14.103 h 1.703 v 1.703 h -1.703 Z M 25.948 14.103 h 1.703 v 1.703 h -1.703 Z M 30.93 14.103 h 1.703 v 1.703 h -1.703 Z";
    private static String SVG_CHOICE_INSIDE = "M 26.177 9.877 h 1.703 v 1.703 h -1.703 Z M 26.177 14.151 h 1.703 v 1.703 h -1.703 Z M 26.177 18.335 h 1.703 v 1.703 h -1.703 Z M 41.972 15.052 H 30.645 Z M 35.917 9.998 H 30.645 Z V 20.135 Z M 35.91 20.034 H 30.645 Z M 19.268 14.963 H 13.289 Z L 23.05 9.933 Z";
    private static String SVG_ALL_INSIDE = "M 26.177 9.877 h 1.703 v 1.703 h -1.703 Z M 26.177 14.151 h 1.703 v 1.703 h -1.703 Z M 26.177 18.335 h 1.703 v 1.703 h -1.703 Z M 41.972 15.052 H 30.645 Z M 35.917 9.998 H 30.645 Z V 20.135 Z M 35.91 20.034 H 30.645 Z M 17.979 15.026 H 13.256 Z h 5.468 Z V 10.101 Z V 19.992 Z M 17.968 10.033 h 5.468 Z M 17.987 20.011 h 5.468 Z";
    
    //private static final double SVG_W = 60;
    //private static final double SVG_H = 26.96;
    private static final double PLUS_SIZE = 10;
    private static final double HEIGHT_ADD = 6;
    private double svgAspectX, svgAspectY;
    
    private Text text;
    
    private Group plusMinusControl;
    
    //private Consumer<MenuItem> onRightClickHandler;
    
    private boolean addGlobalRefArrow; //add arrow sign for global ref elements
    private boolean isSimpleContent;
    private XsdTreeNode xsdTreeNode; 
    
    private static Font font = null;
    private static class SvgSize {
        final double width;
        final double height;
        SvgSize(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }
    private static SvgSize svgSize;
    
    private NodeLabel(XsdTreeNode node) {
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        this.setFocusTraversable(true);
        getStyleClass().add("nodeLabel");
        this.xsdTreeNode = node; 
     }
    
    private NodeLabel(String textStr, XsdTreeNode node) {
        this(node);
        String title = StringUtils.abbreviate(textStr, 30);
        this.text = new Text(StringUtils.rightPad(title, 5, ' '));
        if (textStr.length() > 30) {
            Tooltip tooltip = new Tooltip(textStr);
            tooltip.setShowDelay(Duration.millis(200));
            Tooltip.install(this, tooltip);
        }
        setTextFont();
    }
    
    private void setTextFont() {
        if (font == null) {
            text.setStyle("-fx-font-weight: bold;");
            new Scene(new Group(text));
            text.applyCss();
            font = text.getFont();
            double svgHeight = text.prefHeight(-1) + HEIGHT_ADD; //26.96
            double svgWidth = svgHeight*1.75;//.226;           //60
            svgSize = new SvgSize(svgWidth, svgHeight);
            //System.out.println("svg width: "+svgWidth+", svgHeight: "+svgHeight);
        } else {
            text.setFont(font);
        }
    }
    
    static Font getFont() {
        return font;
    }
    
    public static NodeLabel createNodeLabel(XsdTreeNode node) {
        boolean repeating = node.getMaxOccurs() > 1 || node.getMaxOccurs() == -1;
        int minoccurs = node.getMinOccurs();
        boolean hasChildren = node.hasXsdChildren();
        XSObject term = node.getTerm();
        NodeLabel n;
        switch (term.getType()) {
        case ELEMENT_DECLARATION:
            n = new NodeLabel(term.getName(), node);
            n.setElementOptions(node.isGlobalRef(), node.isSimpleContent());
            n.initNode(SVG_ELEMENT, minoccurs, repeating, hasChildren);
            return n;
        case MODEL_GROUP:
            n =  new NodeLabel(node);
            n.initNode(SVG_MODEL_GROUP, minoccurs, repeating, hasChildren);
            n.addGroupNodeInnerSvg(((XSModelGroup) term).getCompositor());
            return n;
        case WILDCARD:
            XSWildcard any = (XSWildcard) term;
            String ns = "";
            switch (any.getConstraintType()) {
            case XSWildcard.NSCONSTRAINT_ANY:
                ns = "##any";
                break;
            case XSWildcard.NSCONSTRAINT_NOT:
                ns = "##other";
                break;
            case XSWildcard.NSCONSTRAINT_LIST:
                //can be list created from ##local, ##targetNamespace, {URI references}
                List<String> nsList = castStringList(any.getNsConstraintList());
                //String namespace = any.getNamespace();
                //one issue when namespace is null, and nsList contains null, original xsd may contain ##local or ##targetNamespace - this info is lost???
                //ns = nsList.stream().map(s -> s == null ? "##local" : s.equals(namespace) ? "##targetNamespace" : s).collect(Collectors.joining(" "));
                ns = nsList.stream().map(s -> s == null ? "##local" : s).collect(Collectors.joining(" "));
            }
            String name = "any ";
            if (!ns.isEmpty()) {
                name += StringUtils.abbreviate(ns, 25);
            }
            else {
                name += "     ";
            }
            n = new NodeLabel(name, node);
            n.initNode(SVG_MODEL_GROUP, minoccurs, repeating, hasChildren);
            return n;
        case XSConstants.TYPE_DEFINITION:
            n = new NodeLabel(term.getName(), node);
            n.initNode(SVG_TYPE_DEF, minoccurs, repeating, hasChildren);
            return n;
        default:
            throw new RuntimeException("Unrecognized type");
        }
    }
   
    @SuppressWarnings("unchecked")
    private static List<String> castStringList(StringList nsList) {
        return nsList;
    }
    
    private void addGroupNodeInnerSvg(int model) {
        SVGPath svg = new SVGPath();
        if (model == XSModelGroup.COMPOSITOR_SEQUENCE) {
            svg.setContent(SVG_SEQ_INSIDE);
        }
        else if (model == XSModelGroup.COMPOSITOR_CHOICE) {
            svg.setContent(SVG_CHOICE_INSIDE);
            //svg.setFillRule(FillRule.EVEN_ODD);
        }
        else {
            svg.setContent(SVG_ALL_INSIDE);
        }
        svg.setScaleX(svgAspectX);
        svg.setScaleY(svgAspectY);
        svg.setStroke(Color.BLACK);
        svg.setFill(Color.BLACK);
        svg.setStrokeWidth(1);
        
        double w = svg.prefWidth(-1)*svgAspectX;
        double h = svg.prefHeight(w)*svgAspectY;
        //Region r = svgInRegion(svg, w, h);
        //r.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        //getChildren().add(r);
        //r.relocate((SVG_W - w)/2, SVG_H/2- h/2);
        Group g = new Group(svg);
        getChildren().add(g);
        g.relocate((svgSize.width - w)/2, svgSize.height/2- h/2);
        
        
    }
    
    private void initNode(String svg, int minoccurs, boolean repeating, boolean hasChildren) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(svg);
        double w, h, plusMove;
        if (text != null) {
            w = text.prefWidth(-1) + 20;
            h = text.prefHeight(-1) + HEIGHT_ADD;
            text.relocate(10, HEIGHT_ADD/2);
            plusMove = PLUS_SIZE / 2;
        }
        else {
            w = svgSize.width;
            h = svgSize.height;
            plusMove = 1;
        }
        Region r1 = addSvgMainShape(svgPath, minoccurs, w, h);
        
        if (repeating) {
            svgPath = new SVGPath();
            svgPath.setContent(svg);
            Region r2 = svgInRegion(svgPath, w, h);
            getChildren().add(r2);
            r1.relocate(3, 3);
            r2.relocate(0, 0);
            r2.getStyleClass().addAll("nodeShape", minoccurs == 0 ? "optionalShape" : "requiredShape");

        }
        
        if (text != null) {
            getChildren().add(text);
            addGlobalRefSignIfNeeded();
            showSimpleContentIfNeeded();
        }
        if (hasChildren) {
            createPlusMinus();
            plusMinusControl.relocate(w - plusMove, (h - PLUS_SIZE)/2);
        }
    }
    
    private Region addSvgMainShape(SVGPath svg, int minoccurs, double w, double h) {
        final Region svgShape = svgInRegion(svg, w, h);
        svgShape.getStyleClass().addAll("nodeShape", "shapeWithShadow", minoccurs == 0 ? "optionalShape" : "requiredShape");
        getChildren().add(svgShape);
        svgShape.relocate(0, 0);
        svgAspectX = w / svg.prefWidth(-1);
        svgAspectY = h / svg.prefHeight(w);
        return svgShape;
    }
    
    private Region svgInRegion(SVGPath svg, double w, double h) {
        final Region svgShape = new Region();
        svgShape.setShape(svg);
        svgShape.setMinSize(w, h);
        svgShape.setPrefSize(w, h);
        //svgShape.setMaxSize(w, h);
        return svgShape;
    }

    public String getText() {
        return text.getText();
    }
        
    private void createPlusMinus() {
        Group g = new Group();
        g.setFocusTraversable(false);
        Rectangle r = new Rectangle(10, 10);
        r.setFill(Color.WHITE);
        r.setStroke(Color.BLACK);
        r.setStrokeWidth(1);
        Line l1 = new Line(2, 5, 8, 5);
        Line l2 = new Line(5, 2, 5, 8);
        g.getChildren().addAll(r, l1, l2);
        getChildren().add(g);
        plusMinusControl = g;
    }
    
    private void addGlobalRefSignIfNeeded() {
        if (!addGlobalRefArrow)
            return;
        Polyline polyline = new Polyline();
        polyline.getPoints().addAll(new Double[]{
            0.0, 0.0,    
            6.0, 6.0,
            6.0, 3.5,
            3.5, 6.0,
            6.0, 6.0,
            });
        getChildren().add(polyline);
        polyline.getTransforms().add(new Rotate(-90, 0, 0));
        polyline.relocate(0, svgSize.height - 2);
    }    
    
    private void showSimpleContentIfNeeded() {
        if (isSimpleContent) {
            SVGPath simpleC = new SVGPath();
            simpleC.setContent("M 0 3 H 5 M 0 6 H 5 M 0 9 H 5");
            getChildren().add(simpleC);
            simpleC.setFill(Color.TRANSPARENT);
            simpleC.setStroke(Color.BLACK);
            simpleC.relocate(0, 0);
        }
    }
    
    public void toggleOpenControl() {
        Node line = plusMinusControl.getChildren().get(2);
        line.setVisible(!line.isVisible());
    }
    
    public void resetOpenControl(boolean hasChildren) {
        if (hasChildren) {
            if (plusMinusControl != null) {
                Node line = plusMinusControl.getChildren().get(2);
                line.setVisible(true);
            } 
            else {
                createPlusMinus();
            }
        } else {
            if (plusMinusControl != null) {
                getChildren().remove(plusMinusControl);
                plusMinusControl = null;
            }
        }
    }

    public Node getOnClickControl() {
        return plusMinusControl;
    }

    public XsdTreeNode getXsdTreeNode() {
        return xsdTreeNode;
    }

    public void setElementOptions(boolean addGlobalRefArrow, boolean simpleContent) {
        this.addGlobalRefArrow = addGlobalRefArrow;
        this.isSimpleContent = simpleContent;
    }
    
}
