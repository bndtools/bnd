package aQute.launcher.pre;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import aQute.lib.io.IOConstants;

public class EmbeddedLauncher {
	private static final File	CWD					= new File(System.getProperty("user.dir"));

	static final int			BUFFER_SIZE			= IOConstants.PAGE_SIZE * 16;

	public static final String	EMBEDDED_RUNPATH	= "Embedded-Runpath";
	public static Manifest		MANIFEST;

	public static void main(String... args) throws Exception {

		if (args.length > 0 && args[0].equals("-extract")) {
			extract(args);
			return;
		}

		ClassLoader cl = EmbeddedLauncher.class.getClassLoader();
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
		while (manifests.hasMoreElements()) {

			URL murl = manifests.nextElement();

			Manifest m = new Manifest(murl.openStream());
			String runpath = m.getMainAttributes()
				.getValue(EMBEDDED_RUNPATH);
			if (runpath != null) {
				MANIFEST = m;
				List<URL> classpath = new ArrayList<>();

				for (String path : runpath.split("\\s*,\\s*")) {
					URL url = toFileURL(cl.getResource(path));
					classpath.add(url);
				}

				try (URLClassLoader urlc = new URLClassLoader(classpath.toArray(new URL[0]), cl)) {
					Class<?> embeddedLauncher = urlc.loadClass("aQute.launcher.Launcher");
					Method method = embeddedLauncher.getMethod("main", String[].class);
					method.invoke(null, new Object[] {
						args
					});
				}
				return;
			}
		}
	}

	private static void extract(String... args) throws URISyntaxException, IOException {
		String to = "";
		if (args.length == 2) {
			to = args[1];
		}
		File source = new File(EmbeddedLauncher.class.getProtectionDomain()
			.getCodeSource()
			.getLocation()
			.toURI());

		if (!source.isFile()) {
			System.err.println("Cannot locate JAR file " + source);
			System.exit(-1);
		}
		File dir = to == null ? CWD : new File(to);
		dir.mkdirs();

		if (!dir.isDirectory()) {
			System.err.println("No such output directory : " + dir);
			System.exit(-1);
		}

		System.err.println("Extracting to " + dir);

		URI toFile = dir.toURI();

		try (JarFile jar = new JarFile(source)) {
			jar.stream()
				.forEach(entry -> {
					String path = entry.getName();

					// Make sure we do not copy outside our 'to' path

					while (path.startsWith("/") || path.startsWith("."))
						path = path.substring(1);

					String method;
					switch (entry.getMethod()) {
						case ZipEntry.DEFLATED :
							method = "<";
							break;

						case ZipEntry.STORED :
							method = "=";
							break;
						default :
							method = "?";
							break;
					}

					long size = entry.getSize();
					URI resolve = toFile.resolve(path);
					Path target = new File(resolve).toPath();
					System.out.printf("%s %5d %s%n", method, size, target);
					if (entry.isDirectory()) {
						try {
							if (!Files.isDirectory(target))
								Files.createDirectory(target);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					} else {

						try (InputStream inputStream = jar.getInputStream(entry)) {
							Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				});

		}
	}

	private static URL toFileURL(URL resource) throws IOException {
		//
		// Don't bother copying file urls
		//
		if (resource.getProtocol()
			.equalsIgnoreCase("file"))
			return resource;

		//
		// Need to make a copy to a temp file
		//

		File f = File.createTempFile("resource", ".jar");
		Files.createDirectories(f.getParentFile()
			.toPath());
		try (InputStream in = resource.openStream(); OutputStream out = Files.newOutputStream(f.toPath())) {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
		}
		f.deleteOnExit();
		return f.toURI()
			.toURL();
	}

}
