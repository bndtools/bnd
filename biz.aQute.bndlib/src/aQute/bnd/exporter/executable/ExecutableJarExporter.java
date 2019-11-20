package aQute.bnd.exporter.executable;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
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
		Jar jar;
		if (EXECUTABLE_JAR.equals(type)) {
			try (ProjectLauncher launcher = project.getProjectLauncher()) {
				//
				// Ensure that we do not override the -runkeep property
				// if keep is not explicitly set. We therefore take the `or`
				// of the keep option and the -runkeep
				//

				Configuration configuration = Converter.cnv(Configuration.class, options);
				if (configuration.keep() || Processor.isTrue(project.getProperty(Constants.RUNKEEP)))
					launcher.setKeep(true);

				jar = launcher.executable();
				launcher.removeClose(jar);
				project.getInfo(launcher);
			}
		} else if (EXECUTABLE_PACK.equals(type)) {

			String profile = options.getOrDefault(Constants.PROFILE, project.getProperty(Constants.PROFILE));
			jar = project.pack(profile);
			if (jar == null)
				return null;

		} else {
			project.error("Unknown type %s", type);
			return null;
		}

		String name = jar.getName();
		String[] baseext = Strings.extension(name);
		if (baseext != null && ("bnd".equals(baseext[1]) || "bndrun".equals(baseext[1]))) {
			name = baseext[0];
		}
		name = name + Constants.DEFAULT_JAR_EXTENSION;
		return new SimpleEntry<>(name, new JarResource(jar, true));
	}
}
