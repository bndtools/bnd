package aQute.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Jar;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test to verify that Eclipse feature requirement version and match attributes
 * are correctly converted to OSGi version filter expressions.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeatureVersionFilterTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	/**
	 * Test version filter generation for different match rules
	 */
	@Test
	public void testVersionMatchRules() throws Exception {
		// Create a feature with various version requirements
		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature
			      id="test.feature"
			      label="Test Feature"
			      version="1.0.0">
			   
			   <requires>
			      <!-- perfect: exact version match -->
			      <import plugin="plugin.perfect" version="1.2.3.qualifier" match="perfect"/>
			      
			      <!-- equivalent: same major.minor.micro, any qualifier -->
			      <import plugin="plugin.equivalent" version="1.2.3" match="equivalent"/>
			      
			      <!-- compatible: same major.minor, micro >= specified -->
			      <import plugin="plugin.compatible" version="1.2.0" match="compatible"/>
			      
			      <!-- greaterOrEqual: version >= specified -->
			      <import plugin="plugin.greaterOrEqual" version="1.0.0" match="greaterOrEqual"/>
			      
			      <!-- No match specified (defaults to greaterOrEqual) -->
			      <import plugin="plugin.default" version="2.0.0"/>
			      
			      <!-- No version specified -->
			      <import plugin="plugin.noversion"/>
			      
			      <!-- Feature requirement with version -->
			      <import feature="feature.compatible" version="1.5.0" match="compatible"/>
			      
			      <!-- Feature requirement without version -->
			      <import feature="feature.noversion"/>
			   </requires>
			</feature>
			""";

		// Create feature JAR
		File featureJar = new File(tmp, "test.feature.jar");
		try (Jar jar = new Jar("test.feature")) {
			File featureFile = new File(tmp, "feature.xml");
			IO.store(featureXml, featureFile);
			jar.putResource("feature.xml", new aQute.bnd.osgi.FileResource(featureFile));
			jar.write(featureJar);
		}

		// Parse the feature
		Feature feature;
		try (InputStream is = IO.stream(featureJar)) {
			feature = new Feature(is);
			feature.parse();
		}

		// Convert to OSGi Resource
		Resource resource = feature.toResource();

		// Get requirements
		List<Requirement> requirements = resource.getRequirements("osgi.identity");

		System.out.println("\n=== Version Filter Test Results ===\n");
		System.out.println("Total requirements: " + requirements.size());

		// Verify each requirement
		for (Requirement req : requirements) {
			String filter = req.getDirectives()
				.get("filter");
			System.out.println("Filter: " + filter);
		}

		// Test perfect match
		String perfectFilter = findFilterContaining(requirements, "plugin.perfect");
		System.out.println("\nPerfect match:");
		System.out.println("  Filter: " + perfectFilter);
		softly.assertThat(perfectFilter)
			.as("Perfect match should have exact version")
			.contains("osgi.identity=plugin.perfect")
			.contains("(version=1.2.3.qualifier)");

		// Test equivalent match
		String equivalentFilter = findFilterContaining(requirements, "plugin.equivalent");
		System.out.println("\nEquivalent match:");
		System.out.println("  Filter: " + equivalentFilter);
		softly.assertThat(equivalentFilter)
			.as("Equivalent match should create range [1.2.3, 1.2.4)")
			.contains("osgi.identity=plugin.equivalent")
			.contains("(version>=1.2.3)")
			.contains("(!(version>=1.2.4))");

		// Test compatible match
		String compatibleFilter = findFilterContaining(requirements, "plugin.compatible");
		System.out.println("\nCompatible match:");
		System.out.println("  Filter: " + compatibleFilter);
		softly.assertThat(compatibleFilter)
			.as("Compatible match should create range [1.2.0, 1.3.0)")
			.contains("osgi.identity=plugin.compatible")
			.contains("(version>=1.2.0)")
			.contains("(!(version>=1.3.0))");

		// Test greaterOrEqual match
		String greaterOrEqualFilter = findFilterContaining(requirements, "plugin.greaterOrEqual");
		System.out.println("\nGreaterOrEqual match:");
		System.out.println("  Filter: " + greaterOrEqualFilter);
		softly.assertThat(greaterOrEqualFilter)
			.as("GreaterOrEqual match should have unbounded range")
			.contains("osgi.identity=plugin.greaterOrEqual")
			.contains("(version>=1.0.0)");

		// Test default (no match specified)
		String defaultFilter = findFilterContaining(requirements, "plugin.default");
		System.out.println("\nDefault (no match):");
		System.out.println("  Filter: " + defaultFilter);
		softly.assertThat(defaultFilter)
			.as("Default should be greaterOrEqual")
			.contains("osgi.identity=plugin.default")
			.contains("(version>=2.0.0)");

		// Test no version
		String noVersionFilter = findFilterContaining(requirements, "plugin.noversion");
		System.out.println("\nNo version:");
		System.out.println("  Filter: " + noVersionFilter);
		softly.assertThat(noVersionFilter)
			.as("No version should have simple identity filter")
			.isEqualTo("(osgi.identity=plugin.noversion)");

		// Test feature with version
		String featureCompatibleFilter = findFilterContaining(requirements, "feature.compatible");
		System.out.println("\nFeature with compatible match:");
		System.out.println("  Filter: " + featureCompatibleFilter);
		softly.assertThat(featureCompatibleFilter)
			.as("Feature requirement should include type and version range")
			.contains("osgi.identity=feature.compatible")
			.contains("type=org.eclipse.update.feature")
			.contains("(version>=1.5.0)")
			.contains("(!(version>=1.6.0))");

		// Test feature without version
		String featureNoVersionFilter = findFilterContaining(requirements, "feature.noversion");
		System.out.println("\nFeature without version:");
		System.out.println("  Filter: " + featureNoVersionFilter);
		softly.assertThat(featureNoVersionFilter)
			.as("Feature without version should have type but no version constraint")
			.contains("osgi.identity=feature.noversion")
			.contains("type=org.eclipse.update.feature");
	}

	/**
	 * Test edge cases for version filter generation
	 */
	@Test
	public void testVersionFilterEdgeCases() throws Exception {
		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature
			      id="test.edge.feature"
			      label="Test Edge Cases"
			      version="1.0.0">
			   
			   <requires>
			      <!-- Version 0.0.0 (should be treated as no version) -->
			      <import plugin="plugin.zero" version="0.0.0" match="greaterOrEqual"/>
			      
			      <!-- Complex qualifier -->
			      <import plugin="plugin.qualifier" version="1.2.3.v20251201-1234" match="perfect"/>
			      
			      <!-- Large version numbers -->
			      <import plugin="plugin.large" version="99.99.99" match="compatible"/>
			      
			      <!-- Empty match (should default to greaterOrEqual) -->
			      <import plugin="plugin.emptymatch" version="1.0.0" match=""/>
			   </requires>
			</feature>
			""";

		File featureJar = new File(tmp, "test.edge.jar");
		try (Jar jar = new Jar("test.edge")) {
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

		System.out.println("\n=== Edge Case Test Results ===\n");

		// Test 0.0.0 version (should be treated as no version)
		String zeroVersionFilter = findFilterContaining(requirements, "plugin.zero");
		System.out.println("Version 0.0.0:");
		System.out.println("  Filter: " + zeroVersionFilter);
		softly.assertThat(zeroVersionFilter)
			.as("Version 0.0.0 should be treated as no version constraint")
			.isEqualTo("(osgi.identity=plugin.zero)");

		// Test complex qualifier
		String qualifierFilter = findFilterContaining(requirements, "plugin.qualifier");
		System.out.println("\nComplex qualifier:");
		System.out.println("  Filter: " + qualifierFilter);
		softly.assertThat(qualifierFilter)
			.as("Perfect match should preserve qualifier")
			.contains("(version=1.2.3.v20251201-1234)");

		// Test large version numbers
		String largeFilter = findFilterContaining(requirements, "plugin.large");
		System.out.println("\nLarge version:");
		System.out.println("  Filter: " + largeFilter);
		softly.assertThat(largeFilter)
			.as("Large version should create proper range")
			.contains("(version>=99.99.99)")
			.contains("(!(version>=99.100.0))");

		// Test empty match
		String emptyMatchFilter = findFilterContaining(requirements, "plugin.emptymatch");
		System.out.println("\nEmpty match:");
		System.out.println("  Filter: " + emptyMatchFilter);
		softly.assertThat(emptyMatchFilter)
			.as("Empty match should default to greaterOrEqual")
			.contains("(version>=1.0.0)");
	}

	/**
	 * Find a requirement filter containing the specified identity
	 */
	private String findFilterContaining(List<Requirement> requirements, String identity) {
		return requirements.stream()
			.map(req -> req.getDirectives()
				.get("filter"))
			.filter(filter -> filter != null && filter.contains(identity))
			.findFirst()
			.orElse(null);
	}
}
