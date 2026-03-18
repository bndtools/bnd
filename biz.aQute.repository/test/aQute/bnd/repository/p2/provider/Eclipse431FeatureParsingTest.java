package aQute.bnd.repository.p2.provider;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.p2.api.Artifact;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;
import aQute.p2.provider.P2Impl;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test that P2 feature parsing correctly translates Eclipse match rules to OSGi
 * version ranges. Specifically tests the R-4.31-202402290520 repository to
 * verify that feature requirements with range [2.7.0,3.0.0) are not incorrectly
 * narrowed to [2.7.0,2.8.0).
 */
@ExtendWith(SoftAssertionsExtension.class)
public class Eclipse431FeatureParsingTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	// This specific Eclipse 4.31 release is referenced in the bug report for this issue.
	// The org.eclipse.e4.rcp feature in this release requires org.eclipse.emf.common
	// with range [2.7.0,3.0.0) (compatible match), which was incorrectly parsed as
	// [2.7.0,2.8.0).
	private static final String	ECLIPSE_431_REPO	= "https://download.eclipse.org/eclipse/updates/4.31/R-4.31-202402290520/";

	/**
	 * Test that feature requirements from the Eclipse 4.31 repository use
	 * correct version ranges. The org.eclipse.e4.rcp feature requires
	 * org.eclipse.emf.common with range [2.7.0,3.0.0) (compatible match) which
	 * must not be narrowed to [2.7.0,2.8.0).
	 */
	@Test
	public void testEclipse431FeatureVersionRanges() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));

			Unpack200 processor = new Unpack200(proc);
			P2Impl p2 = new P2Impl(processor, client, new URI(ECLIPSE_431_REPO),
				Processor.getPromiseFactory());

			List<Artifact> allArtifacts = p2.getAllArtifacts();
			softly.assertThat(allArtifacts)
				.as("Eclipse 4.31 P2 repo should contain artifacts")
				.isNotEmpty();

			// Find the org.eclipse.e4.rcp feature
			Artifact e4rcpArtifact = allArtifacts.stream()
				.filter(a -> a.classifier == aQute.p2.api.Classifier.FEATURE
					&& "org.eclipse.e4.rcp".equals(a.id))
				.findFirst()
				.orElse(null);

			softly.assertThat(e4rcpArtifact)
				.as("org.eclipse.e4.rcp feature must be present in Eclipse 4.31 repository")
				.isNotNull();

			if (e4rcpArtifact == null) {
				return;
			}

			// Download and parse the feature JAR
			File featureJar = client.build()
				.useCache()
				.go(e4rcpArtifact.uri);
			softly.assertThat(featureJar)
				.as("Feature JAR should be downloadable")
				.isNotNull()
				.exists();

			if (featureJar == null) {
				return;
			}

			Feature feature;
			try (InputStream is = IO.stream(featureJar)) {
				feature = new Feature(is);
				feature.parse();
			}

			softly.assertThat(feature.getId())
				.as("Feature ID")
				.isEqualTo("org.eclipse.e4.rcp");

			// Convert to OSGi Resource
			Resource resource = feature.toResource();
			List<Requirement> requirements = resource.getRequirements("osgi.identity");

			// Find requirements for org.eclipse.emf.common and org.eclipse.emf.ecore
			// In feature.xml these use match="compatible" with version="2.7.0",
			// which must produce range [2.7.0,3.0.0) - NOT [2.7.0,2.8.0)
			String emfCommonFilter = requirements.stream()
				.map(req -> req.getDirectives()
					.get("filter"))
				.filter(f -> f != null && f.contains("org.eclipse.emf.common"))
				.findFirst()
				.orElse(null);

			String emfEcoreFilter = requirements.stream()
				.map(req -> req.getDirectives()
					.get("filter"))
				.filter(f -> f != null && f.contains("org.eclipse.emf.ecore"))
				.findFirst()
				.orElse(null);

			System.out.println("org.eclipse.emf.common filter: " + emfCommonFilter);
			System.out.println("org.eclipse.emf.ecore filter: " + emfEcoreFilter);

			softly.assertThat(emfCommonFilter)
				.as("org.eclipse.emf.common requirement must have range [2.7.0,3.0.0) - not [2.7.0,2.8.0)")
				.isNotNull()
				.contains("(version>=2.7.0)")
				.contains("(!(version>=3.0.0))")
				.doesNotContain("(!(version>=2.8.0))");

			softly.assertThat(emfEcoreFilter)
				.as("org.eclipse.emf.ecore requirement must have range [2.7.0,3.0.0) - not [2.7.0,2.8.0)")
				.isNotNull()
				.contains("(version>=2.7.0)")
				.contains("(!(version>=3.0.0))")
				.doesNotContain("(!(version>=2.8.0))");
		}
	}

	/**
	 * Unit test for buildVersionFilter match rules using a synthetic feature.xml.
	 * Verifies Eclipse match semantics:
	 * <ul>
	 * <li>compatible (same major): [M.m.u, (M+1).0.0)</li>
	 * <li>equivalent (same major.minor): [M.m.u, M.(m+1).0)</li>
	 * <li>greaterOrEqual: [M.m.u, inf)</li>
	 * <li>perfect: [M.m.u.q, M.m.u.q] (exact)</li>
	 * </ul>
	 */
	@Test
	public void testFeatureMatchRuleVersionRanges() throws Exception {
		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature id="test.feature" version="1.0.0">
			   <requires>
			      <import feature="emf.compatible" version="2.7.0" match="compatible"/>
			      <import feature="emf.equivalent" version="2.7.0" match="equivalent"/>
			      <import plugin="plugin.compatible" version="1.2.3" match="compatible"/>
			      <import plugin="plugin.equivalent" version="1.2.3" match="equivalent"/>
			   </requires>
			</feature>
			""";

		File featureJar = new File(tmp, "test.feature.jar");
		try (Jar jar = new Jar("test.feature")) {
			File featureFile = new File(tmp, "feature.xml");
			IO.store(featureXml, featureFile);
			jar.putResource("feature.xml", new aQute.bnd.osgi.FileResource(featureFile));
			jar.write(featureJar);
		}

		Feature feature;
		try (InputStream is = IO.stream(featureJar)) {
			feature = new Feature(is);
			feature.parse();
		}

		Resource resource = feature.toResource();
		List<Requirement> requirements = resource.getRequirements("osgi.identity");

		// "compatible" with version 2.7.0: same major (2), so upper = 3.0.0
		String emfCompatFilter = requirements.stream()
			.map(req -> req.getDirectives()
				.get("filter"))
			.filter(f -> f != null && f.contains("emf.compatible"))
			.findFirst()
			.orElse(null);

		softly.assertThat(emfCompatFilter)
			.as("compatible match on feature emf.compatible version 2.7.0 must give range [2.7.0, 3.0.0)")
			.isNotNull()
			.contains("(version>=2.7.0)")
			.contains("(!(version>=3.0.0))")
			.doesNotContain("(!(version>=2.8.0))");

		// "equivalent" with version 2.7.0: same major.minor (2.7), so upper = 2.8.0
		String emfEquivFilter = requirements.stream()
			.map(req -> req.getDirectives()
				.get("filter"))
			.filter(f -> f != null && f.contains("emf.equivalent"))
			.findFirst()
			.orElse(null);

		softly.assertThat(emfEquivFilter)
			.as("equivalent match on feature emf.equivalent version 2.7.0 must give range [2.7.0, 2.8.0)")
			.isNotNull()
			.contains("(version>=2.7.0)")
			.contains("(!(version>=2.8.0))")
			.doesNotContain("(!(version>=2.7.1))");

		// "compatible" with version 1.2.3: upper = 2.0.0
		String pluginCompatFilter = requirements.stream()
			.map(req -> req.getDirectives()
				.get("filter"))
			.filter(f -> f != null && f.contains("plugin.compatible"))
			.findFirst()
			.orElse(null);

		softly.assertThat(pluginCompatFilter)
			.as("compatible match on plugin.compatible version 1.2.3 must give range [1.2.3, 2.0.0)")
			.isNotNull()
			.contains("(version>=1.2.3)")
			.contains("(!(version>=2.0.0))")
			.doesNotContain("(!(version>=1.3.0))");

		// "equivalent" with version 1.2.3: upper = 1.3.0
		String pluginEquivFilter = requirements.stream()
			.map(req -> req.getDirectives()
				.get("filter"))
			.filter(f -> f != null && f.contains("plugin.equivalent"))
			.findFirst()
			.orElse(null);

		softly.assertThat(pluginEquivFilter)
			.as("equivalent match on plugin.equivalent version 1.2.3 must give range [1.2.3, 1.3.0)")
			.isNotNull()
			.contains("(version>=1.2.3)")
			.contains("(!(version>=1.3.0))")
			.doesNotContain("(!(version>=1.2.4))");
	}
}
