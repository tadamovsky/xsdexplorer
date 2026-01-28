package com.xsdexplorer.uihelpers;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class Utils {

    public static void clipboardCopy(String s) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(s);
        clipboard.setContent(content);                
    }
    
    public static void clipboardCopyList(List<?> list) {
        String all = list.stream().map(Object::toString).collect(Collectors.joining("\n"));
        clipboardCopy(all);
    }
    
    public static String getVersion() {
        Properties props = new Properties();
        try {
            props.load(Utils.class.getClassLoader().getResourceAsStream("META-INF/maven/com.xsdtools/xsdexplorer/pom.properties"));
            return props.getProperty("version", "1.0.0");
        } catch (IOException e) {
            return "1.0.0";
        }        
    }
}
