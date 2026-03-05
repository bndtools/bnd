package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Comprehensive test to verify parsing of Eclipse 4.38 P2 repository features.
 * This test validates that:
 * 1. Features are correctly identified in the repository
 * 2. Feature JARs can be downloaded from the features/ directory
 * 3. feature.xml files are correctly parsed from feature JARs
 * 4. Features are indexed with proper capabilities and requirements
 * 5. Plugin references and included features are correctly extracted
 */
//@Disabled("Deactivated for server builds - only for manual local testing due to long runtime")
@ExtendWith(SoftAssertionsExtension.class)
public class Eclipse438FeatureTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	// Eclipse 4.38 P2 repository URL
	private static final String	ECLIPSE_438_REPO	= "https://download.eclipse.org/eclipse/updates/4.38/R-4.38-202512010920/";

	/**
	 * Test that the Eclipse 4.38 P2 repository is properly indexed with features
	 */
	@Test
	public void testEclipse438RepositoryIndexing() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));
			
			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "eclipse-438-index");
			
			System.out.println("Creating P2 indexer for Eclipse 4.38 repository...");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_REPO),
				"Eclipse 4.38");

			// Verify index was created
			softly.assertThat(indexLocation)
				.as("Index location should exist")
				.exists()
				.isDirectory();

			File indexFile = new File(indexLocation, "index.xml.gz");
			softly.assertThat(indexFile)
				.as("Index file should be created")
				.exists();

			System.out.println("Index location: " + indexLocation.getAbsolutePath());
			System.out.println("Index file: " + indexFile.getAbsolutePath() + " (" + indexFile.length() + " bytes)");
		}
	}

	/**
	 * Test feature extraction and parsing from Eclipse 4.38 repository
	 */
	@Test
	public void testEclipse438FeatureExtraction() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));
			
			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "eclipse-438-features");
			
			System.out.println("\n=== Testing Eclipse 4.38 Feature Extraction ===");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_REPO),
				"Eclipse 4.38");

			// Get all resources from the repository
			org.osgi.service.repository.Repository repository = indexer.getBridge()
				.getRepository();
			RequirementBuilder rb = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();

			java.util.Collection<Capability> allCaps = repository
				.findProviders(java.util.Collections.singleton(req))
				.getOrDefault(req, java.util.Collections.emptyList());

			System.out.println("Total capabilities in repository: " + allCaps.size());

			// Get unique resources
			java.util.Set<Resource> allResources = ResourceUtils.getResources(allCaps);
			System.out.println("Total resources in repository: " + allResources.size());

			// Count and categorize resources by type
			Map<String, Long> typeCount = allResources.stream()
				.flatMap(r -> r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
					.stream())
				.map(cap -> (String) cap.getAttributes()
					.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
				.filter(type -> type != null)
				.collect(Collectors.groupingBy(type -> type, Collectors.counting()));

			System.out.println("\nResource types in repository:");
			typeCount.forEach((type, count) -> System.out.println("  " + type + ": " + count));

			// Verify features exist
			long featureCount = typeCount.getOrDefault("org.eclipse.update.feature", 0L);
			softly.assertThat(featureCount)
				.as("Eclipse 4.38 repository should contain features")
				.isGreaterThan(0);

			// Extract features using the getFeatures() method
			System.out.println("\n=== Extracting features ===");
			List<Feature> features = indexer.getFeatures();

			System.out.println("Total features extracted: " + features.size());
			softly.assertThat(features)
				.as("Should be able to extract features from repository")
				.isNotEmpty();

			// Analyze and validate features
			int validatedCount = 0;
			int withPlugins = 0;
			int withIncludes = 0;
			int withRequires = 0;

			for (Feature feature : features) {
				// Basic validation
				softly.assertThat(feature.getId())
					.as("Feature should have an ID")
					.isNotNull()
					.isNotEmpty();

				softly.assertThat(feature.getVersion())
					.as("Feature " + feature.getId() + " should have a version")
					.isNotNull()
					.isNotEmpty();

				// Count features with different elements
				if (!feature.getPlugins()
					.isEmpty()) {
					withPlugins++;
				}
				if (!feature.getIncludes()
					.isEmpty()) {
					withIncludes++;
				}
				if (!feature.getRequires()
					.isEmpty()) {
					withRequires++;
				}

				// Print details for first few features
				if (validatedCount < 5) {
					System.out.println("\nFeature #" + (validatedCount + 1) + ": " + feature.getId());
					System.out.println("  Version: " + feature.getVersion());
					System.out.println("  Label: " + feature.getLabel());
					System.out.println("  Provider: " + feature.getProviderName());
					System.out.println("  Plugins: " + feature.getPlugins()
						.size());
					System.out.println("  Includes: " + feature.getIncludes()
						.size());
					System.out.println("  Requires: " + feature.getRequires()
						.size());

					// Print some plugin details
					if (!feature.getPlugins()
						.isEmpty()) {
						System.out.println("  Sample plugins:");
						feature.getPlugins()
							.stream()
							.limit(3)
							.forEach(
								p -> System.out.println("    - " + p.id + " version=" + p.version));
					}

					// Print some included features
					if (!feature.getIncludes()
						.isEmpty()) {
						System.out.println("  Included features:");
						feature.getIncludes()
							.stream()
							.limit(3)
							.forEach(inc -> System.out
								.println("    - " + inc.id + " version=" + inc.version
									+ (inc.optional ? " (optional)" : "")));
					}
				}

				validatedCount++;
			}

			System.out.println("\n=== Feature Statistics ===");
			System.out.println("Total features validated: " + validatedCount);
			System.out.println("Features with plugins: " + withPlugins);
			System.out.println("Features with includes: " + withIncludes);
			System.out.println("Features with requires: " + withRequires);

			// Validate that many features have content
			softly.assertThat(withPlugins)
				.as("Many features should contain plugin references")
				.isGreaterThan(0);
		}
	}

	/**
	 * Test retrieval of specific well-known Eclipse platform features
	 */
	@Test
	public void testEclipse438SpecificFeatures() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));
			
			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "eclipse-438-specific");
			
			System.out.println("\n=== Testing Specific Eclipse 4.38 Features ===");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_REPO),
				"Eclipse 4.38");

			// Get all features to find available ones
			List<Feature> allFeatures = indexer.getFeatures();
			softly.assertThat(allFeatures)
				.as("Repository should contain features")
				.isNotEmpty();

			// Print first few features for reference
			System.out.println("\nAvailable features (first 10):");
			allFeatures.stream()
				.limit(10)
				.forEach(f -> System.out.println("  - " + f.getId() + " version " + f.getVersion()));

			// Try to get a specific feature
			if (!allFeatures.isEmpty()) {
				Feature firstFeature = allFeatures.get(0);
				System.out.println("\nTesting getFeature() with: " + firstFeature.getId() + " v" + firstFeature.getVersion());
				
				Feature retrieved = indexer.getFeature(firstFeature.getId(), firstFeature.getVersion());
				
				softly.assertThat(retrieved)
					.as("Should be able to retrieve feature by ID and version")
					.isNotNull();

				if (retrieved != null) {
					softly.assertThat(retrieved.getId())
						.as("Retrieved feature should have correct ID")
						.isEqualTo(firstFeature.getId());

					softly.assertThat(retrieved.getVersion())
						.as("Retrieved feature should have correct version")
						.isEqualTo(firstFeature.getVersion());

					System.out.println("Successfully retrieved feature: " + retrieved.getId());
					System.out.println("  Plugins: " + retrieved.getPlugins()
						.size());
					System.out.println("  Includes: " + retrieved.getIncludes()
						.size());
				}
			}

			// Look for platform feature (common in Eclipse releases)
			Feature platformFeature = allFeatures.stream()
				.filter(f -> f.getId() != null && f.getId()
					.contains("org.eclipse.platform"))
				.findFirst()
				.orElse(null);

			if (platformFeature != null) {
				System.out.println("\nFound platform feature: " + platformFeature.getId());
				System.out.println("  Version: " + platformFeature.getVersion());
				System.out.println("  Label: " + platformFeature.getLabel());
				System.out.println("  Plugins: " + platformFeature.getPlugins()
					.size());
				System.out.println("  Includes: " + platformFeature.getIncludes()
					.size());

				softly.assertThat(platformFeature.getId())
					.as("Platform feature should have ID")
					.isNotNull();

				softly.assertThat(platformFeature.getPlugins())
					.as("Platform feature should have plugins")
					.isNotEmpty();
			}
		}
	}

	/**
	 * Test that feature parsing handles feature.properties correctly
	 */
	@Test
	public void testEclipse438FeatureProperties() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));
			
			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "eclipse-438-properties");
			
			System.out.println("\n=== Testing Feature Property Resolution ===");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_REPO),
				"Eclipse 4.38");

			List<Feature> features = indexer.getFeatures();
			
			// Check that properties are resolved (not showing %key references)
			int resolvedLabels = 0;
			int resolvedProviders = 0;

			for (Feature feature : features) {
				if (feature.getLabel() != null && !feature.getLabel()
					.startsWith("%")) {
					resolvedLabels++;
					if (resolvedLabels <= 3) {
						System.out.println("Feature " + feature.getId() + " has resolved label: " + feature.getLabel());
					}
				}

				if (feature.getProviderName() != null && !feature.getProviderName()
					.startsWith("%")) {
					resolvedProviders++;
				}
			}

			System.out.println("\nFeatures with resolved labels: " + resolvedLabels + "/" + features.size());
			System.out.println("Features with resolved provider names: " + resolvedProviders + "/" + features.size());

			// Most features should have resolved properties
			softly.assertThat(resolvedLabels)
				.as("Many features should have resolved labels")
				.isGreaterThan(features.size() / 2);
		}
	}

	/**
	 * Test that resource capabilities are correctly created for features
	 */
	@Test
	public void testEclipse438FeatureCapabilities() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));
			
			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "eclipse-438-capabilities");
			
			System.out.println("\n=== Testing Feature Capabilities in Index ===");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_REPO),
				"Eclipse 4.38");

			// Get repository and check feature resources
			org.osgi.service.repository.Repository repository = indexer.getBridge()
				.getRepository();
			RequirementBuilder rb = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();

			java.util.Collection<Capability> allCaps = repository
				.findProviders(java.util.Collections.singleton(req))
				.getOrDefault(req, java.util.Collections.emptyList());

			// Find feature capabilities
			List<Capability> featureCaps = allCaps.stream()
				.filter(cap -> "org.eclipse.update.feature"
					.equals(cap.getAttributes()
						.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)))
				.collect(Collectors.toList());

			System.out.println("Total feature capabilities: " + featureCaps.size());

			softly.assertThat(featureCaps)
				.as("Index should contain feature capabilities")
				.isNotEmpty();

			// Inspect first few feature capabilities
			int count = 0;
			for (Capability cap : featureCaps) {
				if (count++ >= 3)
					break;

				Map<String, Object> attrs = cap.getAttributes();
				System.out.println("\nFeature capability #" + count + ":");
				System.out.println("  Identity: " + attrs.get(IdentityNamespace.IDENTITY_NAMESPACE));
				System.out.println("  Version: " + attrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
				System.out.println("  Type: " + attrs.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));

				// Validate required attributes
				softly.assertThat(attrs.get(IdentityNamespace.IDENTITY_NAMESPACE))
					.as("Feature capability should have identity")
					.isNotNull();

				softly.assertThat(attrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
					.as("Feature capability should have version")
					.isNotNull();

				softly.assertThat(attrs.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
					.as("Feature capability should have type")
					.isEqualTo("org.eclipse.update.feature");
			}
		}
	}
}
