package aQute.libg.classloaders;

import static java.lang.invoke.MethodHandles.publicLookup;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class URLClassLoaderWrapper {
	private final URLClassLoader	loader;
	private final MethodHandle		addURL;

	public URLClassLoaderWrapper(ClassLoader loader) throws Exception {
		this.loader = (URLClassLoader) loader;
		Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		m.setAccessible(true);
		addURL = publicLookup().unreflect(m);
	}

	public void addURL(URL url) throws Exception {
		try {
			addURL.invoke(loader, url);
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public Class<?> loadClass(String name) throws Exception {
		return loader.loadClass(name);
	}
}
