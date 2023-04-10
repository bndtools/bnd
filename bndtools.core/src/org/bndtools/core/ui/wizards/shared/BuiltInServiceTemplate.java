package org.bndtools.core.ui.wizards.shared;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.stream.StreamSupport;

import org.bndtools.templating.FolderResource;
import org.bndtools.templating.URLResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class BuiltInServiceTemplate extends BuiltInTemplate {

	private final Bundle bundle = FrameworkUtil.getBundle(BuiltInServiceTemplate.class);

	public BuiltInServiceTemplate(String name, String basePath) {
		super(name, "stringtemplate");
		for (Enumeration<URL> entries = bundle.findEntries(basePath, "*", true); entries.hasMoreElements();) {
			URL url = entries.nextElement();
			String resourcePath = url
				.getPath()
				.substring(basePath.length() + 1);
			if (!resourcePath.endsWith("/")) {
				// It's a file/url resource
				addInputResource(resourcePath, new URLResource(url, "UTF-8"));
			} else {
				// all folders
				for (String path : StreamSupport.stream(Paths.get(resourcePath)
					.spliterator(), false)
					.map(Path::toString)
					.toArray(String[]::new)) {
					addInputResource(path, new FolderResource());
				}
			}
		}
	}

}
