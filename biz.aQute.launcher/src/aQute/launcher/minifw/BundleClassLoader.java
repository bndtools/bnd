package aQute.launcher.minifw;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

class BundleClassLoader extends URLClassLoader implements BundleReference {
	private final Bundle bundle;

	BundleClassLoader(File file, ClassLoader parent, Bundle bundle) throws IOException {
		super(new URL[] {
			file.toURI()
				.toURL()
		}, parent);
		this.bundle = bundle;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}
}
