package aQute.bnd.exporter.feature;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.osgi.framework.Constants;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.exporter.feature.internal.Feature_;
import aQute.bnd.exporter.feature.internal.ID_;
import aQute.bnd.exporter.feature.json.FeatureExporterConfig;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;
import aQute.bnd.unmodifiable.Lists;
import aQute.lib.io.IO;

@BndPlugin(name = "Feature Exporter")
public class FeatureExporter implements Exporter {

	private Project					project;

	private String					type;

	public static String			TYPE_OSGI_FEATURE			= "osgi.feature";
	private FeatureExporterConfig	exporterConfig	= new FeatureExporterConfig();

	@Override
	public String[] getTypes() {
		return new String[] {
			TYPE_OSGI_FEATURE
		};
	}

	@Override
	public Map.Entry<String, Resource> export(String type, final Project project, Map<String, String> options)
		throws Exception {

		this.project = project;
		this.type = type;
		exporterConfig.bundleHashes = parse(options, "bundleHashes");
		exporterConfig.structureDocumentation = parse(options, "structureDocumentation");
		Entry<String, Resource> featureRes = doFeatureFile(exporterConfig);
		return featureRes;
	}

	private boolean parse(Map<String, String> options, String key) {
		return Boolean.parseBoolean(options.getOrDefault(key, "false"));
	}

	private Map.Entry<String, Resource> doFeatureFile(FeatureExporterConfig cfg) throws Exception {

		Feature feature = feature();

		// FileWriter
		String fileName = featureName(project);
		File jsonPath = project.getTargetDir()
			.toPath()
			.resolve(fileName)
			.toFile();

		jsonPath.createNewFile();

		String json = Utils.toJson(feature, exporterConfig);
		IO.write(json.getBytes(), jsonPath);
		Resource jsonResource = new FileResource(jsonPath);
		return new AbstractMap.SimpleEntry<String, Resource>(jsonPath.toString(), jsonResource);
	}

	private Feature feature() {
		Feature_ feature = new Feature_();
		ID_ fId = new ID_();
		fId.groupId = groupId();
		fId.artifactId = artifactId();
		fId.version = version().orElse("0.0.0-INITIAL");
		feature.id = fId;

		feature.name = name();
		feature.description = decription();
		feature.license = license();
		feature.vendor = vendor();
		feature.categories = categories();
		feature.docURL = docURL();

		feature.configurations = featureConfigurations();
		feature.bundles = featureBundles();
		feature.variables = variables();
		feature.extensions = extensions();
		return feature;
	}

	private String groupId() {
		String groupId = project.getProperty("-groupid", "undefinedGroupId");

		return groupId;
	}

	private String artifactId() {
		String bsn = project.getBundleSymbolicName()
			.getKey();
		if (bsn == null || bsn.isEmpty()) {
			bsn = "undefinedArtifactId";
		}
		return bsn;
	}

	private Optional<String> docURL() {
		return Optional.ofNullable(project.getBundleDocURL());
	}

	private List<String> categories() {
		String cat = project.getBundleCategory();
		if (cat == null || cat.isEmpty()) {
			return Lists.of();
		}
		return Lists.of(project.getBundleCategory());
	}

	private Optional<String> version() {

		return Optional.ofNullable(project.getBundleVersion());
	}

	private Optional<String> decription() {

		return Optional.ofNullable(project.getBundleDescription());
	}

	private Optional<String> license() {

		return Optional.ofNullable(project.get(Constants.BUNDLE_LICENSE));
	}

	private Optional<String> name() {

		return Optional.ofNullable(project.getBundleName());
	}

	private Optional<String> vendor() {

		return Optional.ofNullable(project.getBundleVendor());
	}

	private static String featureName(Project project) {

		String pName = project.getName();
		if ("bnd.bnd".equals(pName)) {
			return "feature.json";
		}
		return "feature-" + pName.replace(".bndrun", ".json");
	}

	Map<String, FeatureConfiguration> featureConfigurations() {

		return new HashMap<>();

	}

	List<FeatureBundle> featureBundles() {

		try {
			return Optional.ofNullable(project.getRunbundles()
				.stream()
				.map(c -> Utils.toFeatureBundle(c, exporterConfig))
				.collect(Collectors.toList()))
				.orElse(new ArrayList<>());
		} catch (Exception e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	// Discussion in OSGi-WG if capabilities or requirement shpild be a
	// Attribute of Features.

	// Optional<List<Capability>> capabilities() {
	//
	// Parameters parameters = project.getProvideCapability();
	// List<Capability> list = CapReqBuilder.getCapabilitiesFrom(parameters);
	// return Optional.ofNullable(list);
	//
	// }

	// Optional<List<MatchingRequirement>> requirement() {
	//
	// Parameters parameters = project.getRequireCapability();
	// List<MatchingRequirement> list =
	// CapReqBuilder.getRequirementsFrom(parameters)
	// .stream()
	// .map(MRI::new)
	// .collect(Collectors.toList());
	//
	// return Optional.ofNullable(list);
	// }

	Map<String, Object> variables() {

		Map<String, Object> vars = project.getProperties()
			.entrySet()
			.stream()
			.filter(isVariable())
			.collect(Collectors.toMap(e -> e.getKey()
				.toString(),
				e -> e.getValue()
					.toString()));
		return vars;
	}

	private Predicate<? super Entry<Object, Object>> isVariable() {

		return e -> {
			if (e.getKey() == null) {
				return false;
			}
			String key = (String) e.getKey();
			if (key.toString()
				.startsWith("-")) {
				return false;
			}
			if (key.startsWith("Bundle-")) {
				return false;
			}
			if (key.startsWith("basedir")) {
				return false;
			}
			if (key.startsWith("Provide-Capability")) {
				return false;
			}
			if (key.startsWith("Require-Capability")) {
				return false;
			}

			return true;
		};
	}

	private Map<String, FeatureExtension> extensions() {

		Map<String, FeatureExtension> exts = project.getPlugins(ExtensionPlugin.class)
			.stream()
			.collect(Collectors.toMap(p -> p.toExtension()
				.getName(), ExtensionPlugin::toExtension));

		return exts;
	}
}
