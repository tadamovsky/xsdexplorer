package com.xsdexplorer;
import com.xsdexplorer.uihelpers.Utils;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;

public class LogView {
    public enum Kind {
        INFO("infoIcon"),
        SUCCESS("successIcon"),
        WARNING("warningIcon"),
        ERROR("errorIcon");
        
        private String style;
        Kind(String style) {
            this.style = style;
        }
        
        @Override
        public String toString() {
            return "[" + name() + "]";
        }
        
        public String formatMessage(String msg) {
            return "[" + name() + "] "+msg;
        }
        
    }
    
    private static class Record {
        Kind kind;
        String message;
        public Record(Kind kind, String message) {
            this.kind = kind;
            this.message = message;
        }
        @Override
        public String toString() {
            return message;
        }
    }
    
    private ListView<Record> listView;

    public void addMessage(Kind kind, String message) {
        ObservableList<Record> items = listView.getItems();
        int index = items.size();
        items.add(new Record(kind, message));
        listView.scrollTo(index);
    }
    
    public void clear() {
        listView.getItems().clear();
    }
    
    public Parent createLogView() {
        listView = new ListView<>();
        listView.setPrefHeight(80);
        listView.setCellFactory(tv ->  {
            ListCell<Record> cell = new ListCell<>() {
                final ContextMenu contextMenu = createContextMenu();

                @Override
                public void updateItem(Record item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setTooltip(null);
                        setContextMenu(null);
                        setGraphic(null);
                    } else {
                        setText(item.message);
                        Label img = new Label();
                        img.getStyleClass().add(item.kind.style);
                        setGraphic(img);       
                        //setContextMenu(contextMenu); //with this getOwnerNode returns null
                        setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
                    }
                }
            };
            return cell;
        });
        return listView;
    }
    
    private ContextMenu createContextMenu() {
        MenuItem copyItem = new MenuItem("Copy");
        MenuItem copyAll = new MenuItem("Copy All");
        MenuItem clear = new MenuItem("Clear");
        ContextMenu contextMenu = new ContextMenu(copyItem, copyAll, clear);

        copyItem.setOnAction(e -> {
            
            Object ownerNode = contextMenu.getOwnerNode();
            if (ownerNode instanceof ListCell<?> cell) {
                Utils.clipboardCopy(cell.getText());
            }
        });

        copyAll.setOnAction(e -> {
            ObservableList<Record> items = listView.getItems();
            Utils.clipboardCopyList(items);
        });
        
        clear.setOnAction(e -> listView.getItems().clear());
        return contextMenu;
    }

}
