package aQute.launcher.pre;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import aQute.lib.io.IOConstants;

public class EmbeddedLauncher {
	static final int			BUFFER_SIZE			= IOConstants.PAGE_SIZE * 16;

	public static final String	EMBEDDED_RUNPATH	= "Embedded-Runpath";
	public static Manifest		MANIFEST;

	public static void main(String... args) throws Exception {

		ClassLoader cl = EmbeddedLauncher.class.getClassLoader();
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
		while (manifests.hasMoreElements()) {

			URL murl = manifests.nextElement();

			Manifest m = new Manifest(murl.openStream());
			String runpath = m.getMainAttributes().getValue(EMBEDDED_RUNPATH);
			if (runpath != null) {
				MANIFEST = m;
				List<URL> classpath = new ArrayList<URL>();

				for (String path : runpath.split("\\s*,\\s*")) {
					URL url = toFileURL(cl.getResource(path));
					classpath.add(url);
				}

				try (URLClassLoader urlc = new URLClassLoader(classpath.toArray(new URL[0]), cl)) {
					Class< ? > embeddedLauncher = urlc.loadClass("aQute.launcher.Launcher");
					Method method = embeddedLauncher.getMethod("main", new Class< ? >[] {
							String[].class
					});
					method.invoke(null, new Object[] {
							args
					});
				}
				return;
			}
		}
	}

	private static URL toFileURL(URL resource) throws IOException {
		File f = File.createTempFile("resource", ".jar");
		Files.createDirectories(f.getParentFile().toPath());
		try (InputStream in = resource.openStream(); OutputStream out = Files.newOutputStream(f.toPath())) {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
		}
		f.deleteOnExit();
		return f.toURI().toURL();
	}

}
