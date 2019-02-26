package aQute.launcher;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import aQute.libg.classloaders.ModifiableURLClassLoader;

public class LauncherWrapper {

	private static final Lookup		lookup				= MethodHandles.publicLookup();
	private static final MethodType	methodType			= MethodType.methodType(void.class, String[].class);

	public static void main(String[] args) throws Throwable {
		if (args.length < 1) {
			System.err.println(
				"Requires first argument to be className of launcher class who's main method will be invoked with the remainin arguments.");
			return;
		}

		try (ModifiableURLClassLoader launcherLoader = new ModifiableURLClassLoader(null)) {
			classpath().forEach(launcherLoader::addURL);

			Class<?> launcherClass = launcherLoader.loadClass(args[0]);

			MethodHandle methodHandle = lookup.findStatic(launcherClass, "main", methodType);

			methodHandle.invokeExact(shift(args));
		}
	}

	public static List<URL> classpath() throws Throwable {
		String classpath = java.lang.management.ManagementFactory.getRuntimeMXBean()
			.getClassPath();

		return Arrays.stream(classpath.split(File.pathSeparator))
			.map(path -> {
				try {
					return Paths.get(path)
						.toUri()
						.toURL();
				} catch (MalformedURLException e) {
					throw new IllegalStateException(e);
				}
			})
			.collect(Collectors.toList());
	}

	public static String[] shift(String[] args) {
		int length = args.length - 1;
		String[] shifted = new String[length];
		System.arraycopy(args, 1, shifted, 0, length);
		return shifted;
	}

}
