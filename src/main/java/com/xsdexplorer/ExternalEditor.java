package com.xsdexplorer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSModelImpl;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.datatypes.ObjectList;
import org.codehaus.stax2.XMLStreamReader2;

import com.xsdexplorer.loader.XmlFactory;

public class ExternalEditor {
    
    public static String getCurrentEditor() {
        return XsdExplorer.getApp().getPrefs().get("externalEditor", getDefaultEditor());
    }
    
    public static void setCurrentEditor(String path) {
        XsdExplorer.getApp().getPrefs().put("externalEditor", path);
    }
    
    private static String getDefaultEditor() {
        String pfiles = System.getenv("ProgramFiles");
        if (pfiles != null) {
            File f = new File(pfiles, "Notepad++/notepad++.exe");
            if (f.exists())
                return f.getAbsolutePath();
        }
        pfiles = System.getenv("ProgramFiles(x86)");
        if (pfiles != null) {
            File f = new File(pfiles, "Notepad++/notepad++.exe");
            if (f.exists())
                return f.getAbsolutePath();
        }
        String regLoc = getNotepadPlusPlusRegistryLocation();
        if (regLoc != null)
            return regLoc;
        return "notepad.exe";
    }
    
    private static String getNotepadPlusPlusRegistryLocation() {
        try {
            Process proc = new ProcessBuilder("reg", "query", "HKLM\\SOFTWARE\\Notepad++", "/ve").redirectErrorStream(true).start();
            String out = IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8);
            int k = out.indexOf("REG_SZ");
            if (k == -1)
                return null;
            String dir = out.substring(k + 6).trim();
            File f = new File(dir, "notepad++.exe");
            return f.exists() ? f.getAbsolutePath() : null;
        } catch (IOException e) {
            return null;
        }
    }
    

    void openInExternalEditor(XSModel model, XSObject term) {
        XSModelImpl m = (XSModelImpl) model;
        //boolean isElem = isElement();
        String ns = term.getNamespace() == null ? "" : term.getNamespace();
        String name = term.getName();
        final int len = m.getLength();
        for (int i=0; i<len; ++i) {
            SchemaGrammar sg = (SchemaGrammar) m.get(i);
            String targetNs = sg.getTargetNamespace() == null ? "" : sg.getTargetNamespace();
            if (!targetNs.equals(ns))
                continue;
            //each element coming in pairs:
            //0 - String - filename,name
            //1 - XsElementDecl
            ObjectList globalElements = sg.getComponentsExt(term.getType());
            for (int j = 0; j < globalElements.getLength(); j += 2) {
                String fileWithName = (String) globalElements.get(j);
                if (fileWithName.endsWith(name)) {
                    int last = fileWithName.length() - name.length() - 1;
                    if (fileWithName.charAt(last) != ',')
                        continue;
                    String fileStr = fileWithName.substring(0, last);
                    if (fileStr.startsWith("file://"))
                        fileStr = fileStr.substring(7);
                    File file = new File(fileStr);
                    if (!file.exists())
                        return;
                    try {
                        int line = detectLine(file, term);
                        openEditor(file, line);
                    } catch (Exception e) {
                        System.out.println("error opening external file: "+e.getMessage());
                    }
                    return;
                }
            }
        }
    }

    public void openEditor(File file, int line) throws IOException {
        String currentEditor = getCurrentEditor();
        List<String> commands = new ArrayList<>();
        commands.add(currentEditor);
        commands.add(file.getAbsolutePath());
        if (currentEditor.contains("notepad++")) {
            //assume line support with -n parameter
            commands.add("-n"+line);
        }
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.start();
    }

    private int detectLine(File file, XSObject term) throws XMLStreamException {
        String tagName = term.getType() == XSConstants.ELEMENT_DECLARATION ? "element" : term instanceof XSComplexTypeDecl ? "complexType" : "simpleType"; 
        String termName = term.getName();
        XMLStreamReader2 r = null;
        try {
            r = XmlFactory.factory.createXMLStreamReader(file); 
            r.nextTag();
            if (!r.getLocalName().equals("schema")) {
                return 1; //not schema doc
            }
            while (r.hasNext()) {
                int eventType = r.nextTag();
                if (eventType != XMLEvent.START_ELEMENT) {
                    return 1; 
                }
                String localName = r.getLocalName();
                if (localName.equals(tagName)) {
                    String name = r.getAttributeValue(null, "name");
                    if (termName.equals(name)) {
                        return r.getLocation().getLineNumber();
                    }
                }
                r.skipElement();
            }
        } finally {
            if (r != null)
                r.closeCompletely();
        }
        
        return 1;
    }

}
