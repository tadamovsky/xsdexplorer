package com.xsdexplorer.loader;

import java.io.File;

import com.xsdexplorer.LogView;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

public class SchemaLoaderTask extends Task<SchemaLoader> {
    private SchemaLoader schemaLoader = new SchemaLoader();

    private final File[] files;

    private boolean hasErrors;
    
    public SchemaLoaderTask(File... files) {
        this.files = files;
    }
    
    
    
    @Override
    protected SchemaLoader call() throws Exception {
        schemaLoader.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String msg) -> {
            if (msg.startsWith(LogView.Kind.ERROR.toString()))
                hasErrors = true;
            updateMessage(msg); //will update on javafx ui thread
        });
        for (File f :  files) {
            schemaLoader.loadSchema(f);
            if (hasErrors) {
                hasErrors = false;
            }
            else {
                updateMessage(LogView.Kind.SUCCESS.formatMessage(f +" is valid"));
            }
            
        }
        return schemaLoader;
    }


}
