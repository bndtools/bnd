package aQute.bnd.exporter.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.ID;

import aQute.bnd.exporter.feature.json.FeatureExporterConfig;
import aQute.bnd.unmodifiable.Lists;
import aQute.bnd.unmodifiable.Maps;
import aQute.lib.json.JSONCodec;

public class FeatureJsonTest {
	private static final List<String>			CATEG	= Lists.of("tooling", "docker", "bundle");
	private static final Map<String, Object> VARS = Maps.of("k1", "v1", "k2", "v2");

	private static Feature mockFeature() {

		Feature feature = mock(Feature.class);
		ID id = mock(ID.class);

		when(id.getGroupId()).thenReturn("g");
		when(id.getArtifactId()).thenReturn("a");
		when(id.getVersion()).thenReturn("v");
		when(id.getClassifier()).thenReturn(Optional.of("c"));
		when(id.getType()).thenReturn(Optional.of("t"));

		when(feature.getID()).thenReturn(id);

		when(feature.getName()).thenReturn(Optional.of("name1"));
		when(feature.getLicense()).thenReturn(Optional.of("license1"));
		when(feature.getVendor()).thenReturn(Optional.of("vendor1"));
		when(feature.getDescription()).thenReturn(Optional.of("description1"));
		when(feature.getCategories()).thenReturn(CATEG);
		when(feature.getDocURL()).thenReturn(Optional.of("http://www.osgi.org"));
		when(feature.getSCM()).thenReturn(Optional.of("scm1"));

		when(feature.isComplete()).thenReturn(true);

		when(feature.getVariables()).thenReturn(VARS);

		FeatureConfiguration fc1 = mock(FeatureConfiguration.class);
		Map<String, Object> cfgMap = new TreeMap<String, Object>();
		cfgMap.put("k1", "v1");
		cfgMap.put("k2", null);
		cfgMap.put("k3", 3);
		cfgMap.put("k4", new int[] {
			1, 2
		});
		cfgMap.put("k5", Lists.of("a", "b"));
		cfgMap.put("k6", Float.MAX_VALUE);

		when(fc1.getValues()).thenReturn(cfgMap);

		FeatureConfiguration fc2 = mock(FeatureConfiguration.class);
		when(fc2.getValues()).thenReturn(Maps.of());

		when(feature.getConfigurations()).thenReturn(Maps.of("pid", fc1, "factoryPid~name", fc2));

		FeatureBundle featureBundle1 = mock(FeatureBundle.class);
		ID featureBundle1Id = mock(ID.class);

		when(featureBundle1Id.getGroupId()).thenReturn("bundle1g");
		when(featureBundle1Id.getArtifactId()).thenReturn("bundle1a");
		when(featureBundle1Id.getVersion()).thenReturn("bundle1v");
		when(featureBundle1Id.getClassifier()).thenReturn(Optional.of("bundle1c"));
		when(featureBundle1Id.getType()).thenReturn(Optional.of("bundle1t"));

		when(featureBundle1.getID()).thenReturn(featureBundle1Id);
		FeatureBundle featureBundle2 = mock(FeatureBundle.class);
		ID featureBundle2Id = mock(ID.class);

		when(featureBundle2Id.getGroupId()).thenReturn("bundle2g");
		when(featureBundle2Id.getArtifactId()).thenReturn("bundle2a");
		when(featureBundle2Id.getVersion()).thenReturn("bundle2v");
		when(featureBundle2Id.getClassifier()).thenReturn(Optional.of("bundle2c"));
		when(featureBundle2Id.getType()).thenReturn(Optional.of("bundle2t"));

		when(featureBundle2.getID()).thenReturn(featureBundle2Id);
		when(featureBundle2.getMetadata()).thenReturn(Maps.of("k1", "v1", "hashes",
			Maps.of("md5", "###", "sha1", "###", "sha256", "###"), "osgi.content", "###sha256###"));

		List<FeatureBundle> featureBundles = Lists.of(featureBundle1, featureBundle2);
		when(feature.getBundles()).thenReturn(featureBundles);

		FeatureExtension extension1 = mock(FeatureExtension.class);
		when(extension1.getKind()).thenReturn(Kind.MANDATORY);
		when(extension1.getType()).thenReturn(Type.ARTIFACTS);
		ID featureExtArt1Id = mock(ID.class);
		ID featureExtArt2Id = mock(ID.class);

		when(featureExtArt1Id.getGroupId()).thenReturn("g1");
		when(featureExtArt1Id.getArtifactId()).thenReturn("a1");
		when(featureExtArt1Id.getVersion()).thenReturn("v1");
		when(featureExtArt1Id.getClassifier()).thenReturn(Optional.of("c1"));
		when(featureExtArt1Id.getType()).thenReturn(Optional.of("t1"));
		when(featureExtArt2Id.getGroupId()).thenReturn("g2");
		when(featureExtArt2Id.getArtifactId()).thenReturn("a2");
		when(featureExtArt2Id.getVersion()).thenReturn("v2");
		when(featureExtArt2Id.getClassifier()).thenReturn(Optional.empty());
		when(featureExtArt2Id.getType()).thenReturn(Optional.empty());

		FeatureArtifact featureExtArt1 = mock(FeatureArtifact.class);
		FeatureArtifact featureExtArt2 = mock(FeatureArtifact.class);

		when(featureExtArt1.getID()).thenReturn(featureExtArt1Id);
		when(featureExtArt1.getMetadata()).thenReturn(Maps.of("my.ext.art.meta.1", "v", "foooo", "bar"));
		when(featureExtArt2.getID()).thenReturn(featureExtArt2Id);

		when(extension1.getArtifacts())
			.thenReturn(Lists.of(featureExtArt1, featureExtArt2));
		FeatureExtension extension2 = mock(FeatureExtension.class);
		when(extension2.getKind()).thenReturn(Kind.OPTIONAL);
		when(extension2.getType()).thenReturn(Type.TEXT);
		when(extension2.getText()).thenReturn(Lists.of("line1", "line2", "line3"));

		FeatureExtension extension3 = mock(FeatureExtension.class);
		when(extension3.getKind()).thenReturn(Kind.TRANSIENT);
		when(extension3.getType()).thenReturn(Type.JSON);
		when(extension3.getJSON()).thenReturn("{\"foo\":\"bar\"}");

		Map<String, FeatureExtension> extensionMap = new LinkedHashMap<>();
		extensionMap.put("ext1", extension1);
		extensionMap.put("ext2", extension2);
		extensionMap.put("ext3", extension3);
		when(feature.getExtensions()).thenReturn(extensionMap);

		return feature;

	}

