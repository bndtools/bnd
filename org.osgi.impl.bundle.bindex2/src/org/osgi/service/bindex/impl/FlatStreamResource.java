package org.osgi.service.bindex.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Manifest;

import org.osgi.service.bindex.Resource;

public class FlatStreamResource implements Resource {
	
	private final String location;
	private final InputStream stream;

	FlatStreamResource(String location, InputStream stream) {
		this.location = location;
		this.stream = stream;
	}

	public String getLocation() {
		return location;
	}
	
	public long getSize() {
		return 0L;
	}

	public InputStream getStream() throws IOException {
		return stream;
	}

	public Manifest getManifest() throws IOException {
		return null;
	}

	public List<String> listChildren(String prefix) throws IOException {
		return null;
	}

	public Resource getChild(String path) throws IOException {
		return null;
	}

	public void close() {
	}

}
