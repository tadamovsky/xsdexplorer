module xsdexplorer {
	requires transitive javafx.controls;
	requires javafx.fxml;
	//requires javafx.web;
	requires transitive javafx.graphics;
	requires java.xml;
	requires transitive xercesimpl.xsd11;
	//requires jdk.jsobject;
	requires org.apache.commons.lang3;
	requires transitive java.prefs;
    //requires java.desktop;
    requires java.base;
    requires javafx.base;
    requires com.ctc.wstx;
    requires com.google.common;
    requires org.apache.commons.io;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires xeger;
	
	opens com.xsdexplorer to javafx.fxml;
    opens com.xsdexplorer.tools.gensample to javafx.fxml;
	exports com.xsdexplorer;
}