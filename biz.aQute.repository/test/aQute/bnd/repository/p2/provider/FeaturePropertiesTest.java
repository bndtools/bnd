package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.p2.provider.Feature;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test that Feature parser resolves property references from feature.properties
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeaturePropertiesTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	@Test
	public void testPropertyResolution() throws Exception {
		// Create a feature JAR with feature.xml and feature.properties
		String featureXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<feature\n" +
			"      id=\"test.feature\"\n" +
			"      label=\"%featureName\"\n" +
			"      version=\"1.0.0\"\n" +
			"      provider-name=\"%providerName\">\n" +
			"   <description>%description</description>\n" +
			"   <copyright>%copyright</copyright>\n" +
			"   <license url=\"%licenseURL\">%license</license>\n" +
			"</feature>";

		String featureProperties = "featureName=Test Feature Name\n" +
			"providerName=Test Provider\n" +
			"description=This is a test feature\n" +
			"copyright=Copyright 2025 Test\n" +
			"licenseURL=http://example.com/license\n" +
			"license=Test License Text";

		// Create JAR with feature.xml and feature.properties
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JarOutputStream jos = new JarOutputStream(baos)) {
			// Add feature.xml
			JarEntry xmlEntry = new JarEntry("feature.xml");
			jos.putNextEntry(xmlEntry);
			jos.write(featureXml.getBytes("UTF-8"));
			jos.closeEntry();

			// Add feature.properties
			JarEntry propsEntry = new JarEntry("feature.properties");
			jos.putNextEntry(propsEntry);
			jos.write(featureProperties.getBytes("UTF-8"));
			jos.closeEntry();
		}

		// Parse the feature
		try (InputStream in = new ByteArrayInputStream(baos.toByteArray())) {
			Feature feature = new Feature(in);
			feature.parse();

			// Verify properties were resolved
			softly.assertThat(feature.getId())
				.as("Feature ID should not be resolved")
				.isEqualTo("test.feature");

			softly.assertThat(feature.getLabel())
				.as("Label should be resolved from properties")
				.isEqualTo("Test Feature Name");

			softly.assertThat(feature.getProviderName())
				.as("Provider name should be resolved from properties")
				.isEqualTo("Test Provider");

			softly.assertThat(feature.getVersion())
				.as("Version should not be resolved")
				.isEqualTo("1.0.0");

			System.out.println("Feature parsed with resolved properties:");
			System.out.println("  ID: " + feature.getId());
			System.out.println("  Label: " + feature.getLabel());
			System.out.println("  Provider: " + feature.getProviderName());
			System.out.println("  Version: " + feature.getVersion());
		}
	}

	@Test
	public void testWithoutProperties() throws Exception {
		// Create a feature JAR without feature.properties
		String featureXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<feature\n" +
			"      id=\"test.feature\"\n" +
			"      label=\"Direct Label\"\n" +
			"      version=\"1.0.0\"\n" +
			"      provider-name=\"Direct Provider\">\n" +
			"</feature>";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JarOutputStream jos = new JarOutputStream(baos)) {
			JarEntry xmlEntry = new JarEntry("feature.xml");
			jos.putNextEntry(xmlEntry);
			jos.write(featureXml.getBytes("UTF-8"));
			jos.closeEntry();
		}

		try (InputStream in = new ByteArrayInputStream(baos.toByteArray())) {
			Feature feature = new Feature(in);
			feature.parse();

			softly.assertThat(feature.getLabel())
				.as("Label should be used as-is without properties")
				.isEqualTo("Direct Label");

			softly.assertThat(feature.getProviderName())
				.as("Provider should be used as-is without properties")
				.isEqualTo("Direct Provider");
		}
	}

	@Test
	public void testUnresolvedProperties() throws Exception {
		// Create a feature with property references but no feature.properties file
		String featureXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<feature\n" +
			"      id=\"test.feature\"\n" +
			"      label=\"%missingKey\"\n" +
			"      version=\"1.0.0\"\n" +
			"      provider-name=\"%anotherMissingKey\">\n" +
			"</feature>";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JarOutputStream jos = new JarOutputStream(baos)) {
			JarEntry xmlEntry = new JarEntry("feature.xml");
			jos.putNextEntry(xmlEntry);
			jos.write(featureXml.getBytes("UTF-8"));
			jos.closeEntry();
		}

		try (InputStream in = new ByteArrayInputStream(baos.toByteArray())) {
			Feature feature = new Feature(in);
			feature.parse();

			// Should keep the %key if not found in properties
			softly.assertThat(feature.getLabel())
				.as("Label should remain as %key if not resolved")
				.isEqualTo("%missingKey");

			softly.assertThat(feature.getProviderName())
				.as("Provider should remain as %key if not resolved")
				.isEqualTo("%anotherMissingKey");
		}
	}
}
