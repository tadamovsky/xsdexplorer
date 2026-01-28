package com.xsdexplorer;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import com.xsdexplorer.loader.XmlFactory;

public class AnnotationExtractor {
	
	public static String extract(String text) {
		Reader reader = new StringReader(text);
		XMLStreamReader r = null;
		try {
			r = XmlFactory.factory.createXMLStreamReader(reader);
			boolean inText = false;
			int count = 1;
			StringBuilder b = null;
			while (r.hasNext()) {
			    int eventType = r.next();
				if (!inText && eventType == XMLEvent.START_ELEMENT && r.getName().getLocalPart().equals("documentation")) {
                    //return r.getElementText(); //fails in case documentation contains xml
				    inText = true;
				    b = new StringBuilder();
				}
				else if (inText) {
				    switch (eventType) {
				    case XMLEvent.CHARACTERS:
				    case XMLEvent.CDATA:
				        b.append(r.getText());
				        break;
				    case XMLEvent.START_ELEMENT:
				        ++count;
				        break;
                    case XMLEvent.END_ELEMENT:
                        if (--count == 0) {
                            String ret = b.toString().trim().replaceAll("[ \t\r\n]+", " ");
                            return ret.isEmpty() ? null : ret;
                        }
                        break;
				    }
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (r != null)
					r.close();
			} catch (XMLStreamException ee) {
			}
		}
		return null;
	}
}
