package org.bndtools.core.ui.icons;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public final class Icons {

	private static Properties properties = new Properties();

	static {
		try (InputStream in = Icons.class.getResourceAsStream("/icons.properties")) {
			properties.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load icons.properties");
		}
	}

	public static String path(String name) {
		return properties.getProperty("icons." + name, "icons/missing.gif");
	}

	public static ImageDescriptor desc(String name) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, path(name));
	}

}
