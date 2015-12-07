package org.bndtools.templating;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ResourceMap {
	
	private final Map<String, Resource> map = new LinkedHashMap<>();

	public Resource get(String path) {
		return map.get(path);
	}
	
	public Resource remove(String path) {
		return map.remove(path);
	}

	public Collection<String> getPaths() {
		return map.keySet();
	}
	
	public boolean hasPath(String path) {
		return map.containsKey(path);
	}

	public Resource put(String path, Resource resource) {
		return map.put(path, resource);
	}
	
	public int size() {
		return map.size();
	}

	public Collection<Entry<String, Resource>> entries() {
		return map.entrySet();
	}
	
}
