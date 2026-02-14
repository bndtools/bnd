package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.p2.packed.Unpack200;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;

/**
 * Test that P2Indexer correctly processes Eclipse features and includes them
 * in the index with proper capabilities
 */
@ExtendWith(SoftAssertionsExtension.class)
public class P2IndexerFeatureTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	private static final String	ECF_P2_REPO	= "https://download.eclipse.org/rt/ecf/3.16.5/site.p2/3.16.5.v20250914-0333/";

	/**
	 * Test that the P2 indexer includes features with eclipse.feature type in
	 * the index
	 */
	@Test
	public void testIndexContainsEclipseFeatures() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			Unpack200 processor = new Unpack200(proc);

			File indexLocation = new File(tmp, "ecf-index");
			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECF_P2_REPO), "ECF Test");

			// Get the bridge repository which should contain the indexed resources
			var bridge = indexer.getBridge();
			var repository = bridge.getRepository();

			// Create a requirement that matches all identity capabilities
			aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
				IdentityNamespace.IDENTITY_NAMESPACE);
			org.osgi.resource.Requirement req = rb.buildSyntheticRequirement();

			// Find all providers
			var providers = repository.findProviders(java.util.Collections.singleton(req));
			java.util.Collection<Capability> allCaps = providers.get(req);

			softly.assertThat(allCaps)
				.as("Index should contain identity capabilities")
				.isNotEmpty();

			// Get unique resources
			java.util.Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);

			softly.assertThat(allResources)
				.as("Index should contain resources")
				.isNotEmpty();

			// Find resources with type=eclipse.feature
			List<Resource> featureResources = allResources.stream()
				.filter(r -> {
					List<Capability> identityCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
					if (identityCaps.isEmpty())
						return false;

					Object type = identityCaps.get(0)
						.getAttributes()
						.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
					return "eclipse.feature".equals(type);
				})
				.toList();

			softly.assertThat(featureResources)
				.as("Index should contain eclipse.feature resources")
				.isNotEmpty();

			// Log what we found
			System.out.println("Total resources in index: " + allResources.size());
			System.out.println("Feature resources in index: " + featureResources.size());

			for (Resource feature : featureResources.stream()
				.limit(5)
				.toList()) {
				List<Capability> identityCaps = feature.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
				if (!identityCaps.isEmpty()) {
					Capability identityCap = identityCaps.get(0);
					Object id = identityCap.getAttributes()
						.get(IdentityNamespace.IDENTITY_NAMESPACE);
					Object version = identityCap.getAttributes()
						.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					System.out.println("  Feature: " + id + " version=" + version);
				}
			}

			// Verify at least one feature has proper structure
			if (!featureResources.isEmpty()) {
				Resource firstFeature = featureResources.get(0);
				List<Capability> identityCaps = firstFeature.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);

				softly.assertThat(identityCaps)
					.as("Feature should have identity capability")
					.hasSize(1);

				Capability identityCap = identityCaps.get(0);
				softly.assertThat(identityCap.getAttributes()
					.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
					.as("Feature identity should have type=eclipse.feature")
					.isEqualTo("eclipse.feature");

				softly.assertThat(identityCap.getAttributes()
					.get(IdentityNamespace.IDENTITY_NAMESPACE))
					.as("Feature should have an identity")
					.isNotNull();

				softly.assertThat(identityCap.getAttributes()
					.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
					.as("Feature should have a version")
					.isNotNull();
			}
		}
	}
}
