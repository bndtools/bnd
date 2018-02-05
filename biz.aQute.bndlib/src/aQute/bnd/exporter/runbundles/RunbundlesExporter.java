package aQute.bnd.exporter.runbundles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

@BndPlugin(name = "exporter.runbundles")
public class RunbundlesExporter implements Exporter {
	public static final String RUNBUNDLES = "bnd.runbundles";

	interface Configuration {
		File outputDir();
	}

	public RunbundlesExporter() {}

	@Override
	public String[] getTypes() {
		return new String[] {
			RUNBUNDLES
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {
		Configuration configuration = Converter.cnv(Configuration.class, options);
		project.prepare();
		Path outputPath = configuration.outputDir()
			.toPath();
		IO.mkdirs(outputPath);
		Collection<Container> runbundles = project.getRunbundles();
		for (Container container : runbundles) {
			Path source = container.getFile()
				.toPath();
			Path target = nonCollidingPath(outputPath, source);
			Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
		}
		return new SimpleImmutableEntry<>(outputPath.toAbsolutePath()
			.toString(), new FileResource(outputPath));
	}

	private Path nonCollidingPath(Path outputDir, Path source) {
		String fileName = source.getFileName()
			.toString();
		Path target = outputDir.resolve(fileName);
		String[] parts = Strings.extension(fileName);
		if (parts == null) {
			parts = new String[] {
				fileName, ""
			};
		}
		int i = 1;
		while (Files.exists(target)) {
			target = outputDir.resolve(String.format("%s[%d].%s", parts[0], i++, parts[1]));
		}
		return target;
	}
}
