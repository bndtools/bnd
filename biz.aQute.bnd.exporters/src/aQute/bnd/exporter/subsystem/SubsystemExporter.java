package aQute.bnd.exporter.subsystem;

import static aQute.bnd.osgi.Constants.REMOVEHEADERS;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

	private static final String	ARCHIVE_CONTENT_TYPE		= "-archiveContentType";
	private static final String	CONSTANT_MANIFEST_VERSION	= "Manifest-Version";

	private static final Map<String, String> getDefaultsMap() {

		Map<String, String> defaultsMap = new HashMap<>();
		defaultsMap.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		defaultsMap.put(SubsystemConstants.SUBSYSTEM_VERSION, "0.0.0");
		defaultsMap.put(SubsystemConstants.SUBSYSTEM_MANIFESTVERSION, "1");
		defaultsMap.put(CONSTANT_MANIFEST_VERSION, "1.0");
		return defaultsMap;
	}

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

		EsaArchiveType esaArchiveType = null;
		Boolean storeBundles = null;
		String archiveContent = project.get(ARCHIVE_CONTENT_TYPE);

		esaArchiveType = EsaArchiveType.byParameter(archiveContent);
		if (esaArchiveType == null) {
			project.error("Unknown EsaArchiveType. Use [NONE,CONTENT,ALL]");
			return null;
		}

		// did not handle esa(subsystem) files
		Collection<Container> requireBundles = getRequiredBundles(project);
		Collection<Container> runBundles = project.getRunbundles();

		Collection<Container> bundles = null;
		switch (esaArchiveType) {
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
				project.error("Unsupported EsaArchiveType. Use [NONE,CONTENT,ALL]");
				return null;
		}

		List<File> files = getBundles(bundles, project);

		for (File file : files) {
			ContainerType containerType = ContainerType.byFile(file);

			Domain domain;
			// default & last. Assume JAR
			try (JarInputStream jin = new JarInputStream(IO.stream(file))) {
				Manifest m = jin.getManifest();
				if (m != null) {
					domain = Domain.domain(m);

				} else {
					project.error("Container could not be read: " + file.getName());
					return null;
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

			if (storeBundles) {

				String path = csn + "-" + version + "." + containerType.getExtension();
				jar.putResource(path, new FileResource(file));
			}
		}

		Manifest manifest = new Manifest();
		// as overriding parameter from bndrun or bnd.bnd
		// set imports exports requireCapability and provideCapability from
		// embedded bundles

		// require capability
		MultiMap<String, Attrs> requireCapability = new MultiMap<>();
		Parameters requirementParameter = project.getRequireCapability();

		if (requirementParameter != null && !requirementParameter.entrySet()
			.isEmpty()) {
			for (Entry<String, Attrs> e : requirementParameter.entrySet()) {
				requireCapability.add(e.getKey(), e.getValue());
			}
			set(manifest.getMainAttributes(), org.osgi.framework.Constants.REQUIRE_CAPABILITY, requireCapability);
		}
		// provide capability
		MultiMap<String, Attrs> provideCapability = new MultiMap<>();
		Parameters capabilityParameter = project.getProvideCapability();
		if (provideCapability != null && !provideCapability.entrySet()
			.isEmpty()) {
			for (Entry<String, Attrs> e : capabilityParameter.entrySet()) {
				provideCapability.add(e.getKey(), e.getValue());
			}
			set(manifest.getMainAttributes(), org.osgi.framework.Constants.PROVIDE_CAPABILITY, provideCapability);
		}
		// imports
		MultiMap<String, Attrs> imports = new MultiMap<>();
		Parameters importParameter = project.getImportPackage();

		if (importParameter != null && !importParameter.entrySet()
			.isEmpty()) {
			if (type.equals(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)) {
				project.error("Modifying imports by using headers is not allowed for features");
			} else {

				for (Entry<String, Attrs> e : importParameter.entrySet()) {
					imports.add(e.getKey(), e.getValue());
				}
				set(manifest.getMainAttributes(), org.osgi.framework.Constants.IMPORT_PACKAGE, imports);
			}
		}

		// exports
		MultiMap<String, Attrs> exports = new MultiMap<>();
		Parameters exportParameter = project.getExportPackage();
		if (exportParameter != null && !exportParameter.entrySet()
			.isEmpty()) {
			for (Entry<String, Attrs> e : exportParameter.entrySet()) {
				exports.add(e.getKey(), e.getValue());
			}
			set(manifest.getMainAttributes(), org.osgi.framework.Constants.EXPORT_PACKAGE, exports);
		}
		// set headers from bndrun if not set
		headers(manifest.getMainAttributes(), project);

		// set default headers if not set
		for (Entry<String, String> entry : getDefaultsMap().entrySet()) {
			set(manifest.getMainAttributes(), entry.getKey(), entry.getValue());
		}

		set(manifest.getMainAttributes(), SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, project.getName()
			.replace(".bndrun", ""));

		ByteBufferOutputStream bout = new ByteBufferOutputStream();
		manifest.write(bout);
		jar.putResource(ContainerType.SUBSYSTEM.getManifestURI(), new EmbeddedResource(bout.toByteBuffer(), 0));

		final JarResource jarResource = new JarResource(jar, true);
		String name = project.getName();

		name = name.substring(0, name.lastIndexOf(".bndrun")) + ".esa";

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

	// puts all properties with a HEADER_PATTERN from the bndrun to the
	// Attributes, if it is neccesary
	private void headers(Attributes mainAttributes, final Project project) {
		for (String key : project.getPropertyKeys(true)) {
			if (!Verifier.HEADER_PATTERN.matcher(key)
				.matches())
				continue;

			if (mainAttributes.getValue(key) != null)
				continue;

			String value = project.getProperty(key);
			if (value == null)
				continue;

			value = value.trim();
			if (value.isEmpty() || Constants.EMPTY_HEADER.equals(value))
				continue;

			char c = key.charAt(0);
			if (Character.isUpperCase(c))
				mainAttributes.putValue(key, value);
		}
		Instructions instructions = new Instructions(project.mergeProperties(REMOVEHEADERS));
		Collection<Object> result = instructions.select(mainAttributes.keySet(), false);
		mainAttributes.keySet()
			.removeAll(result);
	}

	// puts the values to the attributes if no other value is set by this key
	private void set(Attributes mainAttributes, String key, String... values) {
		if (mainAttributes.getValue(key) != null)
			return;

		for (String value : values) {
			if (value != null) {
				mainAttributes.putValue(key, value);
				return;
			}
		}
	}
}
