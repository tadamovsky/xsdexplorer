package com.xsdexplorer.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;

import com.google.common.base.Preconditions;

public class EntityResolverImpl implements XMLEntityResolver
{

    private Map<Path, byte[]> fileToContent = Collections.emptyMap();

    public void setFileContentMap(Map<Path, byte[]> fileToContent) {
        this.fileToContent = fileToContent;
    }
    
    @Override
    public XMLInputSource resolveEntity(XMLResourceIdentifier resId) throws XNIException, IOException {
        String publicId = resId.getPublicId();
        String systemId = resId.getLiteralSystemId();
        String namespace = resId.getNamespace();
        String expSystemId = resId.getExpandedSystemId();
        
        System.out.println("publicId: "+publicId+", systemId: "+systemId+", namespace: "+namespace+", exp system id: "+expSystemId);
        if (systemId != null && systemId.endsWith(".dtd")) {
            return getEmptySource(resId); //ignore all dtd declarations
        }
        if ("http://www.w3.org/XML/1998/namespace".equals(namespace)) {
            return getSource(resId, "/catalog/xml.xsd");
        }
        if (!fileToContent.isEmpty()) {
            try {
                return getSourceFromByteArray(resId, getFileContent(Paths.get(new URI(expSystemId))));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        if (expSystemId != null && expSystemId.startsWith("http"))
            return getHtppInputSource(resId);
        return null;
    }

            
    public XMLInputSource getSourceFromPath(Path p) {
        byte[] string = getFileContent(p);
        XMLInputSource xmlIS = new XMLInputSource(null, p.toString(), null);
        xmlIS.setByteStream(new ByteArrayInputStream(string));
        return xmlIS;
    }

    private byte[] getFileContent(Path p) {
        byte[] string = Preconditions.checkNotNull(fileToContent.get(p), "cannot find xsd for %s", p);
        return string;
    }
    
    private XMLInputSource getSourceFromByteArray(XMLResourceIdentifier resId, byte[] string) {
        XMLInputSource xmlIS = new XMLInputSource(resId);
        xmlIS.setByteStream(new ByteArrayInputStream(string));
        return xmlIS;
    }


    private static XMLInputSource getEmptySource(XMLResourceIdentifier resId) throws IOException
    {
        XMLInputSource xmlIS = new XMLInputSource(resId);
        xmlIS.setByteStream(new ByteArrayInputStream(new byte[0]));
        return xmlIS;
    }    

    private static XMLInputSource getSource(XMLResourceIdentifier resId, String resource) throws IOException
    {
        InputStream is = SchemaLoader.class.getResourceAsStream(resource);
        if (is == null) {
            System.err.println("Warning: resource not found: "+resource);
            return null;
        }
        XMLInputSource xmlIS = new XMLInputSource(resId);
        xmlIS.setByteStream(is);
        return xmlIS;
    }
    
    //note: java internal http impl will not redirect from http to https, that's the
    //reason for using apache httpclient
    //https://stackoverflow.com/questions/1884230/httpurlconnection-doesnt-follow-redirect-from-http-to-https
    public static XMLInputSource getHtppInputSource(XMLResourceIdentifier resId) throws IOException {
        XMLInputSource xmlIS = new XMLInputSource(resId);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(resId.getExpandedSystemId());

        ClassicHttpResponse response = httpclient.executeOpen(null, httpget, null);
        HttpEntity entity = response.getEntity();
        xmlIS.setEncoding(entity.getContentEncoding());
        xmlIS.setByteStream(entity.getContent());
        return xmlIS;
    }
    
}