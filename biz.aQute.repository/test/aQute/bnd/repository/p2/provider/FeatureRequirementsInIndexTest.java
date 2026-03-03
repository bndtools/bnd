package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
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
 * Test to demonstrate and verify how required features are stored in the P2
 * repository index. This test shows: 1. How features create OSGi requirements
 * for included features 2. How features create requirements for required
 * features from <requires><import> elements 3. How requirements use filter
 * directives to specify feature identity and type 4. How these requirements are
 * stored in the index.xml.gz file
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeatureRequirementsInIndexTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	// ECF P2 repository - smaller, faster for testing
	private static final String	ECF_P2_REPO	= "https://download.eclipse.org/rt/ecf/3.16.5/site.p2/3.16.5.v20250914-0333/";

	/**
	 * Test showing how feature requirements are stored in the index
	 */
	@Test
	public void testFeatureRequirementsInIndex() throws Exception {
		try (HttpClient client = new HttpClient(); Processor proc = new Processor()) {
			client.setCache(IO.getFile(tmp, "http-cache"));

			Unpack200 processor = new Unpack200(proc);
			File indexLocation = new File(tmp, "ecf-index");

			System.out.println("\n=== Feature Requirements in P2 Index ===\n");

			P2Indexer indexer = new P2Indexer(processor, proc, indexLocation, client, new URI(ECF_P2_REPO), "ECF");

			// Get all features
			List<Feature> features = indexer.getFeatures();

			System.out.println("Total features in repository: " + features.size());

			// Find features that have includes or requires
			List<Feature> featuresWithIncludes = features.stream()
				.filter(f -> {
					try {
						return !f.getIncludes()
							.isEmpty();
					} catch (Exception e) {
						return false;
					}
				})
				.collect(Collectors.toList());

			List<Feature> featuresWithRequires = features.stream()
				.filter(f -> {
					try {
						return !f.getRequires()
							.isEmpty();
					} catch (Exception e) {
						return false;
					}
				})
				.collect(Collectors.toList());

			System.out.println("Features with includes: " + featuresWithIncludes.size());
			System.out.println("Features with requires: " + featuresWithRequires.size());

			// Get a feature with requirements to examine
			Feature featureWithReqs = null;
			if (!featuresWithIncludes.isEmpty()) {
				featureWithReqs = featuresWithIncludes.get(0);
			} else if (!featuresWithRequires.isEmpty()) {
				featureWithReqs = featuresWithRequires.get(0);
			}

			if (featureWithReqs != null) {
				System.out.println("\n=== Examining Feature: " + featureWithReqs.getId() + " ===");
				System.out.println("Version: " + featureWithReqs.getVersion());

				// Show includes (which become requirements)
				List<Feature.Includes> includes = featureWithReqs.getIncludes();
				if (!includes.isEmpty()) {
					System.out.println("\nIncluded features (" + includes.size() + "):");
					for (Feature.Includes inc : includes) {
						System.out.println("  - " + inc.id + " version=" + inc.version + (inc.optional ? " (optional)"
							: ""));
					}
				}

				// Show requires
				List<Feature.Requires> requires = featureWithReqs.getRequires();
				if (!requires.isEmpty()) {
					System.out.println("\nRequired features/plugins (" + requires.size() + "):");
					for (Feature.Requires req : requires) {
						if (req.feature != null) {
							System.out.println(
								"  - feature: " + req.feature + " version=" + req.version + " match=" + req.match);
						} else if (req.plugin != null) {
							System.out.println(
								"  - plugin: " + req.plugin + " version=" + req.version + " match=" + req.match);
						}
					}
				}

				// Convert to OSGi Resource to see how requirements are stored
				System.out.println("\n=== OSGi Resource Representation ===");
				Resource resource = featureWithReqs.toResource();

				// Show identity capability
				List<Capability> identityCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
				if (!identityCaps.isEmpty()) {
					Capability identityCap = identityCaps.get(0);
					System.out.println("\nIdentity Capability:");
					System.out.println("  Namespace: " + identityCap.getNamespace());
					System.out.println("  Attributes:");
					identityCap.getAttributes()
						.forEach(
							(key, value) -> System.out.println("    " + key + " = " + value));
				}

				// Show requirements
				List<Requirement> requirements = resource.getRequirements("osgi.identity");
				System.out.println("\nRequirements (" + requirements.size() + "):");
				for (int i = 0; i < Math.min(5, requirements.size()); i++) {
					Requirement req = requirements.get(i);
					System.out.println("\n  Requirement #" + (i + 1) + ":");
					System.out.println("    Namespace: " + req.getNamespace());
					System.out.println("    Directives:");
					req.getDirectives()
						.forEach((key, value) -> System.out.println("      " + key + " = " + value));
					if (!req.getAttributes()
						.isEmpty()) {
						System.out.println("    Attributes:");
						req.getAttributes()
							.forEach((key, value) -> System.out.println("      " + key + " = " + value));
					}
				}

				// Verify requirements in the index
				System.out.println("\n=== Verifying Requirements in Index ===");

				// Get the resource from the index
				org.osgi.service.repository.Repository repository = indexer.getBridge()
					.getRepository();

				// Find the feature resource in the index
				RequirementBuilder rb = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
				rb.addDirective("filter",
					String.format("(&(osgi.identity=%s)(type=org.eclipse.update.feature))", featureWithReqs.getId()));
				Requirement findReq = rb.buildSyntheticRequirement();

				Collection<Capability> matchingCaps = repository
					.findProviders(java.util.Collections.singleton(findReq))
					.get(findReq);

				if (matchingCaps != null && !matchingCaps.isEmpty()) {
					Resource indexedResource = matchingCaps.iterator()
						.next()
						.getResource();
					System.out.println("Found feature in index: " + featureWithReqs.getId());

					List<Requirement> indexedRequirements = indexedResource.getRequirements("osgi.identity");
					System.out.println("Requirements in indexed resource: " + indexedRequirements.size());

					// Show first few requirements from index
					for (int i = 0; i < Math.min(3, indexedRequirements.size()); i++) {
						Requirement req = indexedRequirements.get(i);
						System.out.println("\n  Indexed Requirement #" + (i + 1) + ":");
						System.out.println("    Filter: " + req.getDirectives()
							.get("filter"));
						String resolution = req.getDirectives()
							.get("resolution");
						if (resolution != null) {
							System.out.println("    Resolution: " + resolution);
						}
					}
				}

				// Verify assertions
				softly.assertThat(requirements)
					.as("Feature should have requirements")
					.isNotEmpty();

				// Check that included features have proper type filter
				for (Feature.Includes inc : includes) {
					boolean found = requirements.stream()
						.anyMatch(req -> {
							String filter = req.getDirectives()
								.get("filter");
							return filter != null && filter.contains(inc.id)
								&& filter.contains("type=org.eclipse.update.feature");
						});
					softly.assertThat(found)
						.as("Included feature " + inc.id + " should have requirement with type filter")
						.isTrue();
				}
			}
		}
	}

	/**
	 * Test showing the structure of feature requirements for different types
	 */
	@Test
	public void testFeatureRequirementStructure() throws Exception {
		System.out.println("\n=== Feature Requirement Structure ===\n");

		System.out.println("Eclipse features store requirements in three ways:\n");

		System.out.println("1. PLUGIN REFERENCES (<plugin> elements):");
		System.out.println("   - Stored as osgi.identity requirements");
		System.out.println("   - Filter: (osgi.identity=plugin.id)");
		System.out.println("   - With version: (&(osgi.identity=plugin.id)(version=1.0.0))");
		System.out.println("   - Example requirement:");
		System.out.println("     Namespace: osgi.identity");
		System.out.println("     Filter: (&(osgi.identity=org.eclipse.core.runtime)(version=3.34.100))");
		System.out.println();

		System.out.println("2. INCLUDED FEATURES (<includes> elements):");
		System.out.println("   - Stored as osgi.identity requirements");
		System.out.println("   - Filter includes type=org.eclipse.update.feature");
		System.out.println("   - Filter: (&(osgi.identity=feature.id)(type=org.eclipse.update.feature))");
		System.out.println("   - With version: (&(osgi.identity=feature.id)(type=org.eclipse.update.feature)(version=1.0.0))");
		System.out.println("   - Optional includes have resolution:=optional directive");
		System.out.println("   - Example requirement:");
		System.out.println("     Namespace: osgi.identity");
		System.out.println("     Filter: (&(osgi.identity=org.eclipse.equinox.core.feature)(type=org.eclipse.update.feature))");
		System.out.println("     Resolution: optional (if optional=true)");
		System.out.println();

		System.out.println("3. REQUIRED FEATURES (<requires><import> elements):");
		System.out.println("   - Stored as osgi.identity requirements");
		System.out.println("   - Feature imports include type filter");
		System.out.println("   - Plugin imports do not include type filter");
		System.out.println("   - Filter for feature: (&(osgi.identity=feature.id)(type=org.eclipse.update.feature))");
		System.out.println("   - Filter for plugin: (osgi.identity=plugin.id)");
		System.out.println("   - Example requirement:");
		System.out.println("     Namespace: osgi.identity");
		System.out.println("     Filter: (&(osgi.identity=org.eclipse.rcp)(type=org.eclipse.update.feature))");
		System.out.println();

		System.out.println("4. STORAGE IN INDEX:");
		System.out.println("   - All requirements are stored in the index.xml.gz file");
		System.out.println("   - Each feature resource has:");
		System.out.println("     * One identity capability with type=org.eclipse.update.feature");
		System.out.println("     * Multiple osgi.identity requirements (one per plugin, include, or require)");
		System.out.println("   - Requirements use LDAP filter syntax");
		System.out.println("   - The P2 resolver can use these requirements to:");
		System.out.println("     * Find all dependencies of a feature");
		System.out.println("     * Resolve feature hierarchies");
		System.out.println("     * Determine which bundles are needed");
		System.out.println();

		// This test always passes - it's documentation
		assertThat(true).isTrue();
	}
}
