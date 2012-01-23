package org.osgi.service.bindex;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Manifest;

public interface Resource {
	
	String getLocation();
	
	long getSize();
	
	InputStream getStream() throws IOException;
	
	Manifest getManifest() throws IOException;
	
	List<String> listChildren(String prefix) throws IOException;
	
	Resource getChild(String path) throws IOException;
	
	
	void close();
}
