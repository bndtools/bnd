package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test Eclipse 4.38 Platform repository to ensure features are
 * properly extracted and available, even when full JAR extraction fails.
 * This tests the minimal feature creation fallback mechanism.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class Eclipse438PlatformFeatureTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	// Use the actual Eclipse 4.38 Platform repository
	private static final String	ECLIPSE_438_PLATFORM_REPO	= "https://download.eclipse.org/eclipse/updates/4.38/";

	/**
	 * Test that org.eclipse.sdk feature (which we know exists) is present in the Eclipse 4.38
	 * Platform repository and can be retrieved via getFeatures().
	 * This ensures the minimal feature creation works when full extraction fails.
	 */
	@Test
	public void testEclipsePlatformFeatureIsAvailable() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "eclipse-438-platform");

			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_PLATFORM_REPO),
				"Eclipse 4.38 Platform Test");

			// Get features using the FeatureProvider interface
			List<Feature> features = indexer.getFeatures();

			softly.assertThat(features)
				.as("Features list should not be null")
				.isNotNull();

			softly.assertThat(features)
				.as("Features list should not be empty")
				.isNotEmpty();

			// Print all feature IDs to help debug
			System.out.println("\nAll features in repository:");
			features.forEach(f -> System.out.println("  - " + f.getId() + " : " + f.getVersion()));

			// Find org.eclipse.sdk feature (which we know exists from the list above)
			Feature sdkFeature = features.stream()
				.filter(f -> "org.eclipse.sdk".equals(f.getId()))
				.findFirst()
				.orElse(null);

			softly.assertThat(sdkFeature)
				.as("org.eclipse.sdk feature must be present in features list")
				.isNotNull();

			if (sdkFeature != null) {
				softly.assertThat(sdkFeature.getId())
					.as("Feature ID should be org.eclipse.sdk")
					.isEqualTo("org.eclipse.sdk");

				softly.assertThat(sdkFeature.getVersion())
					.as("Feature version should not be null")
					.isNotNull();

				// Check that version follows Eclipse pattern (should contain 4.38)
				softly.assertThat(sdkFeature.getVersion())
					.as("Feature version should contain 4.38")
					.contains("4.38");

				System.out.println("Found org.eclipse.sdk feature:");
				System.out.println("  ID: " + sdkFeature.getId());
				System.out.println("  Version: " + sdkFeature.getVersion());
				System.out.println("  Label: " + sdkFeature.getLabel());
				System.out.println("  Provider: " + sdkFeature.getProviderName());
			}

			// Also check for org.eclipse.platform feature (which exists as BOTH bundle and feature)
			Feature platformFeature = features.stream()
				.filter(f -> "org.eclipse.platform".equals(f.getId()))
				.findFirst()
				.orElse(null);

			softly.assertThat(platformFeature)
				.as("org.eclipse.platform feature must be present in features list (repo has both bundle and feature with this ID)")
				.isNotNull();

			if (platformFeature != null) {
				System.out.println("\nFound org.eclipse.platform feature:");
				System.out.println("  ID: " + platformFeature.getId());
				System.out.println("  Version: " + platformFeature.getVersion());
				System.out.println("  Label: " + platformFeature.getLabel());
				System.out.println("  Provider: " + platformFeature.getProviderName());
			}

			// Count feature vs bundle resources
			var bridge = indexer.getBridge();
			var repository = bridge.getRepository();

			aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
				IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();

			var providers = repository.findProviders(java.util.Collections.singleton(req));
			java.util.Collection<Capability> allCaps = providers.get(req);
			java.util.Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);

			// Look for org.eclipse.platform in raw resources
			List<Resource> platformResources = allResources.stream()
				.filter(r -> {
					List<Capability> identityCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					if (identityCaps.isEmpty())
						return false;
					Object id = identityCaps.get(0)
						.getAttributes()
						.get(IdentityNamespace.IDENTITY_NAMESPACE);
					return "org.eclipse.platform".equals(id);
				})
				.toList();

			System.out.println("\norg.eclipse.platform resources found: " + platformResources.size());
			platformResources.forEach(r -> {
				r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
					.forEach(cap -> {
						Object type = cap.getAttributes()
							.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
						Object label = cap.getAttributes()
							.get("label");
						Object providerName = cap.getAttributes()
							.get("provider-name");
						System.out.println("  Type: " + type);
						if (label != null) {
							System.out.println("    Label: " + label);
						}
						if (providerName != null) {
							System.out.println("    Provider: " + providerName);
						}
					});
			});

			Map<String, Long> resourcesByType = allResources.stream()
				.flatMap(r -> r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
					.stream())
				.map(cap -> (String) cap.getAttributes()
					.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
				.filter(type -> type != null)
				.collect(Collectors.groupingBy(type -> type, Collectors.counting()));

			System.out.println("\nResource type distribution:");
			resourcesByType.forEach((type, count) -> System.out.println("  " + type + ": " + count));

			softly.assertThat(resourcesByType.get("org.eclipse.update.feature"))
				.as("Index should contain feature resources")
				.isGreaterThan(0L);
		}
	}

	/**
	 * Test that feature resources in the index have proper identity capabilities
	 * with the correct attributes (type, id, version, label, provider-name)
	 */
	@Test
	public void testFeatureResourcesHaveProperIdentityCapabilities() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "eclipse-438-platform-2");

			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_PLATFORM_REPO),
				"Eclipse 4.38 Platform Test 2");

			var bridge = indexer.getBridge();
			var repository = bridge.getRepository();

			// Find all feature resources
			aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
				IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();

			var providers = repository.findProviders(java.util.Collections.singleton(req));
			java.util.Collection<Capability> allCaps = providers.get(req);
			java.util.Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);

			List<Resource> featureResources = allResources.stream()
				.filter(r -> {
					List<Capability> identityCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					if (identityCaps.isEmpty())
						return false;
					Object type = identityCaps.get(0)
						.getAttributes()
						.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
					return "org.eclipse.update.feature".equals(type);
				})
				.toList();

			softly.assertThat(featureResources)
				.as("Should find feature resources in index")
				.isNotEmpty();

			// Find org.eclipse.sdk resource specifically (we know it exists)
			Resource sdkResource = featureResources.stream()
				.filter(r -> {
					List<Capability> identityCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					if (identityCaps.isEmpty())
						return false;
					Object id = identityCaps.get(0)
						.getAttributes()
						.get(IdentityNamespace.IDENTITY_NAMESPACE);
					return "org.eclipse.sdk".equals(id);
				})
				.findFirst()
				.orElse(null);

			softly.assertThat(sdkResource)
				.as("org.eclipse.sdk resource should be in index")
				.isNotNull();

			if (sdkResource != null) {
				List<Capability> identityCaps = sdkResource
					.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
				softly.assertThat(identityCaps)
					.as("SDK resource should have identity capability")
					.hasSize(1);

				Capability identity = identityCaps.get(0);
				Map<String, Object> attrs = identity.getAttributes();

				softly.assertThat(attrs.get(IdentityNamespace.IDENTITY_NAMESPACE))
					.as("Identity capability should have correct osgi.identity")
					.isEqualTo("org.eclipse.sdk");

				softly.assertThat(attrs.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
					.as("Identity capability should have type org.eclipse.update.feature")
					.isEqualTo("org.eclipse.update.feature");

				softly.assertThat(attrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
					.as("Identity capability should have version")
					.isNotNull();

				softly.assertThat(attrs.get("label"))
					.as("Feature should have label attribute")
					.isNotNull();

				softly.assertThat(attrs.get("provider-name"))
					.as("Feature should have provider-name attribute")
					.isNotNull();

				System.out.println("\norg.eclipse.sdk identity attributes:");
				attrs.forEach((key, value) -> System.out.println("  " + key + " = " + value));
			}
		}
	}

	/**
	 * Test that both features with extractable JARs and features that fail
	 * extraction are included in the features list. This ensures the minimal
	 * feature creation fallback works properly.
	 */
	@Test
	public void testMinimalFeatureCreationFallback() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "eclipse-438-platform-3");

			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECLIPSE_438_PLATFORM_REPO),
				"Eclipse 4.38 Platform Test 3");

			// Get all features
			List<Feature> features = indexer.getFeatures();

			softly.assertThat(features)
				.as("Features should be available even when some JARs can't be extracted")
				.isNotEmpty();

			// Get all feature resources from index - must use getResources() directly
			// to bypass BridgeRepository deduplication which drops resources with same BSN+version
			var bridge = indexer.getBridge();
			aQute.bnd.osgi.repository.ResourcesRepository resourcesRepo = 
				(aQute.bnd.osgi.repository.ResourcesRepository) bridge.getRepository();
			List<Resource> allResources = resourcesRepo.getResources();

			long featureResourceCount = allResources.stream()
				.filter(r -> {
					List<Capability> identityCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					if (identityCaps.isEmpty())
						return false;
					Object type = identityCaps.get(0)
						.getAttributes()
						.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
					return "org.eclipse.update.feature".equals(type);
				})
				.count();

			System.out.println("\nFeature statistics:");
			System.out.println("  Features in index (resources): " + featureResourceCount);
			System.out.println("  Features returned by getFeatures(): " + features.size());

			// The number of Feature objects should match the number of feature resources
			// This ensures we're creating minimal features for resources that can't be fully extracted
			softly.assertThat((long) features.size())
				.as("All feature resources should have corresponding Feature objects")
				.isEqualTo(featureResourceCount);
		}
	}
}
