package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test extractFeatureFromResource functionality in P2Indexer
 */
@ExtendWith(SoftAssertionsExtension.class)
public class P2IndexerExtractFeatureTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	private static final String	ECF_P2_REPO	= "https://download.eclipse.org/rt/ecf/3.16.5/site.p2/3.16.5.v20250914-0333/";

	/**
	 * Test that extractFeatureFromResource correctly extracts a Feature from an
	 * indexed resource
	 */
	@Test
	public void testExtractFeatureFromResource() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			// Enable debug logging
			proc.setTrace(true);
			
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "ecf-index");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECF_P2_REPO), "ECF Test");

			// First, check that we have feature resources in the index
			org.osgi.service.repository.Repository repository = indexer.getBridge().getRepository();
			aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
				org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();
			
			java.util.Collection<org.osgi.resource.Capability> allCaps = repository.findProviders(
				java.util.Collections.singleton(req))
				.getOrDefault(req, java.util.Collections.emptyList());
			
			System.out.println("Total capabilities in repository: " + allCaps.size());
			
			// Get unique resources
			java.util.Set<org.osgi.resource.Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);
			System.out.println("Total resources in repository: " + allResources.size());
			
			// Count features
			int featureCount = 0;
			for (org.osgi.resource.Resource resource : allResources) {
				java.util.List<org.osgi.resource.Capability> identities = resource.getCapabilities(org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE);
				for (org.osgi.resource.Capability identity : identities) {
					Object type = identity.getAttributes().get(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
					if ("eclipse.feature".equals(type)) {
						featureCount++;
						if (featureCount <= 3) {
							System.out.println("Feature resource " + featureCount + ": " + identity.getAttributes().get(org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE));
							// Try to extract
							aQute.bnd.osgi.resource.ResourceUtils.ContentCapability contentCap = aQute.bnd.osgi.resource.ResourceUtils.getContentCapability(resource);
							if (contentCap == null) {
								System.out.println("  NO CONTENT CAPABILITY!");
							} else {
								System.out.println("  Content URL: " + contentCap.url());
							}
						}
						break;
					}
				}
			}
			
			System.out.println("Total feature resources found: " + featureCount);
			softly.assertThat(featureCount)
				.as("Index should contain feature resources")
				.isGreaterThan(0);

			// Get all features from the index using the getFeatures() method
			var features = indexer.getFeatures();

			System.out.println("Features extracted via getFeatures(): " + features.size());
			
			softly.assertThat(features)
				.as("Index should contain features")
				.isNotEmpty();

			// Check that each feature has proper data
			for (Feature feature : features) {
				softly.assertThat(feature.getId())
					.as("Feature should have an ID")
					.isNotNull()
					.isNotEmpty();

				softly.assertThat(feature.getVersion())
					.as("Feature should have a version")
					.isNotNull()
					.isNotEmpty();

				// Print first few features for debugging
				if (features.indexOf(feature) < 3) {
					System.out.println("Feature: " + feature.getId() + " version=" + feature.getVersion());
					System.out.println("  Label: " + feature.getLabel());
					System.out.println("  Plugins: " + feature.getPlugins()
						.size());
					System.out.println("  Includes: " + feature.getIncludes()
						.size());
				}
			}
		}
	}

	/**
	 * Test that getFeature(id, version) can retrieve a specific feature
	 */
	@Test
	public void testGetSpecificFeature() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "ecf-index-2");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECF_P2_REPO), "ECF Test");

			// Try to get a known feature from the ECF repository
			// We know this one exists from our previous test
			Feature feature = indexer.getFeature("org.eclipse.ecf.core.feature", "1.7.0.v20250304-2338");

			softly.assertThat(feature)
				.as("Should be able to retrieve specific feature")
				.isNotNull();

			if (feature != null) {
				softly.assertThat(feature.getId())
					.isEqualTo("org.eclipse.ecf.core.feature");

				softly.assertThat(feature.getVersion())
					.isEqualTo("1.7.0.v20250304-2338");

				System.out.println("Retrieved feature: " + feature);
				System.out.println("  Plugins: " + feature.getPlugins()
					.size());
				System.out.println("  Includes: " + feature.getIncludes()
					.size());
			}
		}
	}
}
