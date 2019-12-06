package biz.aQute.resolve;

import static test.lib.Utils.createRepo;
import static test.lib.Utils.findContentURI;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.lib.MockRegistry;
import test.lib.NullLogService;

@SuppressWarnings({
	"restriction", "deprecation"
})
public class BndrunResolveContextTest extends TestCase {

	private final LogService log = new NullLogService();

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	/**
	 * Simple test that checks if we can find a resource through the
	 * findProviders
	 */
	public void testSimple() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName()));

		BndrunResolveContext context = new BndrunResolveContext(new BndEditModel(), registry, log);

		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);
		assertEquals(1, providers.size());
		Resource resource = providers.get(0)
			.getResource();
		assertEquals(IO.getFile("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
	}

	/**
	 * Test the blacklist. We reject any resources that matches a specific
	 * requirements
	 */

	public void testSimpleBlacklist() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName()));

		BndEditModel model = new BndEditModel();
		Requirement blacklist = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();
		model.setRunBlacklist(Arrays.asList(blacklist));

		BndrunResolveContext context = new BndrunResolveContext(model, registry, log);

		//
		// This one is ok in testSimple
		// but should fail because we blacklisted it
		//

		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);
		assertEquals(0, providers.size());
	}

	/**
	 * See if we can reject the 4.0.2 framework, which should normally be
	 * selected because it is the highest (this is tested later).
	 */
	public void testBlacklistFramework() throws Exception {

		MockRegistry registry = new MockRegistry();
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		BndEditModel model = new BndEditModel();
		model.setRunFw("org.apache.felix.framework;version='[4,4.1)'");
		Requirement blacklist = new CapReqBuilder("osgi.identity")
			.addDirective("filter", "(&(osgi.identity=org.apache.felix.framework)(version>=4.0.1))")
			.buildSyntheticRequirement();
		model.setRunBlacklist(Arrays.asList(blacklist));

		BndrunResolveContext context = new BndrunResolveContext(model, registry, log);
		context.init();

		Resource framework = context.getFramework();
		assertNotNull(framework);

		assertEquals(IO.getFile("testdata/org.apache.felix.framework-4.0.0.jar")
			.toURI(), findContentURI(framework));
	}

	public void testEffective() throws Exception {
		BndrunResolveContext context = new BndrunResolveContext(new BndEditModel(), new MockRegistry(), log);

		Requirement resolveReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE)
			.buildSyntheticRequirement();
		Requirement activeReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE)
			.buildSyntheticRequirement();
		Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

		assertTrue(context.isEffective(resolveReq));
		assertFalse(context.isEffective(activeReq));
		assertTrue(context.isEffective(noEffectiveDirectiveReq));
	}

	public void testEffective2() throws Exception {
		BndEditModel model = new BndEditModel();
		model.genericSet(BndrunResolveContext.RUN_EFFECTIVE_INSTRUCTION, "active, arbitrary");

		BndrunResolveContext context = new BndrunResolveContext(model, new MockRegistry(), log);

		Requirement resolveReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE)
			.buildSyntheticRequirement();
		Requirement activeReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE)
			.buildSyntheticRequirement();
		Requirement arbitrary1Req = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, "arbitrary")
			.buildSyntheticRequirement();
		Requirement arbitrary2Req = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, "VeryArbitrary")
			.buildSyntheticRequirement();

		Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

		assertTrue(context.isEffective(resolveReq));
		assertTrue(context.isEffective(activeReq));
		assertTrue(context.isEffective(arbitrary1Req));
		assertFalse(context.isEffective(arbitrary2Req));
		assertTrue(context.isEffective(noEffectiveDirectiveReq));
	}

	public void testEffective3() throws Exception {
		BndEditModel model = new BndEditModel();
		model.genericSet(BndrunResolveContext.RUN_EFFECTIVE_INSTRUCTION,
			"active;skip:=\"filtered.ns,another.filtered.ns\", arbitrary");

		BndrunResolveContext context = new BndrunResolveContext(model, new MockRegistry(), log);

		Requirement resolveReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE)
			.buildSyntheticRequirement();
		Requirement activeReq = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE)
			.buildSyntheticRequirement();
		Requirement filteredActiveReq = new CapReqBuilder("filtered.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE)
			.buildSyntheticRequirement();
		Requirement anotherFilteredActiveReq = new CapReqBuilder("another.filtered.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE)
			.buildSyntheticRequirement();
		Requirement arbitrary1Req = new CapReqBuilder("dummy.ns")
			.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, "arbitrary")
			.buildSyntheticRequirement();

		Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

		assertTrue(context.isEffective(resolveReq));
		assertTrue(context.isEffective(activeReq));
		assertTrue(context.isEffective(arbitrary1Req));
		assertFalse(context.isEffective(filteredActiveReq));
		assertFalse(context.isEffective(anotherFilteredActiveReq));
		assertTrue(context.isEffective(noEffectiveDirectiveReq));
	}

	public void testEmptyInitialWirings() throws Exception {
		assertEquals(0, new BndrunResolveContext(new BndEditModel(), new MockRegistry(), log).getWirings()
			.size());
	}

	public void testBasicFindProviders() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);
		assertEquals(1, providers.size());
		Resource resource = providers.get(0)
			.getResource();

		assertEquals(IO.getFile("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
	}

	public void testProviderPreference() throws Exception {
		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();

		MockRegistry registry;
		BndrunResolveContext context;
		List<Capability> providers;
		Resource resource;

		// First try it with repo1 first
		registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName() + "1"));
		registry.addPlugin(createRepo(IO.getFile("testdata/repo2.index.xml"), getTestName() + "2"));

		context = new BndrunResolveContext(new BndEditModel(), registry, log);
		providers = context.findProviders(req);
		assertEquals(2, providers.size());
		resource = providers.get(0)
			.getResource();
		assertEquals(IO.getFile("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
		resource = providers.get(1)
			.getResource();
		assertEquals(IO.getFile("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));

		// Now try it with repo2 first
		registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo2.index.xml"), getTestName() + "3"));
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName() + "4"));

		context = new BndrunResolveContext(new BndEditModel(), registry, log);
		providers = context.findProviders(req);
		assertEquals(2, providers.size());
		resource = providers.get(0)
			.getResource();
		assertEquals(IO.getFile("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
		resource = providers.get(1)
			.getResource();
		assertEquals(IO.getFile("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
	}

	public void testReorderRepositories() throws Exception {
		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)")
			.buildSyntheticRequirement();

		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo1.index.xml"), getTestName() + "1"));
		registry.addPlugin(createRepo(IO.getFile("testdata/repo2.index.xml"), getTestName() + "2"));

		BndrunResolveContext context;
		List<Capability> providers;
		Resource resource;
		BndEditModel runModel;

		runModel = new BndEditModel();
		runModel.setRunRepos(Arrays.asList(getTestName() + "2", getTestName() + "1"));

		context = new BndrunResolveContext(runModel, registry, log);
		providers = context.findProviders(req);
		assertEquals(2, providers.size());
		resource = providers.get(0)
			.getResource();
		assertEquals(IO.getFile("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
		resource = providers.get(1)
			.getResource();
		assertEquals(IO.getFile("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar")
			.toURI(), findContentURI(resource));
	}

	public void testFrameworkIsMandatory() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		context.init();

		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(), findContentURI(context.getFramework()));
	}

	public void testChooseHighestFrameworkVersion() throws Exception {
		MockRegistry registry;
		BndEditModel runModel;
		BndrunResolveContext context;
		Collection<Resource> resources;
		Resource fwkResource;

		registry = new MockRegistry();
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.0.index.xml"), getTestName() + "1"));
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName() + "2"));

		runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

		context = new BndrunResolveContext(runModel, registry, log);
		context.init();

		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(), findContentURI(context.getFramework()));

		// Try it the other way round
		registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName() + "3"));
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.0.index.xml"), getTestName() + "4"));

		runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

		context = new BndrunResolveContext(runModel, registry, log);
		context.init();

		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(), findContentURI(context.getFramework()));
	}

	public void testFrameworkCapabilitiesPreferredOverRepository() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(2, providers.size());
		assertEquals(IO.getFile("testdata/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
		assertEquals(IO.getFile("testdata/osgi.cmpn-4.3.0.jar")
			.toURI(),
			findContentURI(providers.get(1)
				.getResource()));
	}

	public void testResolverHookFiltersResult() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		registry.addPlugin((ResolverHook) (requirement, candidates) -> {
			for (Iterator<Capability> iter = candidates.iterator(); iter.hasNext();) {
				Object id = iter.next()
					.getResource()
					.getCapabilities("osgi.identity")
					.get(0)
					.getAttributes()
					.get("osgi.identity");
				if ("osgi.cmpn".equals(id))
					iter.remove();
			}
		});

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
		// The capability from osgi.cmpn is NOT here
	}

	public void testResolverHookFiltersResultWithBlacklist() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String, String> blacklistProp = new HashMap<>();

		BndEditModel runModel = new BndEditModel();
		ArrayList<Requirement> blacklistlist = new ArrayList<>();

		blacklistlist.add(CapReqBuilder.createSimpleRequirement("osgi.identity", "osgi.cmpn", "4.3.0")
			.buildSyntheticRequirement());
		runModel.setRunBlacklist(blacklistlist);
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(1, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
			.getResource()));
	}

	public void testResolverHookFiltersResultWithBlacklistAndVersionRange1() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String, String> blacklistProp = new HashMap<>();

		BndEditModel runModel = new BndEditModel();
		ArrayList<Requirement> blacklistlist = new ArrayList<>();
		blacklistlist.add(CapReqBuilder.createSimpleRequirement("osgi.identity", "osgi.cmpn", "[4.0.0,4.3.0)")
			.buildSyntheticRequirement());
		runModel.setRunBlacklist(blacklistlist);
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(2, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
			.getResource()));
		assertEquals(new File("testdata/osgi.cmpn-4.3.0.jar").toURI(), findContentURI(providers.get(1)
			.getResource()));
	}

	public void testResolverHookFiltersResultWithBlacklistAndVersionRange2() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String, String> blacklistProp = new HashMap<>();

		BndEditModel runModel = new BndEditModel();
		ArrayList<Requirement> blacklistlist = new ArrayList<>();
		blacklistlist.add(CapReqBuilder.createSimpleRequirement("osgi.identity", "osgi.cmpn", "[4.0.0,4.4.0)")
			.buildSyntheticRequirement());
		runModel.setRunBlacklist(blacklistlist);
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(1, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
			.getResource()));
	}

	public void testResolverHookCannotFilterFrameworkCapabilities() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/osgi.cmpn-4.3.0.index.xml"), getTestName() + "1"));
		registry
			.addPlugin(
				createRepo(IO.getFile("testdata/org.apache.felix.framework-4.0.2.index.xml"), getTestName() + "2"));

		// Add a hook that tries to remove all capabilities from resource with
		// id "org.apache.felix.framework"
		registry.addPlugin((ResolverHook) (requirement, candidates) -> {
			for (Iterator<Capability> iter = candidates.iterator(); iter.hasNext();) {
				Object id = iter.next()
					.getResource()
					.getCapabilities("osgi.identity")
					.get(0)
					.getAttributes()
					.get("osgi.identity");
				if ("org.apache.felix.framework".equals(id)) {
					fail("this line should not be reached");
				}
			}
		});

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
			.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		// The filter was ineffective
		assertEquals(2, providers.size());
		assertEquals(IO.getFile("testdata/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
		assertEquals(IO.getFile("testdata/osgi.cmpn-4.3.0.jar")
			.toURI(),
			findContentURI(providers.get(1)
				.getResource()));
	}

	public void testPreferLeastRequirementsAndMostCapabilities() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo4/index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("x").buildSyntheticRequirement();
		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(3, providers.size());
		// x.3 has same requirements but more capabilities than x.2
		assertEquals(IO.getFile("testdata/repo4/x.3.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
		// x.2 has same capabilities but fewer requirements than x.1
		assertEquals(IO.getFile("testdata/repo4/x.2.jar")
			.toURI(),
			findContentURI(providers.get(1)
				.getResource()));
		assertEquals(IO.getFile("testdata/repo4/x.1.jar")
			.toURI(),
			findContentURI(providers.get(2)
				.getResource()));
	}

	public void testResolvePreferences() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo4/index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.genericSet("-resolve.preferences", "x.1");

		Requirement requirement = new CapReqBuilder("x").buildSyntheticRequirement();
		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(3, providers.size());
		assertEquals(IO.getFile("testdata/repo4/x.1.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
		assertEquals(IO.getFile("testdata/repo4/x.3.jar")
			.toURI(),
			findContentURI(providers.get(1)
				.getResource()));
		assertEquals(IO.getFile("testdata/repo4/x.2.jar")
			.toURI(),
			findContentURI(providers.get(2)
				.getResource()));
	}

	public void testSelfCapabilityPreferredOverRepository() throws Exception {
		MockRegistry registry = new MockRegistry();
		Repository repo = createRepo(IO.getFile("testdata/repo4.index.xml"), getTestName());

		registry.addPlugin(repo);

		Requirement resourceReq = new CapReqBuilder("osgi.identity")
			.addDirective("filter", "(osgi.identity=dummy-selfcap)")
			.buildSyntheticRequirement();
		Resource resource = repo.findProviders(Collections.singleton(resourceReq))
			.get(resourceReq)
			.iterator()
			.next()
			.getResource();

		Requirement packageReq = resource.getRequirements("osgi.wiring.package")
			.get(0);

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(packageReq);

		assertNotNull(providers);
		assertEquals(2, providers.size());
		assertEquals(IO.getFile("testdata/repo4/dummy.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testInputRequirementsAsMandatoryResource() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement req = new CapReqBuilder("osgi.identity")
			.addDirective("filter", "(osgi.identity=org.apache.felix.gogo.command)")
			.buildSyntheticRequirement();
		runModel.setRunRequires(Collections.singletonList(req));

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		context.init();

		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(), findContentURI(context.getFramework()));

		Collection<Resource> mandRes = context.getMandatoryResources();

		assertEquals(1, mandRes.size());
		Resource resource = mandRes.iterator()
			.next();
		assertNotNull(resource);

		IdentityCapability ic = ResourceUtils.getIdentityCapability(resource);
		assertNotNull(ic);

		assertEquals("<<INITIAL>>", ic.osgi_identity());
	}

	public void testEERequirementResolvesFramework() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.ee").addDirective("filter", "(osgi.ee=J2SE-1.5)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testJREPackageResolvesFramework() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = CapReqBuilder.createPackageRequirement("javax.annotation", null)
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testJREPackageNotResolved() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.J2SE_1_5); // javax.annotation added in Java 6

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		Requirement req = CapReqBuilder.createPackageRequirement("javax.annotation", null)
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(0, providers.size());
	}

	public void testDontResolveBuildOnlyLibraries() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/buildrepo.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		BndrunResolveContext context;

		context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers1 = context
			.findProviders(CapReqBuilder.createPackageRequirement("org.osgi.framework", null)
				.buildSyntheticRequirement());
		assertEquals(0, providers1.size());

		context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers2 = context
			.findProviders(CapReqBuilder.createPackageRequirement("java.security", null)
				.buildSyntheticRequirement());
		assertEquals(0, providers2.size());
	}

	public void testResolveSystemBundleAlias() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.wiring.host")
			.addDirective("filter", "(osgi.wiring.host=system.bundle)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testUnsatisfiedSystemPackage() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=sun.reflect)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(0, providers.size());
	}

	public void testResolveSystemPackagesExtra() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);
		runModel.setSystemPackages(Collections.singletonList(new ExportedPackage("sun.reflect", new Attrs())));

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.wiring.package")
			.addDirective("filter", "(osgi.wiring.package=sun.reflect)")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testUnsatisfiedRequirement() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.extender")
			.addDirective("filter", "(&(osgi.extender=foobar)(version>=1.0))")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);
		assertEquals(0, providers.size());
	}

	public void testResolveSystemCapabilitiesExtra() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");
		runModel.setEE(EE.JavaSE_1_6);
		runModel.genericSet("-runsystemcapabilities", "osgi.extender;osgi.extender=foobar;version:Version=1.0");

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

		Requirement req = new CapReqBuilder("osgi.extender")
			.addDirective("filter", "(&(osgi.extender=foobar)(version>=1.0))")
			.buildSyntheticRequirement();
		List<Capability> providers = context.findProviders(req);

		assertEquals(1, providers.size());
		assertEquals(IO.getFile("testdata/repo3/org.apache.felix.framework-4.0.2.jar")
			.toURI(),
			findContentURI(providers.get(0)
				.getResource()));
	}

	public void testResolveProvidedCapabilitiesWithDistro() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getTestName()));

		BndEditModel model = new BndEditModel();

		model.genericSet("-runprovidedcapabilities",
			"osgi.service;objectClass=foo.bar.FooBarService;effective:=active");

		model.setDistro(Arrays.asList("testdata/distro.jar;version=file"));

		List<Requirement> requires = new ArrayList<>();
		List<Capability> caps = CapReqBuilder
			.getCapabilitiesFrom(new Parameters("osgi.service;objectClass=foo.bar.FooBarService;effective:=active"));
		Requirement req = CapReqBuilder.createRequirementFromCapability(caps.get(0))
			.buildSyntheticRequirement();
		requires.add(req);

		model.setRunRequires(requires);
		BndrunResolveContext context = new BndrunResolveContext(model, registry, log);
		context.setLevel(0);
		context.init();
		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(logger);

			resolver.resolve(context);
		}
	}

}
