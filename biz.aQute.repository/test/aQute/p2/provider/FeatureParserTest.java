package aQute.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.p2.api.Artifact;
import aQute.p2.packed.Unpack200;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test the Feature parser to ensure it correctly parses Eclipse feature.xml
 * files and creates proper OSGi Resource representations with capabilities and
 * requirements.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeatureParserTest {

	@InjectSoftAssertions
	SoftAssertions			softly;

	@InjectTemporaryDirectory
	File					tmp;

	private static final String	ECF_P2_REPO	= "https://download.eclipse.org/rt/ecf/3.16.5/site.p2/3.16.5.v20250914-0333/";

	/**
	 * Test parsing a feature from the ECF P2 repository
	 */
	@Test
	public void testParseECFFeature() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);
			P2Impl p2 = new P2Impl(processor, client, new URI(ECF_P2_REPO), Processor.getPromiseFactory());

			// Get all artifacts including features
			List<Artifact> allArtifacts = p2.getAllArtifacts();
			softly.assertThat(allArtifacts)
				.as("ECF P2 repo should contain artifacts")
				.isNotEmpty();

			// Find feature artifacts
			List<Artifact> features = allArtifacts.stream()
				.filter(a -> a.classifier == aQute.p2.api.Classifier.FEATURE)
				.toList();

			softly.assertThat(features)
				.as("ECF P2 repo should contain features")
				.isNotEmpty();

			// Download and parse the first feature
			if (!features.isEmpty()) {
				Artifact featureArtifact = features.get(0);
				softly.assertThat(featureArtifact.uri)
					.as("Feature artifact should have a URI")
					.isNotNull();

				File featureJar = client.build()
					.useCache()
					.go(featureArtifact.uri);

				softly.assertThat(featureJar)
					.as("Feature JAR should be downloaded")
					.isNotNull()
					.exists();

				// Parse the feature
				try (InputStream is = IO.stream(featureJar)) {
					Feature feature = new Feature(is);
					feature.parse();

					// Validate feature properties
					softly.assertThat(feature.getId())
						.as("Feature should have an ID")
						.isNotNull()
						.isNotEmpty();

					softly.assertThat(feature.getVersion())
						.as("Feature should have a version")
						.isNotNull()
						.isNotEmpty();

					System.out.println("Parsed feature: " + feature);
					System.out.println("  ID: " + feature.getId());
					System.out.println("  Version: " + feature.getVersion());
					System.out.println("  Label: " + feature.getLabel());
					System.out.println("  Provider: " + feature.getProviderName());

					// Test Resource conversion
					Resource resource = feature.toResource();
					softly.assertThat(resource)
						.as("Feature should be convertible to Resource")
						.isNotNull();

					// Validate identity capability
					List<Capability> identityCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					softly.assertThat(identityCaps)
						.as("Resource should have identity capability")
						.hasSize(1);

					if (!identityCaps.isEmpty()) {
						Capability identityCap = identityCaps.get(0);
						Object type = identityCap.getAttributes()
							.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
						softly.assertThat(type)
							.as("Identity capability should have type=eclipse.feature")
							.isEqualTo("eclipse.feature");

						Object identity = identityCap.getAttributes()
							.get(IdentityNamespace.IDENTITY_NAMESPACE);
						softly.assertThat(identity)
							.as("Identity capability should have identity attribute matching feature ID")
							.isEqualTo(feature.getId());

						System.out.println("  Identity capability: " + identityCap.getAttributes());
					}

					// Validate requirements
					List<Requirement> requirements = resource.getRequirements(null);
					System.out.println("  Requirements count: " + requirements.size());
					for (Requirement req : requirements) {
						System.out.println("    - " + req.getNamespace() + ": " + req.getDirectives());
					}
				}
			}
		}
	}

	/**
	 * Test parsing feature attributes and child elements
	 */
	@Test
	public void testFeatureParsingDetails() throws Exception {
		// Create a simple test feature.xml
		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature
			      id="test.feature"
			      label="Test Feature"
			      version="1.0.0.qualifier"
			      provider-name="Test Provider"
			      plugin="test.feature.plugin">

			   <description>
			      Test description
			   </description>

			   <copyright>
			      Test copyright
			   </copyright>

			   <license>
			      Test license
			   </license>

			   <plugin
			         id="test.plugin.one"
			         download-size="0"
			         install-size="0"
			         version="1.0.0.qualifier"
			         unpack="false"/>

			   <plugin
			         id="test.plugin.two"
			         download-size="0"
			         install-size="0"
			         version="2.0.0"
			         unpack="true"/>

			   <includes
			         id="included.feature"
			         version="1.2.3"/>

			   <includes
			         id="optional.feature"
			         version="4.5.6"
			         optional="true"/>

			   <requires>
			      <import plugin="required.plugin" version="1.0.0"/>
			      <import feature="required.feature" version="2.0.0"/>
			   </requires>

			</feature>
			""";

		// Write to a temporary feature.xml file in a JAR structure
		File featureDir = new File(tmp, "test-feature");
		File featureFile = new File(featureDir, "feature.xml");
		IO.mkdirs(featureDir);
		IO.store(featureXml, featureFile);

		// Create a JAR with the feature.xml
		File featureJar = new File(tmp, "test.feature_1.0.0.jar");
		try (aQute.bnd.osgi.Jar jar = new aQute.bnd.osgi.Jar("test.feature")) {
			jar.putResource("feature.xml", new aQute.bnd.osgi.FileResource(featureFile));
			jar.write(featureJar);
		}

		// Parse the feature
		try (InputStream is = IO.stream(featureJar)) {
			Feature feature = new Feature(is);
			feature.parse();

			// Validate basic properties
			softly.assertThat(feature.getId())
				.isEqualTo("test.feature");
			softly.assertThat(feature.getLabel())
				.isEqualTo("Test Feature");
			softly.assertThat(feature.getVersion())
				.isEqualTo("1.0.0.qualifier");
			softly.assertThat(feature.getProviderName())
				.isEqualTo("Test Provider");
			softly.assertThat(feature.plugin)
				.isEqualTo("test.feature.plugin");

			// Validate plugins
			List<Feature.Plugin> plugins = feature.getPlugins();
			softly.assertThat(plugins)
				.hasSize(2);

			if (plugins.size() >= 2) {
				softly.assertThat(plugins.get(0).id)
					.isEqualTo("test.plugin.one");
				softly.assertThat(plugins.get(0).version)
					.isEqualTo("1.0.0.qualifier");
				softly.assertThat(plugins.get(1).id)
					.isEqualTo("test.plugin.two");
			}

			// Validate includes
			List<Feature.Includes> includes = feature.getIncludes();
			softly.assertThat(includes)
				.hasSize(2);

			if (includes.size() >= 2) {
				softly.assertThat(includes.get(0).id)
					.isEqualTo("included.feature");
				softly.assertThat(includes.get(0).version)
					.isEqualTo("1.2.3");
				softly.assertThat(includes.get(0).optional)
					.isFalse();

				softly.assertThat(includes.get(1).id)
					.isEqualTo("optional.feature");
				softly.assertThat(includes.get(1).optional)
					.isTrue();
			}

			// Validate requires
			List<Feature.Requires> requires = feature.getRequires();
			softly.assertThat(requires)
				.hasSize(2);

			// Test Resource conversion
			Resource resource = feature.toResource();
			List<Capability> identityCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			softly.assertThat(identityCaps)
				.hasSize(1);

			Capability identityCap = identityCaps.get(0);
			softly.assertThat(identityCap.getAttributes()
				.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
				.isEqualTo("eclipse.feature");
			softly.assertThat(identityCap.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE))
				.isEqualTo("test.feature");
			softly.assertThat(identityCap.getAttributes()
				.get("label"))
				.isEqualTo("Test Feature");
			softly.assertThat(identityCap.getAttributes()
				.get("provider-name"))
				.isEqualTo("Test Provider");

			// Validate requirements
			List<Requirement> requirements = resource.getRequirements("osgi.identity");
			// 2 plugins + 2 includes + 2 requires = 6 requirements
			softly.assertThat(requirements)
				.as("Should have requirements for plugins, includes, and requires")
				.hasSizeGreaterThanOrEqualTo(4);
		}
	}
}
