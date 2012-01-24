package org.osgi.service.bindex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.List;
import java.util.jar.Manifest;

public interface Resource {
	
	static String NAME = "name";
	static String LOCATION = "location";
	static String SIZE = "size";
	static String LAST_MODIFIED = "lastmodified";
	
	String getLocation();
	
	Dictionary<String, Object> getProperties();
	
	long getSize();
	
	InputStream getStream() throws IOException;
	
	Manifest getManifest() throws IOException;
	
	List<String> listChildren(String prefix) throws IOException;
	
	Resource getChild(String path) throws IOException;
	
	void close();
}
