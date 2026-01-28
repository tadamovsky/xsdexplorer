package com.xsdexplorer.uihelpers;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

//https://stackoverflow.com/questions/39827911/javafx-8-scaling-zooming-scrollpane-relative-to-mouse-position
public class ZoomableScrollPane extends ScrollPane {
    private double scaleValue = 1;//0.7;
    private double zoomIntensity = 0.02;
    private Node target;
    private Node zoomNode;

    public ZoomableScrollPane(Node target) {
        super();
        this.target = target;
        this.zoomNode = new Group(target);
        setContent(outerNode(zoomNode));
        setFocusTraversable(false);
       // updateScale();
    }

    @Override
    public void requestFocus() {
        //prevent focus steal
        //System.out.println();
    }
    
    private Node outerNode(Node node) {
        Node outerNode = centeredNode(node);
        outerNode.setOnScroll(e -> {
        	if (e.isControlDown()) {
        		e.consume();
        		onScroll(e.getTextDeltaY(), new Point2D(e.getX(), e.getY()));
        	}
        });
        return outerNode;
    }

    private Node centeredNode(Node node) {
        VBox vBox = new VBox(node);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private void updateScale() {
        target.setScaleX(scaleValue);
        target.setScaleY(scaleValue);
    }

    private void onScroll(double wheelDelta, Point2D mousePoint) {
        double zoomFactor = Math.exp(wheelDelta * zoomIntensity);

        Bounds innerBounds = zoomNode.getLayoutBounds();
        Bounds viewportBounds = getViewportBounds();

        // calculate pixel offsets from [0, 1] range
        double valX = this.getHvalue() * (innerBounds.getWidth() - viewportBounds.getWidth());
        double valY = this.getVvalue() * (innerBounds.getHeight() - viewportBounds.getHeight());

        scaleValue = scaleValue * zoomFactor;
        updateScale();
        this.layout(); // refresh ScrollPane scroll positions & target bounds

        // convert target coordinates to zoomTarget coordinates
        Point2D posInZoomTarget = target.parentToLocal(zoomNode.parentToLocal(mousePoint));

        // calculate adjustment of scroll position (pixels)
        Point2D adjustment = target.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
        Bounds updatedInnerBounds = zoomNode.getBoundsInLocal();
        this.setHvalue((valX + adjustment.getX()) / (updatedInnerBounds.getWidth() - viewportBounds.getWidth()));
        this.setVvalue((valY + adjustment.getY()) / (updatedInnerBounds.getHeight() - viewportBounds.getHeight()));
    }
    
    //https://stackoverflow.com/questions/15840513/javafx-scrollpane-programmatically-moving-the-viewport-centering-content/23518314#23518314
    public void centerNodeInScrollPane(Node node) {
        double h = getContent().getBoundsInLocal().getHeight();
        
        Bounds boundsInParent = node.getBoundsInParent();
        Node child = node;
        while (child.getParent() != this) {
            child = child.getParent();
            boundsInParent = child.localToParent(boundsInParent);
        }
        
        
        double y = (boundsInParent.getMaxY() + 
                boundsInParent.getMinY()) / 2.0;
        double v = getViewportBounds().getHeight();
        double vValue = getVmax() * ((y - 0.5 * v) / (h - v));
        setVvalue(vValue);
    }
}