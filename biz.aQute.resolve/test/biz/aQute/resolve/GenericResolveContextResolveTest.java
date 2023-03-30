package biz.aQute.resolve;

import static aQute.bnd.osgi.resource.MultiReleaseNamespace.MULTI_RELEASE_VERSION_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;
import static test.lib.Utils.createRepo;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;

@SuppressWarnings("restriction")
public class GenericResolveContextResolveTest {
	ResolverLogger	logger	= new ResolverLogger(0, System.out);

	@InjectTemporaryDirectory
	File			tmp;

	private String getTestName() {
		return tmp.getName();
	}

	@Test
	public void testContractCase() throws Exception {
		Processor p = new Processor();
		SimpleIndexer indexer = new SimpleIndexer();
		List<Resource> resources2 = indexer.reporter(p)
			.files(new FileSet(IO.getFile("testdata/jar"), "**.jar").getFiles())
			.getResources();
		ResourcesRepository repository = new ResourcesRepository(resources2);

		GenericResolveContext grc = new GenericResolveContext(logger);
		grc.setLevel(2);
		grc.addRepository(repository);
		grc.addEE(EE.JavaSE_17);
		grc.addFramework("org.apache.felix.framework", null);
		grc.addRequireBundle("org.apache.felix.http.jetty", new VersionRange("0"));
		grc.done();

		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(new ResolverLogger(4));
			Set<Resource> resources = resolver.resolve(grc)
				.keySet();

			assertThat(resources).hasSize(4);
		}
	}

	/**
	 * Simple basic resolve. We use a small index with gogo + framework and then
	 * try to see if we can resolve the runtime from the shell requirement.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSimpleResolve() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName(), tmp);
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

	@Test
	public void testMultiReleaseJar() throws Exception {
		ResourcesRepository repository = new ResourcesRepository();
		SupportingResource multirelease = ResourceBuilder.parse(IO.getFile("testdata/jar/multi-release-ok.jar"), null);
		repository.add(multirelease);

		List<Capability> bundles = repository
			.findProvider(RequirementBuilder.createBundleRequirement("multirelease.main", "0.0.0")
				.buildSyntheticRequirement());

		Resource v1_8 = bundles.stream()
			.map(Capability::getResource)
			.filter(r -> !r.getCapabilities(CONTENT_NAMESPACE)
				.stream()
				.anyMatch(c -> String.valueOf(c.getAttributes()
					.get(CAPABILITY_URL_ATTRIBUTE))
					.contains(MULTI_RELEASE_VERSION_ATTRIBUTE)))
			.findFirst()
			.get();
		Resource v9 = bundles.stream()
			.map(Capability::getResource)
			.filter(r -> r.getCapabilities(CONTENT_NAMESPACE)
				.stream()
				.allMatch(c -> String.valueOf(c.getAttributes()
					.get(CAPABILITY_URL_ATTRIBUTE))
					.endsWith(MULTI_RELEASE_VERSION_ATTRIBUTE + "=9")))
			.findFirst()
			.get();
		Resource v12 = bundles.stream()
			.map(Capability::getResource)
			.filter(r -> r.getCapabilities(CONTENT_NAMESPACE)
				.stream()
				.allMatch(c -> String.valueOf(c.getAttributes()
					.get(CAPABILITY_URL_ATTRIBUTE))
					.endsWith(MULTI_RELEASE_VERSION_ATTRIBUTE + "=12")))
			.findFirst()
			.get();
		Resource v17 = bundles.stream()
			.map(Capability::getResource)
			.filter(r -> r.getCapabilities(CONTENT_NAMESPACE)
				.stream()
				.allMatch(c -> String.valueOf(c.getAttributes()
					.get(CAPABILITY_URL_ATTRIBUTE))
					.endsWith(MULTI_RELEASE_VERSION_ATTRIBUTE + "=17")))
			.findFirst()
			.get();

		Repository repo3 = createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName(), tmp);

		SortedSet<EE> tailSet = new TreeSet<>(EE.all()
			.tailSet(EE.JavaSE_1_8));
		tailSet.remove(EE.UNKNOWN);

		for (EE ee : tailSet) {
			GenericResolveContext grc = new GenericResolveContext(logger);
			grc.setLevel(2);
			grc.addRepository(repository);
			grc.addRepository(repo3);
			grc.addEE(ee);
			grc.addFramework("org.apache.felix.framework", null);
			Attrs attrs = new Attrs();
			attrs.put("fake", "fake");
			attrs.putTyped("version", new Version("1.2.3"));
			grc.addCapability("fake", attrs);

			grc.addRequireBundle("multirelease.main", new VersionRange("[0,1]"));
			grc.done();

			try (ResolverLogger logger = new ResolverLogger(4)) {
				Resolver resolver = new BndResolver(new ResolverLogger(4));

				try {
					Set<Resource> resources = resolver.resolve(grc)
						.keySet();
					assertThat(resources).hasSize(3);

					switch (ee) {
						case JavaSE_1_8 -> assertThat(resources).contains(v1_8);
						case JavaSE_9 -> assertThat(resources).contains(v9);
						case JavaSE_10 -> assertThat(resources).contains(v9);
						case JavaSE_11 -> assertThat(resources).contains(v9);
						case JavaSE_12 -> assertThat(resources).contains(v12);
						case JavaSE_13 -> assertThat(resources).contains(v12);
						case JavaSE_14 -> assertThat(resources).contains(v12);
						case JavaSE_15 -> assertThat(resources).contains(v12);
						case JavaSE_16 -> assertThat(resources).contains(v12);
						case JavaSE_17 -> assertThat(resources).contains(v17);
						default -> assertThat(resources).contains(v17);
					}
				} catch (Exception e) {
					System.out.println(logger.getLog());
					e.printStackTrace();
					fail("exception in resolve");
				}
			}
		}
	}

	/**
	 * Check default directive
	 *
	 * @throws Exception
	 */
	@Test
	public void testResolveRequirementNoDirective() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName(), tmp);
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
	@Test
	public void testResolveRequirementResolveDirective() throws Exception {

		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName(), tmp);
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

	@Test
	public void testResolveRequirementActiveDirective() throws Exception {
		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"), getTestName(), tmp);
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
			if (identities.size() == 1) {
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
