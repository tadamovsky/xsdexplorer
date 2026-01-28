package com.xsdexplorer.loader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteStreams;


public class FromZip {
    private Map<Path, byte[]> mapOfXsdStrings = new HashMap<>();
    private Path basePath;
    
    FromZip(byte[] zip) {
        openZip(new ByteArrayInputStream(zip));
    }
    
    public FromZip(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            openZip(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openZip(InputStream inputStream) {
        //must use some base path in order to resolve relative includes later on
        //also needed for xerces xml resolver
        basePath = Paths.get(System.getProperty("user.dir"));
        try  (ZipInputStream zip = new ZipInputStream( inputStream)) {
            ZipEntry entry;
            while((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                
                String fileName = entry.getName();
                if (fileName.endsWith(".xsd")) {
                    byte[] content = ByteStreams.toByteArray(zip);
                    mapOfXsdStrings.put(basePath.resolve(fileName), content);
                } 
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }    
    
    public Path getBasePath() {
        return basePath;
    }
    
    public Map<Path, byte[]> getMapOfXsdStrings() {
        return mapOfXsdStrings;
    }
    
    public List<Path> relativize(List<Path> paths) {
        return paths.stream().map(p -> basePath.relativize(p)).toList();
    }

}
