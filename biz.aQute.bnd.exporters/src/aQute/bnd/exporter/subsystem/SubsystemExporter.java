package aQute.bnd.exporter.subsystem;

import static aQute.bnd.osgi.Constants.REMOVEHEADERS;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.service.subsystem.SubsystemConstants;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.export.Exporter;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.ByteBufferOutputStream;

@BndPlugin(name = "subsystem")
public class SubsystemExporter implements Exporter {

	private static final String	OSGI_INF_SUBSYSTEM_MF		= "OSGI-INF/SUBSYSTEM.MF";

	@Override
	public String[] getTypes() {
		return new String[] {
			SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE,
			SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
		};
	}

	@Override
	public Map.Entry<String, Resource> export(String type, final Project project, Map<String, String> options)
		throws Exception {
		Jar jar = new Jar(".");

		project.addClose(jar);

		Manifest manifest = new Manifest();
		manifest.getMainAttributes()
			.putValue("Manifest-Version", "1.0");
		manifest.getMainAttributes()
			.putValue("Subsystem-ManifestVersion", "1");

		List<Container> distro = project.getBundles(Strategy.LOWEST, project.getProperty(Constants.DISTRO),
			Constants.DISTRO);
		List<File> distroFiles = getBundles(distro, project);
		List<File> files = getBundles(project.getRunbundles(), project);

		MultiMap<String, Attrs> imports = new MultiMap<>();
		MultiMap<String, Attrs> exports = new MultiMap<>();
		Parameters requirements = new Parameters();
		Parameters capabilities = new Parameters();

		for (File file : files) {
			Domain domain = Domain.domain(file);
			String bsn = domain.getBundleSymbolicName()
				.getKey();
			String version = domain.getBundleVersion();

			for (Entry<String, Attrs> e : domain.getImportPackage()
				.entrySet()) {
				imports.add(e.getKey(), e.getValue());
			}

			for (Entry<String, Attrs> e : domain.getExportPackage()
				.entrySet()) {
				exports.add(e.getKey(), e.getValue());
			}

			String path = bsn + "-" + version + ".jar";
			jar.putResource(path, new FileResource(file));
		}

		headers(project, manifest.getMainAttributes());

		set(manifest.getMainAttributes(), SubsystemConstants.SUBSYSTEM_TYPE, type);

		String ssn = project.getName();

		Collection<String> bsns = project.getBsns();
		if (bsns.size() > 0) {
			ssn = bsns.iterator()
				.next();
		}
		set(manifest.getMainAttributes(), SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, ssn);

		ByteBufferOutputStream bout = new ByteBufferOutputStream();
		manifest.write(bout);

		jar.putResource(OSGI_INF_SUBSYSTEM_MF, new EmbeddedResource(bout.toByteBuffer(), 0L));

		final JarResource jarResource = new JarResource(jar, true);
		final String name = ssn + ".esa";

		return new SimpleEntry<>(name, jarResource);
	}

	private List<File> getBundles(Collection<Container> bundles, Processor reporter) throws Exception {
		List<File> files = new ArrayList<>();

		for (Container c : bundles) {
			switch (c.getType()) {
				case ERROR :
					// skip, already reported
					break;

				case PROJECT :
				case EXTERNAL :
				case REPO :
					files.add(c.getFile());
					break;
				case LIBRARY :
					c.contributeFiles(files, reporter);
					break;
			}
		}
		return files;
	}

	private void headers(final Project project, Attributes application) {
		for (String key : project.getPropertyKeys(true)) {
			if (!Verifier.HEADER_PATTERN.matcher(key)
				.matches())
				continue;

			if (application.getValue(key) != null)
				continue;

			String value = project.getProperty(key);
			if (value == null)
				continue;

			value = value.trim();
			if (value.isEmpty() || Constants.EMPTY_HEADER.equals(value))
				continue;

			char c = value.charAt(0);
			if (Character.isUpperCase(c))
				application.putValue(key, value);
		}
		Instructions instructions = new Instructions(project.mergeProperties(REMOVEHEADERS));
		Collection<Object> result = instructions.select(application.keySet(), false);
		application.keySet()
			.removeAll(result);
	}

	private void set(Attributes application, String key, String... values) {
		if (application.getValue(key) != null)
			return;

		for (String value : values) {
			if (value != null) {
				application.putValue(key, value);
				return;
			}
		}

	}

}
