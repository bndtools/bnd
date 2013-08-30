package org.osgi.service.indexer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.Manifest;

import org.osgi.service.indexer.Resource;

class FlatStreamResource implements Resource {

	private final String location;
	private final InputStream stream;

	private final Dictionary<String, Object> properties = new Hashtable<String, Object>();

	FlatStreamResource(String name, String location, InputStream stream) {
		this.location = location;
		this.stream = stream;

		properties.put(NAME, name);
		properties.put(LOCATION, location);
	}

	public String getLocation() {
		return location;
	}

	public Dictionary<String, Object> getProperties() {
		return properties;
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
