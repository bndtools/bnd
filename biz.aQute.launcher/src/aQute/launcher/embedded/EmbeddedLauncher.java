package aQute.launcher.embedded;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

public class EmbeddedLauncher {

	public static final String	EMBEDDED_RUNPATH	= "Embedded-Runpath";
	static byte[]				buffer				= new byte[30000];

	public static void main(String... args) throws Exception {
		ClassLoader cl = EmbeddedLauncher.class.getClassLoader();
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
		while (manifests.hasMoreElements()) {

			Manifest m = new Manifest(manifests.nextElement().openStream());
			m.write(System.out);
			String runpath = m.getMainAttributes().getValue(EMBEDDED_RUNPATH);
			if (runpath != null) {
				List<URL> classpath = new ArrayList<URL>();

				for (String path : runpath.split("\\s*,\\s*")) {
					URL url = toFileURL(cl.getResource(path));
					classpath.add(url);
				}

				URLClassLoader urlc = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
				System.out.println("URLS: " + Arrays.toString(urlc.getURLs()));
				Class< ? > embeddedLauncher = urlc.loadClass("aQute.launcher.Launcher");
				Method method = embeddedLauncher.getMethod("main", new Class< ? >[] {
					String[].class
				});
				method.invoke(null, new Object[] {
					args
				});
				return;
			}
		}
	}

	private static URL toFileURL(URL resource) throws IOException {
		File f = File.createTempFile("resource", "jar");
		f.getParentFile().mkdirs();
		InputStream in = resource.openStream();
		try {
			OutputStream out = new FileOutputStream(f);
			try {
				int size = in.read(buffer);
				while ( size > 0 ) {
					out.write( buffer, 0, size);
					size = in.read(buffer);
				}
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
		return f.toURL();
	}

}
