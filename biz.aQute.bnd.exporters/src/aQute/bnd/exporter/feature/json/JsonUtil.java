package aQute.bnd.exporter.feature.json;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

import aQute.bnd.unmodifiable.Lists;
import aQute.lib.json.JSONCodec;

public class JsonUtil {

	public static String createJsonFreatureIdAsGav(ID id) {
		if (Objects.isNull(id)) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(id.getGroupId());
		builder.append(":");
		builder.append(id.getArtifactId());
		if (id.getClassifier()
			.isPresent()) {
			builder.append(":");
			builder.append(id.getClassifier()
				.get());
		}
		if (id.getType()
			.isPresent()) {
			builder.append(":");
			builder.append(id.getType()
				.get());
		}
		builder.append(":");
		builder.append(id.getVersion());

		return builder.toString();

	}

	public static Map<String, Object> createJsonFreatureArtefactMap(FeatureArtifact fa) {
		if (Objects.isNull(fa)) {
			return null;
		}

		Map<String, Object> faMap = new LinkedHashMap<>();

		ID id = fa.getID();
		faMap.put("id", createJsonFreatureIdAsGav(id));
		StringBuilder builder = new StringBuilder();

		for (Entry<String, Object> entry : fa.getMetadata()
			.entrySet()) {

			faMap.put(entry.getKey(), entry.getValue());

		}

		return faMap;

	}

	public static Map<String, Object> createJsonFreatureMap(Feature feature, FeatureExporterConfig cfg)
		throws Exception {

		Map<String, Object> jsonMapFeature = new LinkedHashMap<>();
		// ID
		String gav = createJsonFreatureIdAsGav(feature.getID());
		jsonMapFeature.put("id", gav);
		// Meta

		feature.getName()
			.ifPresent(v -> jsonMapFeature.put("name", v));
		feature.getDescription()
			.ifPresent(v -> jsonMapFeature.put("description", v));
		feature.getVendor()
			.ifPresent(v -> jsonMapFeature.put("vendor", v));
		feature.getLicense()
			.ifPresent(v -> jsonMapFeature.put("license", v));
		if (!feature.getCategories()
			.isEmpty()) {
			jsonMapFeature.put("categories", feature.getCategories());
		}
		feature.getDocURL()
			.ifPresent(v -> jsonMapFeature.put("docurl", v));
		feature.getSCM()
			.ifPresent(v -> jsonMapFeature.put("scm", v));
		if (cfg.structureDocumentation) {
			comment(jsonMapFeature, "A complete feature has no external dependencies");
		}
		if (feature.isComplete()) {
			jsonMapFeature.put("complete", feature.isComplete());
		}

		Map<String, Object> vars = feature.getVariables();

		if (!vars.isEmpty()) {
			if (cfg.structureDocumentation) {
				comment(jsonMapFeature,
					"Variables used in configuration and framework properties are substituted at launch time.");
			}
			jsonMapFeature.put("variables", vars);
		}

		Map<String, Map<String, Object>> jsonConfigurationsMap = createJsonConfigurationMap(
			feature.getConfigurations());
		if (!jsonConfigurationsMap.isEmpty()) {
			if (cfg.structureDocumentation) {
				comment(jsonMapFeature,
					Lists.of("The configurations are specified following the format defined by the OSGi Configurator",
						"specification: https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html",
						"Variables declared in the variables section can be used for late binding of variables",
						"they can be specified with the Launcher, or the default from the variables section is used.",
						"Factory configurations can be specified using the named factory syntax, which separates",
						"The factory PID and the name with a tilde '~'"));
			}
			jsonMapFeature.put("configuration", jsonConfigurationsMap);
		}

		List<FeatureBundle> bundles = feature.getBundles();
		if (!bundles.isEmpty()) {
			List<Object> jsonBundles = createJsonBundlesList(bundles);
			if (cfg.structureDocumentation) {
				comment(jsonMapFeature,
					Lists.of("The bundles that are part of the feature. Bundles are referenced using Maven",
						"coordinates and can have additional metadata associated with them. Bundles can",
						"specified as either a simple string (the Maven coordinates of the bundle) or",
						"as an object with 'id' and additional metadata."));
			}
			jsonMapFeature.put("bundles", jsonBundles);
		}

		Map<String, Map<String, Object>> jsonExtensionsMap = createJsonExtensionsMap(feature.getExtensions());
		if (!jsonExtensionsMap.isEmpty()) {
			if (cfg.structureDocumentation) {

				comment(jsonMapFeature,
					Lists.of("Features Extensions can include custom content. This makes",
						"it possible to keep custom entities and information relating to the Feature together",
						"with the rest of the Feature."));
			}
			jsonMapFeature.put("extensions", jsonExtensionsMap);
		}
		return jsonMapFeature;
	}

	private static Map<String, Map<String, Object>> createJsonExtensionsMap(Map<String, FeatureExtension> extensionsMap)
		throws Exception {
		if (Objects.isNull(extensionsMap)) {
			return null;
		}

		Map<String, Map<String, Object>> jsonExtensionsMap = new LinkedHashMap<>();
		for (Entry<String, FeatureExtension> extensionsEntry : extensionsMap.entrySet()) {
			Entry<String, Map<String, Object>> jsonExtensionsEntry = createJsonExtensionEntry(extensionsEntry);
			jsonExtensionsMap.put(jsonExtensionsEntry.getKey(), jsonExtensionsEntry.getValue());
		}
		return jsonExtensionsMap;

	}

