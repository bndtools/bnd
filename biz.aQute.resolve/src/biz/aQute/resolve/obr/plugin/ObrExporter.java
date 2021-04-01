package biz.aQute.resolve.obr.plugin;

import static aQute.bnd.osgi.Constants.RESOLVE_EXCLUDESYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Resource;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.service.export.Exporter;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.RunResolution;

@BndPlugin(name = "OBR Exporter")
public class ObrExporter implements Exporter {

	static String TYPE = "bnd.exporter.obr";

	@Override
	public String[] getTypes() {
		return new String[] {
			TYPE
		};
	}

	@Override
	public Entry<String, aQute.bnd.osgi.Resource> export(String type, Project project, Map<String, String> options)
		throws Exception {

		if (!(project instanceof Bndrun)) {
			project.error("Project must be of type bndrun");
			return null;
		}

		String outputdir = options.get("outputdir");
		String name = options.getOrDefault("name", removeExtension(project.getName()) + ".xml");
		String excludesystem = options.getOrDefault("excludesystem", "true");

		File outputDirectory = outputdir == null ? project.getTargetDir() : project.getFile(outputdir);

		project.setProperty(RESOLVE_EXCLUDESYSTEM, excludesystem);

		File obrIndex = new File(outputDirectory, name);
		RunResolution resolution = ((Bndrun) project).resolve();
		List<Resource> resources = resolution.getOrderedResources();

		exportOBR(project, resources, obrIndex);

		if (project.isOk()) {
			FileResource result = new FileResource(obrIndex);
			return new SimpleEntry<>("obr", result);
		}
		return null;
	}

	private static void exportOBR(Project project, Collection<Resource> resources, File output) {
		try {
			XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();
			xmlResourceGenerator.resources(resources);
			xmlResourceGenerator.save(output);
		} catch (IOException e) {
			project.exception(e, "Failed to generate OBR", project);
		}
	}

	private static String removeExtension(String filename) {
		return filename.substring(0, filename.lastIndexOf('.'));
	}

}
