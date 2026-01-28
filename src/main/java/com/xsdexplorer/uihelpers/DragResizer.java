package com.xsdexplorer.uihelpers;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * based on https://stackoverflow.com/questions/16925612/how-to-resize-component-with-mouse-drag-in-javafx
 * only suitable for horizontal resize to left of right control in BorderPane
 * 
 */
public class DragResizer {

    private final Region region;

    private boolean dragging;

    private DragResizer(Region aRegion) {
        region = aRegion;
    }

    public static void makeResizable(Region region) {
        final DragResizer resizer = new DragResizer(region);
        region.addEventFilter(MouseEvent.MOUSE_PRESSED, resizer::mousePressed);
        region.addEventFilter(MouseEvent.MOUSE_RELEASED, resizer::mouseReleased);
        region.addEventFilter(MouseEvent.MOUSE_DRAGGED, resizer::mouseDragged);
        region.addEventFilter(MouseEvent.MOUSE_MOVED, resizer::mouseOver);
    }

    private void mouseReleased(MouseEvent event) {
        dragging = false;
        region.setCursor(Cursor.DEFAULT);
    }

    private void mouseOver(MouseEvent event) {
        if(isInDraggableZone(event) || dragging) {
            region.setCursor(Cursor.W_RESIZE);
        }
        else {
            region.setCursor(Cursor.DEFAULT);
        }
    }

    private boolean isInDraggableZone(MouseEvent event) {
        return event.getX() <= 5;
    }

    private void mouseDragged(MouseEvent event) {
        if(!dragging) {
            return;
        }

        double newWidth = region.getWidth() -  event.getX();
        region.setPrefWidth(newWidth);
    }

    private void mousePressed(MouseEvent event) {

        // ignore clicks outside of the draggable margin
        if(!isInDraggableZone(event)) {
            return;
        }

        dragging = true;
    }
}