	@Test
	public void testGenerateFeatureDefaultConfig() throws Exception {

		FeatureExporterConfig c = new FeatureExporterConfig();

		String s = Utils.toJson(mockFeature(), c);
		System.out.println(s);

		Map<?, ?> resultMap = new JSONCodec().dec()
			.from(s)
			.get(Map.class);

		assertEquals(resultMap.get("id"), "g:a:c:t:v");
		assertEquals(resultMap.get("name"), "name1");
		assertEquals(resultMap.get("id"), "g:a:c:t:v");
		assertEquals(resultMap.get("description"), "description1");
		assertEquals(resultMap.get("vendor"), "vendor1");
		assertEquals(resultMap.get("license"), "license1");
		assertEquals(resultMap.get("categories"), CATEG);
		assertEquals(resultMap.get("docurl"), "http://www.osgi.org");
		assertEquals(resultMap.get("scm"), "scm1");
		assertEquals(resultMap.get("complete"), true);
		assertEquals(resultMap.get("variables"), VARS);

		Assertions.assertThat((List<Object>) resultMap.get("bundles"))
			.contains("bundle1g:bundle1a:bundle1c:bundle1t:bundle1v",
				Maps.of("id", "bundle2g:bundle2a:bundle2c:bundle2t:bundle2v", "k1", "v1", "hashes",
					Maps.of("md5", "###", "sha1", "###", "sha256", "###"), "osgi.content", "###sha256###"));


		Assertions.assertThat((Map) resultMap.get("extensions"))
			.containsEntry("ext1",
				Maps.of("kind", "MANDATORY", "type", "ARTIFACTS", "artifacts",
					Lists.of(Maps.of("id", "g1:a1:c1:t1:v1", "my.ext.art.meta.1", "v", "foooo", "bar"),
						Maps.of("id", "g2:a2:v2"))))
			.containsEntry("ext2",
				Maps.of("kind", "OPTIONAL", "type", "TEXT", "text", Lists.of("line1", "line2", "line3")))
			.containsEntry("ext3", Maps.of("kind", "TRANSIENT", "type", "JSON", "json", Maps.of("foo", "bar")));
	}

}
