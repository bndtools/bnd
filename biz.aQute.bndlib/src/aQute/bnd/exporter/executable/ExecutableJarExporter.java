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
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;

@BndPlugin(name = "exporter.executablejar")
public class ExecutableJarExporter implements Exporter {
	public static final String EXECUTABLE_JAR = "bnd.executablejar";

	interface Configuration {
		boolean keep();
	}

	public ExecutableJarExporter() {}

	@Override
	public String[] getTypes() {
		return new String[] {
			EXECUTABLE_JAR
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {
		Configuration configuration = Converter.cnv(Configuration.class, options);
		project.prepare();
		try (ProjectLauncher launcher = project.getProjectLauncher()) {
			launcher.setKeep(configuration.keep());
			Jar jar = launcher.executable();
			project.getInfo(launcher);

			String name = jar.getName();
			String[] baseext = Strings.extension(name);
			if (baseext != null && ("bnd".equals(baseext[1]) || "bndrun".equals(baseext[1]))) {
				name = baseext[0];
			}
			name = name + Constants.DEFAULT_JAR_EXTENSION;
			return new SimpleEntry<>(name, new JarResource(jar, true));
		}
	}
}
