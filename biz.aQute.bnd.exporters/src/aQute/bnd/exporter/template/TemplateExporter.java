package aQute.bnd.exporter.template;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;

@BndPlugin(name = "Templated Exporter")
public class TemplateExporter implements Exporter {

	public static final String	TEMPLATE_EXPORTER_TYPE		= "bnd.templated";

	public TemplateExporter() {}

	@Override
	public String[] getTypes() {
		return new String[] {
			TEMPLATE_EXPORTER_TYPE
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {

		Configuration config = Converter.cnv(Configuration.class, options);
		project.prepare();
		if (!checkConfig(project, config)) {
			return null;
		}
		Jar jar;
		if (!Strings.nonNullOrEmpty(config.template())) {
			jar = new Jar(project.getName());
		} else {
			jar = new Jar(project.getName(), project.getFile(config.template()));
		}
		if (!config.metadata()) {
			jar.setDoNotTouchManifest();
		}

		Collection<Container> runbundles = project.getRunbundles();
		if (!runbundles.isEmpty()) {
			String runBundlesDir = checkSlash(config.runbundlesDir());
			addContainers(jar, runbundles, runBundlesDir, config.runbundlesOverwriteExisting());
		}

		Collection<Container> runpathBundles = project.getRunpath();
		if (!runpathBundles.isEmpty()) {
			String runpathDir = checkSlash(config.runpathDir());
			addContainers(jar, runpathBundles, runpathDir, config.runpathOverwriteExisting());
		}

		String frameworkTarget = config.frameworkTarget();
		if (!frameworkTarget.isEmpty()) {
			String target = frameworkTarget;
			if (!frameworkTarget.endsWith("/")) {
				if (project.getRunFw()
					.size() > 1) {
					project.error(
						"-runfw contains more then one entry, which is not compatible with a frameworkTarget that does not end with /");
					return null;
				}
				Container framework = project.getRunFw()
					.iterator()
					.next();
				jar.putResource(frameworkTarget, new FileResource(framework.getFile()));
			} else {
				addContainers(jar, project.getRunFw(), target, false);
			}

		}

		String includeresource = project.mergeProperties(Constants.INCLUDERESOURCE);
		if (!Strings.nonNullOrEmpty(includeresource)) {
			includeresource = project.mergeProperties(Constants.INCLUDE_RESOURCE);
		}
		if (Strings.nonNullOrEmpty(includeresource)) {
			try (Builder b = new Builder()) {
				if (config.metadata()) {
					b.setParent(project);
				}
				b.setBase(project.getBase());
				b.setProperty(Constants.INCLUDERESOURCE, includeresource);
				b.setProperty(Constants.RESOURCEONLY, "true");
				b.build();
				if (b.isOk()) {
					Jar resources = b.getJar();
					jar.addAll(resources);
					// make sure copied resources are not closed
					// when Builder and its Jar are closed
					resources.getResources()
						.clear();
				}
				project.getInfo(b);
			}
		} else if (config.metadata()) {
			try (Builder b = new Builder(project)) {
				b.setBase(project.getBase());
				b.setProperty(Constants.RESOURCEONLY, "true");
				b.build();
				if (b.isOk()) {
					Jar resources = b.getJar();
					jar.addAll(resources);
					jar.setManifest(resources.getManifest());
					// make sure copied resources are not closed
					// when Builder and its Jar are closed
					resources.getResources()
						.clear();
				}
				b.getWarnings()
					.stream()
					.filter(s -> !s.startsWith("The JAR is empty: The instructions for the JAR named"))
					.forEach(s -> project.warning(s));
				b.getErrors()
					.forEach(e -> project.error(e));
			}
		}
		return new SimpleEntry<>(jar.getName(), new JarResource(jar, true));
	}

	private boolean checkConfig(Project project, Configuration config) throws Exception {
		boolean result = true;
		if (!project.getRunbundles()
			.isEmpty() && config.runbundlesDir() == null) {
			project.error("When -runbundles is present, the runbundlesDir Attribute must be set");
			result = false;
		}
		if (!project.getRunpath()
			.isEmpty() && config.runpathDir() == null) {
			project.error("When -runpath is present, the runpathDir Attribute must be set");
			result = false;
		}
		return result;
	}

	private void addContainers(Jar jar, Collection<Container> containers, String directory, boolean overwrite)
		throws IOException {
		for (Container container : containers) {
			File source = container.getFile();
			String path = makePath(jar, source.getName(), directory, overwrite);
			jar.putResource(path, new FileResource(source));
		}
	}

	private String checkSlash(String path) {
		String result = path;
		if (!path.isEmpty() && !path.endsWith("/")) {
			path = path.concat("/");
		}
		return path;
	}

	private String makePath(Jar jar, String fileName, String prefix, boolean overwrite) {
		String[] parts = Strings.extension(fileName);
		if (parts == null) {
			parts = new String[] {
				fileName, ""
			};
		}
		fileName = prefix.concat(fileName);
		if (!overwrite) {
			int i = 1;
			while (jar.exists(fileName)) {
				fileName = String.format("%s%s[%d].%s", prefix, parts[0], i++, parts[1]);
			}
		}
		return fileName;
	}
}
