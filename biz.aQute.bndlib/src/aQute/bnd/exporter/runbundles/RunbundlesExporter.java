package aQute.bnd.exporter.runbundles;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.lib.strings.Strings;

@BndPlugin(name = "exporter.runbundles")
public class RunbundlesExporter implements Exporter {
	public static final String RUNBUNDLES = "bnd.runbundles";

	public RunbundlesExporter() {}

	@Override
	public String[] getTypes() {
		return new String[] {
			RUNBUNDLES
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {
		project.prepare();
		Collection<Container> runbundles = project.getRunbundles();
		Jar jar = new Jar(project.getName());
		jar.setDoNotTouchManifest();
		for (Container container : runbundles) {
			File source = container.getFile();
			String path = nonCollidingPath(jar, source.getName());
			jar.putResource(path, new FileResource(source));
		}
		return new SimpleEntry<>(jar.getName(), new JarResource(jar, true));
	}

	private String nonCollidingPath(Jar jar, String fileName) {
		String[] parts = Strings.extension(fileName);
		if (parts == null) {
			parts = new String[] {
				fileName, ""
			};
		}
		int i = 1;
		while (jar.exists(fileName)) {
			fileName = String.format("%s[%d].%s", parts[0], i++, parts[1]);
		}
		return fileName;
	}
}