	private static Entry<String, Map<String, Object>> createJsonExtensionEntry(
		Entry<String, FeatureExtension> extensionsEntry) throws Exception {

		FeatureExtension featureExtension = extensionsEntry.getValue();
		if (Objects.isNull(featureExtension)) {
			return null;
		}
		Map<String, Object> jsonExtensionEntryValueMap = new LinkedHashMap<>();
		jsonExtensionEntryValueMap.put("kind", featureExtension.getKind());
		jsonExtensionEntryValueMap.put("type", featureExtension.getType());

		switch (featureExtension.getType()) {
			case ARTIFACTS :
				jsonExtensionEntryValueMap.put("artifacts", featureExtension.getArtifacts()
					.stream()
					.map(JsonUtil::createJsonFreatureArtefactMap)
					.collect(Collectors.toList()));
				break;
			case JSON :
				Map<?, ?> jsonMap = new JSONCodec().dec()
					.from(featureExtension.getJSON())
					.get(Map.class);

				jsonExtensionEntryValueMap.put("json", jsonMap);
				break;
			case TEXT :
				jsonExtensionEntryValueMap.put("text", featureExtension.getText());
				break;
			default :
				break;

		}

		Entry<String, Map<String, Object>> jsonExtensionEntry = new AbstractMap.SimpleEntry<>(extensionsEntry.getKey(),
			jsonExtensionEntryValueMap);
		return jsonExtensionEntry;
	}

	private static List<Object> createJsonBundlesList(List<FeatureBundle> featureBundles) {

		if (Objects.isNull(featureBundles)) {
			return null;
		}

		List<Object> jsonBundlesList = new ArrayList<>();

		for (FeatureBundle featureBundle : featureBundles) {

			String gav = createJsonFreatureIdAsGav(featureBundle.getID());
			if (Objects.nonNull(featureBundle.getMetadata()) && !featureBundle.getMetadata()
				.isEmpty()) {

				Map<String, Object> jsonBundle = new LinkedHashMap<>();
				jsonBundle.put("id", gav);
				for (Entry<String, Object> entryMetadata : featureBundle.getMetadata()
					.entrySet()) {

					jsonBundle.put(entryMetadata.getKey(), entryMetadata.getValue());
				}
				jsonBundlesList.add(jsonBundle);
			} else {
				jsonBundlesList.add(gav);

			}
		}
		return jsonBundlesList;
	}

	private static Map<String, Map<String, Object>> createJsonConfigurationMap(
		Map<String, FeatureConfiguration> configurationsMap) {
		if (Objects.isNull(configurationsMap)) {
			return null;
		}
		Map<String, Map<String, Object>> jsonConfigurationMap = new LinkedHashMap<>();
		for (Entry<String, FeatureConfiguration> configurationsEntry : configurationsMap.entrySet()) {
			Entry<String, Map<String, Object>> jsonConfigurationEntry = createJsonConfigurationEntry(
				configurationsEntry);
			jsonConfigurationMap.put(jsonConfigurationEntry.getKey(), jsonConfigurationEntry.getValue());
		}
		return jsonConfigurationMap;
	}

	public static Entry<String, Map<String, Object>> createJsonConfigurationEntry(
		Entry<String, FeatureConfiguration> entry) {
		FeatureConfiguration featureConfiguration = entry.getValue();
		if (Objects.isNull(featureConfiguration)) {
			return null;
		}
		Map<String, Object> jsonConfigurationEntryValueMap = new LinkedHashMap<>();
		Map<String, Object> configurationValuesMap = featureConfiguration.getValues();
		for (Entry<String, Object> configurationValuesEntry : configurationValuesMap.entrySet()) {

			String jsonKey = configurationValuesEntry.getKey();
			Object jsonValue = configurationValuesEntry.getValue();

			String type = TypeConverter.convertObjectToTyped(jsonValue);
			if (type != null && !type.isEmpty()) {
				jsonKey = jsonKey + ":" + type;
			}
			jsonConfigurationEntryValueMap.put(jsonKey, jsonValue);
		}
		Entry<String, Map<String, Object>> jsonConfigurationEntry = new AbstractMap.SimpleEntry<String, Map<String, Object>>(
			entry.getKey(), jsonConfigurationEntryValueMap);

		return jsonConfigurationEntry;
	}

	static void comment(Map<String, Object> map, String line) {
		map.put(newCommentKey(), "");
		map.put(newCommentKey(), "// " + line);
	}

	static void comment(Map<String, Object> map, List<String> lines) {
		map.put(newCommentKey(), "");
		map.put(newCommentKey(), "/*");
		for (String line : lines) {
			map.put(newCommentKey(), " * " + line);
		}
		map.put(newCommentKey(), " */");
	}

	public static String undoComment(String in) {
		return in.replaceAll("(.*)\\\"____COMMENT____.*\\\":\\\"(.*)\\\",", "$1$2");
	}

	private static String newCommentKey() {
		return "____COMMENT____" + UUID.randomUUID();
	}
}
