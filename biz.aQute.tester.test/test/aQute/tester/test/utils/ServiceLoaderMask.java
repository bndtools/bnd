package aQute.tester.test.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

public class ServiceLoaderMask extends ClassLoader {
	@Override
	public URL getResource(String name) {
		if (name != null && name.startsWith("META-INF/services")) {
			return null;
		}
		return super.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if (name != null && name.startsWith("META-INF/services")) {
			return Collections.emptyEnumeration();
		}
		return super.getResources(name);
	}
}
