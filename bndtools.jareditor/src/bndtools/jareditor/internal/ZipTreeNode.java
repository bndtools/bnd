/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.jareditor.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IAdaptable;

class ZipTreeNode implements IAdaptable {
	
	private final ZipTreeNode parent;
	private final String name;
	private final ZipEntry entry;
	private final Map<String, ZipTreeNode> children = new LinkedHashMap<String, ZipTreeNode>();

	private ZipTreeNode(ZipTreeNode parent, String name, ZipEntry entry) {
		this.parent = parent;
		this.name = name;
		this.entry = entry;
	}
	
	public ZipTreeNode getParent() {
		return parent;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}
	
	public Collection<ZipTreeNode> getChildren() {
		return Collections.unmodifiableCollection(children.values());
	}
	
	public ZipEntry getZipEntry() {
		return entry;
	}
	
	@Override
	public String toString() {
		return name;
	
	}
	public static void addEntry(Map<String, ZipTreeNode> rootMap, ZipEntry entry) {
		List<String> path = getPath(entry);
		pushEntry(null, rootMap, path, entry);
	}
	
	private static void pushEntry(ZipTreeNode parent, Map<String, ZipTreeNode> map, List<String> path, ZipEntry entry) {
		String pathPart = path.remove(0);
		ZipTreeNode node = map.get(pathPart);
		if(node == null) {
			node = new ZipTreeNode(parent, pathPart, path.isEmpty() ? entry : null);
			map.put(pathPart, node);
		}
		if(!path.isEmpty()) pushEntry(node, node.children, path, entry);
	}
	
	private static List<String> getPath(ZipEntry entry) {
		List<String> path = new LinkedList<String>();
		
		String name = entry.getName();
		int index = 0;
		while(index < name.length()) {
			int nextSlash = name.indexOf('/', index);
			
			if(nextSlash == -1) {
				path.add(name.substring(index));
				break;
			} else {
				path.add(name.substring(index, nextSlash + 1));
				index = nextSlash + 1;
			}
		}
		
		return path;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if(adapter == JarEntry.class) {
			return entry;
		}
		return null;
	}

}
