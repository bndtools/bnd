package aQute.libg.classloaders;

import java.lang.reflect.*;
import java.net.*;

public class URLClassLoaderWrapper {
	final URLClassLoader	loader;
	final Method			addURL;

	public URLClassLoaderWrapper(ClassLoader loader) throws Exception {
		this.loader = (URLClassLoader) loader;
		addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		addURL.setAccessible(true);
	}

	public void addURL(URL url) throws Exception {
		try {
			addURL.invoke(loader, url);
		}
		catch (InvocationTargetException ite) {
			throw (Exception) ite.getTargetException();
		}
	}

	public Class< ? > loadClass(String name) throws Exception {
		return loader.loadClass(name);
	}
}
