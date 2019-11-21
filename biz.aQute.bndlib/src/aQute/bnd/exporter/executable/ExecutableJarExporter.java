package aQute.bnd.exporter.executable;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.bnd.version.Version;
import aQute.lib.collections.ExtList;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;

/**
 * Exports a project or run file to an executable JAR.
 * <p>
 * This exporter supports 2 types for backward compatibility. This exporter used
 * the project launcher's execute function. However, there also was an -export
 * instruction that used the {@link Project#pack(String)} method. This method
 * was a bit more powerful. Since both exports were used, the
 * {@link #EXECUTABLE_JAR} was used from Gradle and bndtools while the
 * {@link #EXECUTABLE_PACK} was used from bnd build, we needed to handle them
 * slightly differently since it is difficult to ensure backward compatibility.
 */
@BndPlugin(name = "exporter.executablejar")
public class ExecutableJarExporter implements Exporter {
	public static final String	EXECUTABLE_JAR	= "bnd.executablejar";
	public static final String	EXECUTABLE_PACK	= "bnd.executablejar.pack";

	interface Configuration {
		boolean keep();
	}

	public ExecutableJarExporter() {}

	@Override
	public String[] getTypes() {
		return new String[] {
			EXECUTABLE_JAR, EXECUTABLE_PACK
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {
		project.prepare();
		Jar jar = null;

		if (EXECUTABLE_JAR.equals(type)) {
			jar = defaultExporter(project, options);
		} else if (EXECUTABLE_PACK.equals(type)) {
			String profile = options.getOrDefault(Constants.PROFILE, project.getProfile());
			jar = pack(project, profile);
		} else {
			project.error("Unknown type %s", type);
		}

		if (jar == null)
			return null;

		String name = jar.getName();
		String[] baseext = Strings.extension(name);
		if (baseext != null && ("bnd".equals(baseext[1]) || "bndrun".equals(baseext[1]))) {
			name = baseext[0];
		}
		name = name + Constants.DEFAULT_JAR_EXTENSION;
		return new SimpleEntry<>(name, new JarResource(jar, true));
	}

	private Jar defaultExporter(Project project, Map<String, String> options) throws Exception, IOException {
		Jar jar;
		try (ProjectLauncher launcher = project.getProjectLauncher()) {

			Configuration configuration = Converter.cnv(Configuration.class, options);

			//
			// Ensure that we do not override the -runkeep property
			// if keep is not explicitly set. We therefore take the `or`
			// of the keep option and the -runkeep. This is a bit
			// awkward caused by backward compatibility.
			//
			launcher.setKeep(configuration.keep() || project.getRunKeep());

			jar = launcher.executable();
			launcher.removeClose(jar);
			project.getInfo(launcher);
		}
		return jar;
	}

	public static Jar pack(Project project, String profile) throws Exception {
		List<String> ignore = new ExtList<>(Constants.BUNDLE_SPECIFIC_HEADERS);
		try (ProjectBuilder pb = project.getBuilder(null)) {
			ignore.remove(Constants.BUNDLE_SYMBOLICNAME);
			ignore.remove(Constants.BUNDLE_VERSION);
			ignore.add(Constants.SERVICE_COMPONENT);

			try (ProjectLauncher launcher = project.getProjectLauncher()) {
				launcher.getRunProperties()
					.put("profile", profile); // TODO
												// remove
				launcher.getRunProperties()
					.put(Constants.PROFILE, profile);
				Jar jar = launcher.executable();
				Manifest m = jar.getManifest();
				Attributes main = m.getMainAttributes();
				for (String key : project) {
					if (Character.isUpperCase(key.charAt(0)) && !ignore.contains(key)) {
						String value = project.getProperty(key);
						if (value == null)
							continue;
						Name name = new Name(key);
						String trimmed = value.trim();
						if (trimmed.isEmpty())
							main.remove(name);
						else if (Constants.EMPTY_HEADER.equals(trimmed))
							main.put(name, "");
						else
							main.put(name, value);
					}
				}

				if (main.getValue(Constants.BUNDLE_SYMBOLICNAME) == null)
					main.putValue(Constants.BUNDLE_SYMBOLICNAME, pb.getBsn());

				if (main.getValue(Constants.BUNDLE_SYMBOLICNAME) == null)
					main.putValue(Constants.BUNDLE_SYMBOLICNAME, project.getName());

				if (main.getValue(Constants.BUNDLE_VERSION) == null) {
					main.putValue(Constants.BUNDLE_VERSION, Version.LOWEST.toString());
					project.warning("No version set, uses 0.0.0");
				}

				jar.setManifest(m);
				jar.calcChecksums(new String[] {
					"SHA1", "MD5"
				});
				launcher.removeClose(jar);
				return jar;
			}
		}
	}
}
