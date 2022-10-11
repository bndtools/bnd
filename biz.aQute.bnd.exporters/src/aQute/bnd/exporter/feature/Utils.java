package aQute.bnd.exporter.feature;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.ID;

import aQute.bnd.build.Container;
import aQute.bnd.exporter.feature.internal.FeatureBundle_;
import aQute.bnd.exporter.feature.internal.ID_;
import aQute.bnd.exporter.feature.json.FeatureExporterConfig;
import aQute.bnd.exporter.feature.json.JsonUtil;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.json.JSONCodec;
import biz.aQute.bnd.reporter.maven.dto.ChecksumDTO;
import biz.aQute.bnd.reporter.maven.dto.MavenCoordinatesDTO;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ChecksumPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MavenCoordinatePlugin;

public class Utils {

	public static String	FEATURE					= "bnd.feature.";
	public static String	FEATURE_BUNDLE			= FEATURE + "bundle";
	public static String	FEATURE_BUNDLE_CHECKSUM	= FEATURE_BUNDLE + "checksum.";

	public static FeatureBundle toFeatureBundle(Container container, FeatureExporterConfig exporterConfig) {

		try (Jar jar = new Jar(container.getFile()); Processor p = new Processor()) {

			ID id = idFromMavenCoord(jar, p);

			FeatureBundle_ featureBundle = new FeatureBundle_();
			featureBundle.id = id;

			if (exporterConfig.bundleHashes) {
				addMetadataChecksum(jar, p, featureBundle.getMetadata());
			}
			// maybe license

			return featureBundle;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static ID idFromMavenCoord(Jar jar, Processor p) {
		MavenCoordinatePlugin mavenCoordPlugin = new MavenCoordinatePlugin();
		mavenCoordPlugin.setReporter(p);

		final MavenCoordinatesDTO mvnCoordDTO = mavenCoordPlugin.extract(jar, Locale.forLanguageTag("und"));


		ID_ id = new ID_();
		id.groupId = mvnCoordDTO.groupId;
		id.artifactId = mvnCoordDTO.artifactId;
		id.version = mvnCoordDTO.version;
		id.type = Optional.ofNullable(mvnCoordDTO.type);
		id.classifier = Optional.ofNullable(mvnCoordDTO.classifier);

		return id;
	}

	private static void addMetadataChecksum(Jar jar, Processor p, Map<String, Object> map) {
		ChecksumPlugin plugin = new ChecksumPlugin();
		plugin.setReporter(p);

		final ChecksumDTO checksumDTO = plugin.extract(jar, Locale.forLanguageTag("und"));
		map.put(FEATURE_BUNDLE_CHECKSUM, checksumDTO);

	}

	public static String toJson(Feature feature, FeatureExporterConfig c) throws Exception {

		JSONCodec jsonCodec = new JSONCodec();

		Map<String, Object> featureMap = JsonUtil.createJsonFreatureMap(feature, c);

		String s = jsonCodec.enc()
			.indent("  ")
			.linebreak("\n")
			.put(featureMap)
			.toString();

		BufferedReader sr = new BufferedReader(new StringReader(s));

		s = sr.lines()
			.map(JsonUtil::undoComment)
			.collect(Collectors.joining(System.lineSeparator()));

		sr.close();
		return s;

	}


}
