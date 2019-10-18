package aQute.launcher.pre;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class EmbeddedLauncher {

	private static final File	CWD					= new File(System.getProperty("user.dir"));
	private static final String	LAUNCH_TRACE		= "launch.trace";
	private static final int	BUFFER_SIZE			= 4096 * 16;

	public static final String	EMBEDDED_RUNPATH	= "Embedded-Runpath";
	public static final String	LAUNCHER_PATH		= "launcher.runpath";

	public static Manifest		MANIFEST;

	public static void main(String... args) throws Throwable {

		boolean isVerbose = isTrace();

		if (args.length > 0 && args[0].equals("-extract")) {
			if (isVerbose)
				log("running in extraction mode");
			extract(args);
			return;
		}

		findAndExecute(isVerbose, "main", void.class, args);
	}

	/**
	 * Runs the Launcher like the main method, but returns an usable exit Code.
	 * This Method was introduced to enable compatibility with the Equinox
	 * native executables.
	 *
	 * @param args the arguments to run the Launcher with
	 * @return an exit code
	 * @throws Throwable
	 */
	public int run(String... args) throws Throwable {

		boolean isVerbose = isTrace();

		if (isVerbose) {
			log("The following arguments are given:");
			for (String arg : args) {
				log(arg);
			}
		}

		String methodName = "run";
		Class<Integer> returnType = int.class;

		return findAndExecute(isVerbose, methodName, returnType, args);
	}

	/**
	 * @param isVerbose should we log debug messages
	 * @param methodName the method name to look for
	 * @param returnType the expected return type
	 * @param args the arguments for the method
	 * @return what ever the method returns
	 * @throws Throwable
	 */
	private static <T> T findAndExecute(boolean isVerbose, String methodName, Class<T> returnType, String... args)
		throws Throwable {
		ClassLoader cl = EmbeddedLauncher.class.getClassLoader();
		if (isVerbose)
			log("looking for " + EMBEDDED_RUNPATH + " in META-INF/MANIFEST.MF");
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
		while (manifests.hasMoreElements()) {
			URL murl = manifests.nextElement();
			if (isVerbose)
				log("found manifest %s", murl);

			Manifest m = new Manifest(murl.openStream());
			String runpath = m.getMainAttributes()
				.getValue(EMBEDDED_RUNPATH);
			if (runpath != null) {
				if (isVerbose)
					log("Going through the following " + EMBEDDED_RUNPATH + " %s", runpath);
				MANIFEST = m;
				return executeWithRunPath(isVerbose, methodName, returnType, cl, runpath, false, args);
			}
		}
		if (isVerbose)
			log("looking for -D" + LAUNCHER_PATH);
		String runpath = System.getProperty(LAUNCHER_PATH);
		if (runpath != null) {
			if (isVerbose)
				log("found -D" + LAUNCHER_PATH + "=%s", runpath);
			int l = runpath.length() - 1;
			if (l > 1) {
				char q = runpath.charAt(0);
				if (((q == '\'') || (q == '"')) && (q == runpath.charAt(l))) {
					runpath = runpath.substring(1, l);
				}
			}
			return executeWithRunPath(isVerbose, methodName, returnType, cl, runpath, true, args);
		}

		throw new RuntimeException(
			"Found Nothing to launch. Maybe no " + EMBEDDED_RUNPATH + " or -D" + LAUNCHER_PATH + " was set");
	}

	private static <T> T executeWithRunPath(boolean isVerbose, String methodName, Class<T> returnType, ClassLoader cl,
		String runpath, boolean pathExternal, String... args) throws Throwable {
		List<URL> classpath = new ArrayList<>();

		for (String path : runpath.split("\\s*,\\s*")) {
			URL url = !pathExternal ? toFileURL(cl.getResource(path))
				: Paths.get(path)
					.toUri()
					.toURL();
			if (isVerbose)
				log("Adding to classpath %s", url.toString());
			classpath.add(url);
		}

		if (isVerbose)
			log("creating classloader using %s", Loader.class.getName());
		try (Loader urlc = new Loader(classpath.toArray(new URL[0]), cl)) {
			if (isVerbose)
				log("Try to load aQute.launcher.Launcher");
			Class<?> aQutelauncherLauncher = urlc.loadClass("aQute.launcher.Launcher");
			if (isVerbose)
				log("looking for method %s with return type %s", methodName, returnType.toString());

			MethodHandle mh = MethodHandles.publicLookup()
				.findStatic(aQutelauncherLauncher, methodName, MethodType.methodType(returnType, String[].class));
			try {
				if (isVerbose)
					log("found method and start executing");
				return (T) mh.invoke(args);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new InvocationTargetException(e);
			}
		}
	}

	private static void log(String message, Object... args) {
		System.out.println("[" + EmbeddedLauncher.class.getSimpleName() + "] " + String.format(message, args));
	}

	private static boolean isTrace() {
		return Boolean.getBoolean(LAUNCH_TRACE);
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

	public static class Loader extends URLClassLoader {
		public Loader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}
	}

}
