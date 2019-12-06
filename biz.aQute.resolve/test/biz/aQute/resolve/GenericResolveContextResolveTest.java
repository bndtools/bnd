package biz.aQute.resolve;

import static test.lib.Utils.createRepo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class GenericResolveContextResolveTest extends TestCase {
	ResolverLogger logger = new ResolverLogger(0, System.out);

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	/**
	 * Simple basic resolve. We use a small index with gogo + framework and then
	 * try to see if we can resolve the runtime from the shell requirement.
	 *
	 * @throws Exception
	 */
	public void testSimpleResolve() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName());
		GenericResolveContext grc = new GenericResolveContext(logger);
		grc.setLevel(2);
		grc.addRepository(repository);

		grc.addFramework("org.apache.felix.framework", null);
		grc.addEE(EE.JavaSE_1_7);
		grc.addRequireBundle("org.apache.felix.gogo.shell", new VersionRange("[0,1]"));
		grc.done();
		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(new ResolverLogger(4));

			Set<Resource> resources = resolver.resolve(grc)
				.keySet();
			assertNotNull(getResource(resources, "org.apache.felix.gogo.runtime", "0.10"));
		}
	}

	/**
	 * Check default directive
	 *
	 * @throws Exception
	 */
	public void testResolveRequirementNoDirective() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName());
		GenericResolveContext grc = new GenericResolveContext(logger);
		grc.setLevel(2);
		grc.addRepository(repository);

		Requirement logservice = new CapReqBuilder("osgi.service")
			.addDirective("filter", "(objectClass=org.osgi.service.log.LogService)")
			.buildSyntheticRequirement();
		List<Capability> providers = grc.findProviders(logservice);

		assertEquals(2, providers.size());

		assertNames(providers, "test.a", "test.b");
	}

	/**
	 * Check expressly set directive
	 *
	 * @throws Exception
	 */
	public void testResolveRequirementResolveDirective() throws Exception {

		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName());
		GenericResolveContext grc = new GenericResolveContext(logger);
		grc.addRepository(repository);
		Requirement logservice = new CapReqBuilder("osgi.service")
			.addDirective("filter", "(objectClass=org.osgi.service.log.LogService)")
			.addDirective("effective", "resolve")
			.buildSyntheticRequirement();
		List<Capability> providers = grc.findProviders(logservice);

		assertEquals(2, providers.size());

		assertNames(providers, "test.a", "test.b");
	}

	public void testResolveRequirementActiveDirective() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName());
		GenericResolveContext grc = new GenericResolveContext(logger);
		grc.addRepository(repository);

		Requirement logservice = new CapReqBuilder("osgi.service")
			.addDirective("filter", "(objectClass=org.osgi.service.log.LogService)")
			.addDirective("effective", "active")
			.buildSyntheticRequirement();
		List<Capability> providers = grc.findProviders(logservice);

		assertEquals(3, providers.size());

		assertNames(providers, "test.a", "test.b", "test.c");
	}

	private static Resource getResource(Set<Resource> resources, String bsn, String versionString) {
		for (Resource resource : resources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			if (identities != null && identities.size() == 1) {
				Capability idCap = identities.get(0);
				Object id = idCap.getAttributes()
					.get(IdentityNamespace.IDENTITY_NAMESPACE);
				Object version = idCap.getAttributes()
					.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (bsn.equals(id)) {
					if (versionString == null) {
						return resource;
					}
					Version requested = Version.parseVersion(versionString);
					Version current;
					if (version instanceof Version) {
						current = (Version) version;
					} else {
						current = Version.parseVersion("" + version);
					}
					if (requested.equals(current)) {
						return resource;
					}
				}
			}
		}
		return null;
	}

	void assertNames(List<Capability> providers, String... ids) {
		Set<String> resourceNames = new HashSet<>();
		for (Capability cap : providers) {
			resourceNames.add(cap.getResource()
				.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
				.get(0)
				.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE)
				.toString());
		}

		Set<String> expectedResourceNames = new HashSet<>(Arrays.asList(ids));

		assertEquals(expectedResourceNames, resourceNames);
	}

}
