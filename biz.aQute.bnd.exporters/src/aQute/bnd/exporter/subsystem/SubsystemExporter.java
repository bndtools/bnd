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
import java.util.jar.JarInputStream;
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
import aQute.lib.io.IO;

@BndPlugin(name = "subsystem")
public class SubsystemExporter implements Exporter {

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

		EasArchivType esaArchivType = null;
		Boolean storeBundles = null;
		String archiveContent = project.get("Subsystem-ArchiveContent");

		esaArchivType = EasArchivType.byParameter(archiveContent);
		if (esaArchivType == null) {
			esaArchivType = EasArchivType.CONTENT;
		}

		Collection<Container> requireBundles = getRequiredBundles(project);
		Collection<Container> runBundles = project.getRunbundles();

		Collection<Container> bundles = null;
		switch (esaArchivType) {
			case NONE :
				bundles = requireBundles;
				storeBundles = false;
				break;

			case CONTENT :

				bundles = requireBundles;
				storeBundles = true;
				break;

			case ALL :

				bundles = runBundles;
				storeBundles = true;
				break;

			default :

				throw new IllegalArgumentException("Unknown EsaArchivType: " + esaArchivType);

		}

		List<File> files = getBundles(bundles, project);

		MultiMap<String, Attrs> imports = new MultiMap<>();
		MultiMap<String, Attrs> exports = new MultiMap<>();
		MultiMap<String, Attrs> requireCapability = new MultiMap<>();
		MultiMap<String, Attrs> provideCapability = new MultiMap<>();

		for (File file : files) {
			ContainerType containerType = ContainerType.byFile(file);

			Domain domain;
			// default & last. Assume JAR
			try (JarInputStream jin = new JarInputStream(IO.stream(file))) {
				Manifest m = jin.getManifest();
				if (m != null) {
					domain = Domain.domain(m);

				} else {
					throw new Exception("Container could not be read: " + file.getName());
				}
			}

			String csn = null;
			Parameters p = domain.getParameters(containerType.getSymbolicNameKey());
			if (!p.isEmpty()) {
				csn = p.entrySet()
					.iterator()
					.next()
					.getKey();

			}
			String version = domain.get(containerType.getVersionKey());

			for (Entry<String, Attrs> e : domain.getImportPackage()
				.entrySet()) {
				imports.add(e.getKey(), e.getValue());
			}

			for (Entry<String, Attrs> e : domain.getExportPackage()
				.entrySet()) {
				exports.add(e.getKey(), e.getValue());
			}

			for (Entry<String, Attrs> e : domain.getRequireCapability()
				.entrySet()) {
				requireCapability.add(e.getKey(), e.getValue());
			}

			for (Entry<String, Attrs> e : domain.getProvideCapability()
				.entrySet()) {
				provideCapability.add(e.getKey(), e.getValue());
			}

			if (storeBundles) {

				String path = csn + "-" + version + "." + containerType.getExtension();
				jar.putResource(path, new FileResource(file));
			}
		}

		// as overriding parameter from bndrun or bnd.bnd
		Parameters requirementParameter = project.getRequireCapability();
		Parameters capabilityParameter = project.getProvideCapability();

		if (requirementParameter != null && !requirementParameter.isEmpty()) {
			requireCapability.clear();
			for (Entry<String, Attrs> e : requirementParameter.entrySet()) {
				requireCapability.add(e.getKey(), e.getValue());
			}
		}

		if (capabilityParameter != null && !capabilityParameter.isEmpty()) {
			provideCapability.clear();
			for (Entry<String, Attrs> e : capabilityParameter.entrySet()) {
				provideCapability.add(e.getKey(), e.getValue());
			}
		}
		headers(project, manifest.getMainAttributes());

		set(manifest.getMainAttributes(), org.osgi.framework.Constants.REQUIRE_CAPABILITY, requireCapability);
		set(manifest.getMainAttributes(), org.osgi.framework.Constants.PROVIDE_CAPABILITY, provideCapability);
		set(manifest.getMainAttributes(), org.osgi.framework.Constants.IMPORT_PACKAGE, imports);
		set(manifest.getMainAttributes(), org.osgi.framework.Constants.EXPORT_PACKAGE, exports);
		set(manifest.getMainAttributes(), SubsystemConstants.SUBSYSTEM_VERSION,
			project.get(SubsystemConstants.SUBSYSTEM_VERSION, "0.0.0"));

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

		jar.putResource(ContainerType.SUBSYSTEM.getManifestUri(), new EmbeddedResource(bout.toByteBuffer(), 0));

		final JarResource jarResource = new JarResource(jar, true);
		final String name = ssn + ".esa";

		return new SimpleEntry<>(name, jarResource);
	}

	private void set(Attributes mainAttributes, String key, MultiMap<String, Attrs> multiMap) {

		Parameters parameters = new Parameters();
		List<Attrs> allattrs = new ArrayList<>();
		for (Entry<String, List<Attrs>> entry : multiMap.entrySet()) {

			for (Attrs attrs : entry.getValue()) {

				parameters.add(entry.getKey(), attrs);
			}
		}
		mainAttributes.putValue(key, parameters.toString());
		// (key, allattrs);

	}

	private Collection<Container> getRequiredBundles(final Project project) throws Exception {
		Collection<Attrs> attrs = project.getParameters(Constants.RUNREQUIRES, "bnd.identity")
			.values();
		String value = "";
		for (Attrs attrs2 : attrs) {
			value += attrs2.get("id");
		}

		// project.getBundles(Strategy.LOWEST,
		// project.getProperty(Constants.RUNREQUIRES), Constants.RUNREQUIRES);

		List<Container> containers = project.getBundles(Strategy.HIGHEST, value, Constants.RUNREQUIRES);
		return containers;
	}

	private List<File> getBundles(Collection<Container> containers, Processor reporter) throws Exception {
		List<File> files = new ArrayList<>();

		for (Container container : containers) {
			switch (container.getType()) {
				case ERROR :
					// skip, already reported
					break;

				case PROJECT :
				case EXTERNAL :
				case REPO :
					files.add(container.getFile());
					break;
				case LIBRARY :
					container.contributeFiles(files, reporter);
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