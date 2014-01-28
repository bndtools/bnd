package biz.aQute.resolve.internal;

import static test.lib.Utils.*;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.resource.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;

import test.lib.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.resolve.hook.*;

public class BndrunResolveContextTest extends TestCase {

    private static final LogService log = new NullLogService();

    public static void testEffective() {
        BndrunResolveContext context = new BndrunResolveContext(new BndEditModel(), new MockRegistry(), log);

        Requirement resolveReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE).buildSyntheticRequirement();
        Requirement activeReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE).buildSyntheticRequirement();
        Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

        assertTrue(context.isEffective(resolveReq));
        assertFalse(context.isEffective(activeReq));
        assertTrue(context.isEffective(noEffectiveDirectiveReq));
    }

    public static void testEffective2() {
        BndEditModel model = new BndEditModel();
        model.genericSet(BndrunResolveContext.RUN_EFFECTIVE_INSTRUCTION, "active, arbitrary");

        BndrunResolveContext context = new BndrunResolveContext(model, new MockRegistry(), log);

        Requirement resolveReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE).buildSyntheticRequirement();
        Requirement activeReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE).buildSyntheticRequirement();
        Requirement arbitrary1Req = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, "arbitrary").buildSyntheticRequirement();
        Requirement arbitrary2Req = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, "VeryArbitrary").buildSyntheticRequirement();

        Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

        assertTrue(context.isEffective(resolveReq));
        assertTrue(context.isEffective(activeReq));
        assertTrue(context.isEffective(arbitrary1Req));
        assertFalse(context.isEffective(arbitrary2Req));
        assertTrue(context.isEffective(noEffectiveDirectiveReq));
    }

    public static void testEmptyInitialWirings() {
        assertEquals(0, new BndrunResolveContext(new BndEditModel(), new MockRegistry(), log).getWirings().size());
    }

    public static void testBasicFindProviders() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml")));

        BndEditModel runModel = new BndEditModel();
        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);
        assertEquals(1, providers.size());
        Resource resource = providers.get(0).getResource();

        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

    public static void testProviderPreference() {
        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)").buildSyntheticRequirement();

        MockRegistry registry;
        BndrunResolveContext context;
        List<Capability> providers;
        Resource resource;

        // First try it with repo1 first
        registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/repo2.index.xml")));

        context = new BndrunResolveContext(new BndEditModel(), registry, log);
        providers = context.findProviders(req);
        assertEquals(2, providers.size());
        resource = providers.get(0).getResource();
        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
        resource = providers.get(1).getResource();
        assertEquals(new File("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));

        // Now try it with repo2 first
        registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo2.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml")));

        context = new BndrunResolveContext(new BndEditModel(), registry, log);
        providers = context.findProviders(req);
        assertEquals(2, providers.size());
        resource = providers.get(0).getResource();
        assertEquals(new File("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
        resource = providers.get(1).getResource();
        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

    public static void testReorderRepositories() {
        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)").buildSyntheticRequirement();

        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml"), "Repository1"));
        registry.addPlugin(createRepo(new File("testdata/repo2.index.xml"), "Repository2"));

        BndrunResolveContext context;
        List<Capability> providers;
        Resource resource;
        BndEditModel runModel;

        runModel = new BndEditModel();
        runModel.setRunRepos(Arrays.asList(new String[] {
                "Repository2", "Repository1"
        }));

        context = new BndrunResolveContext(runModel, registry, log);
        providers = context.findProviders(req);
        assertEquals(2, providers.size());
        resource = providers.get(0).getResource();
        assertEquals(new File("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
        resource = providers.get(1).getResource();
        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

    public static void testFrameworkIsMandatory() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        Collection<Resource> resources = context.getMandatoryResources();
        assertEquals(1, resources.size());
        Resource fwkResource = resources.iterator().next();
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(fwkResource));
    }

    public static void testChooseHighestFrameworkVersion() {
        MockRegistry registry;
        BndEditModel runModel;
        BndrunResolveContext context;
        Collection<Resource> resources;
        Resource fwkResource;

        registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.0.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

        context = new BndrunResolveContext(runModel, registry, log);
        resources = context.getMandatoryResources();
        assertEquals(1, resources.size());
        fwkResource = resources.iterator().next();
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(fwkResource));

        // Try it the other way round
        registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.0.index.xml")));

        runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework;version='[4,4.1)'");

        context = new BndrunResolveContext(runModel, registry, log);
        resources = context.getMandatoryResources();
        assertEquals(1, resources.size());
        fwkResource = resources.iterator().next();
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(fwkResource));
    }

    public static void testFrameworkCapabilitiesPreferredOverRepository() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))").buildSyntheticRequirement();

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(requirement);

        assertEquals(2, providers.size());
        assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
        assertEquals(new File("testdata/osgi.cmpn-4.3.0.jar").toURI(), findContentURI(providers.get(1).getResource()));
    }

    public static void testResolverHookFiltersResult() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

        // Add a hook that removes all capabilities from resource with id "osgi.cmpn"
        registry.addPlugin(new ResolverHook() {
            public void filterMatches(Requirement requirement, List<Capability> candidates) {
                for (Iterator<Capability> iter = candidates.iterator(); iter.hasNext();) {
                    Object id = iter.next().getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
                    if ("osgi.cmpn".equals(id))
                        iter.remove();
                }
            }
        });

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))").buildSyntheticRequirement();

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(requirement);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
        // The capability from osgi.cmpn is NOT here
    }

	public static void testResolverHookFiltersResultWithBlacklist() {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
		registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String,String> blacklistProp = new HashMap<String,String>();
		blacklistProp.put(ResolutionBlacklistFilter.BLACKLIST_PROPERTYNAME, "osgi.cmpn;version=4.3.0");
		ResolutionBlacklistFilter blacklist = new ResolutionBlacklistFilter();
		blacklist.setProperties(blacklistProp);
		registry.addPlugin(blacklist);

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter",
				"(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
				.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(1, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
				.getResource()));
	}

	public static void testResolverHookFiltersResultWithBlacklistAndVersionRange1() {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
		registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String,String> blacklistProp = new HashMap<String,String>();
		blacklistProp.put(ResolutionBlacklistFilter.BLACKLIST_PROPERTYNAME, "osgi.cmpn;version='[4.0.0,4.3.0)'");
		ResolutionBlacklistFilter blacklist = new ResolutionBlacklistFilter();
		blacklist.setProperties(blacklistProp);
		registry.addPlugin(blacklist);

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter",
				"(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
				.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(2, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
				.getResource()));
		assertEquals(new File("testdata/osgi.cmpn-4.3.0.jar").toURI(), findContentURI(providers.get(1).getResource()));
	}

	public static void testResolverHookFiltersResultWithBlacklistAndVersionRange2() {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
		registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

		// Add a hook that removes all capabilities from resource with id
		// "osgi.cmpn"
		HashMap<String,String> blacklistProp = new HashMap<String,String>();
		blacklistProp.put(ResolutionBlacklistFilter.BLACKLIST_PROPERTYNAME, "osgi.cmpn;version='[4.0.0,4.4.0)'");
		ResolutionBlacklistFilter blacklist = new ResolutionBlacklistFilter();
		blacklist.setProperties(blacklistProp);
		registry.addPlugin(blacklist);

		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework");

		Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter",
				"(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))")
				.buildSyntheticRequirement();

		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		List<Capability> providers = context.findProviders(requirement);

		assertEquals(1, providers.size());
		assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0)
				.getResource()));
	}
	
    public static void testResolverHookCannotFilterFrameworkCapabilities() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/osgi.cmpn-4.3.0.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/org.apache.felix.framework-4.0.2.index.xml")));

        // Add a hook that tries to remove all capabilities from resource with id "org.apache.felix.framework"
        registry.addPlugin(new ResolverHook() {
            public void filterMatches(Requirement requirement, List<Capability> candidates) {
                for (Iterator<Capability> iter = candidates.iterator(); iter.hasNext();) {
                    Object id = iter.next().getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
                    if ("org.apache.felix.framework".equals(id)) {
                        fail("this line should not be reached");
                    }
                }
            }
        });

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        Requirement requirement = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(&(osgi.wiring.package=org.osgi.util.tracker)(version>=1.5)(!(version>=1.6)))").buildSyntheticRequirement();

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(requirement);

        // The filter was ineffective
        assertEquals(2, providers.size());
        assertEquals(new File("testdata/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
        assertEquals(new File("testdata/osgi.cmpn-4.3.0.jar").toURI(), findContentURI(providers.get(1).getResource()));
    }

    public static void testPreferLeastRequirementsAndMostCapabilities() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo4/index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        Requirement requirement = new CapReqBuilder("x").buildSyntheticRequirement();
        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(requirement);

        assertEquals(3, providers.size());
        // x.3 has same requirements but more capabilities than x.2
        assertEquals(new File("testdata/repo4/x.3.jar").toURI(), findContentURI(providers.get(0).getResource()));
        // x.2 has same capabilities but fewer requirements than x.1
        assertEquals(new File("testdata/repo4/x.2.jar").toURI(), findContentURI(providers.get(1).getResource()));
        assertEquals(new File("testdata/repo4/x.1.jar").toURI(), findContentURI(providers.get(2).getResource()));
    }

    public static void testResolvePreferences() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo4/index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.genericSet("-resolve.preferences", "x.1");

        Requirement requirement = new CapReqBuilder("x").buildSyntheticRequirement();
        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(requirement);

        assertEquals(3, providers.size());
        assertEquals(new File("testdata/repo4/x.1.jar").toURI(), findContentURI(providers.get(0).getResource()));
        assertEquals(new File("testdata/repo4/x.3.jar").toURI(), findContentURI(providers.get(1).getResource()));
        assertEquals(new File("testdata/repo4/x.2.jar").toURI(), findContentURI(providers.get(2).getResource()));
    }

    public static void testSelfCapabilityPreferredOverRepository() {
        MockRegistry registry = new MockRegistry();
        Repository repo = createRepo(new File("testdata/repo4.index.xml"));

        registry.addPlugin(repo);

        Requirement resourceReq = new CapReqBuilder("osgi.identity").addDirective("filter", "(osgi.identity=dummy-selfcap)").buildSyntheticRequirement();
        Resource resource = repo.findProviders(Collections.singleton(resourceReq)).get(resourceReq).iterator().next().getResource();

        Requirement packageReq = resource.getRequirements("osgi.wiring.package").get(0);

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers = context.findProviders(packageReq);

        assertNotNull(providers);
        assertEquals(2, providers.size());
        assertEquals(new File("testdata/repo4/dummy.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testInputRequirementsAsMandatoryResource() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");

        Requirement req = new CapReqBuilder("osgi.identity").addDirective("filter", "(osgi.identity=org.apache.felix.gogo.command)").buildSyntheticRequirement();
        runModel.setRunRequires(Collections.singletonList(req));

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        Collection<Resource> mandRes = context.getMandatoryResources();

        assertEquals(2, mandRes.size());
        Iterator<Resource> iter = mandRes.iterator();
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(iter.next()));
        assertEquals("<<INITIAL>>", iter.next().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity"));
    }

    public static void testEERequirementResolvesFramework() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.ee").addDirective("filter", "(osgi.ee=J2SE-1.5)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testJREPackageResolvesFramework() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = CapReqBuilder.createPackageRequirement("javax.annotation", null).buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testJREPackageNotResolved() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.J2SE_1_5); // javax.annotation added in Java 6

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
        Requirement req = CapReqBuilder.createPackageRequirement("javax.annotation", null).buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(0, providers.size());
    }

    public static void testDontResolveBuildOnlyLibraries() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/buildrepo.index.xml")));

        BndEditModel runModel = new BndEditModel();
        BndrunResolveContext context;

        context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers1 = context.findProviders(CapReqBuilder.createPackageRequirement("org.osgi.framework", null).buildSyntheticRequirement());
        assertEquals(0, providers1.size());

        context = new BndrunResolveContext(runModel, registry, log);
        List<Capability> providers2 = context.findProviders(CapReqBuilder.createPackageRequirement("java.security", null).buildSyntheticRequirement());
        assertEquals(0, providers2.size());
    }

    public static void testResolveSystemBundleAlias() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.wiring.host").addDirective("filter", "(osgi.wiring.host=system.bundle)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testUnsatisfiedSystemPackage() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=sun.reflect)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(0, providers.size());
    }

    public static void testResolveSystemPackagesExtra() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);
        runModel.setSystemPackages(Collections.singletonList(new ExportedPackage("sun.reflect", new Attrs())));

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=sun.reflect)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testUnsatisfiedRequirement() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.extender").addDirective("filter", "(&(osgi.extender=foobar)(version>=1.0))").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);
        assertEquals(0, providers.size());
    }

    public static void testResolveSystemCapabilitiesExtra() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);
        runModel.genericSet("-runsystemcapabilities", "osgi.extender;osgi.extender=foobar;version:Version=1.0");

        BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);

        Requirement req = new CapReqBuilder("osgi.extender").addDirective("filter", "(&(osgi.extender=foobar)(version>=1.0))").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);

        assertEquals(1, providers.size());
        assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
    }

    public static void testMacroInSystemCapability() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel runModel = new BndEditModel();
        runModel.setRunFw("org.apache.felix.framework");
        runModel.setEE(EE.JavaSE_1_6);
        runModel.genericSet("-runsystemcapabilities", "${native_capability}");

        String origOsName = System.getProperty("os.name");
        String origOsVersion = System.getProperty("os.version");
        String origOsArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.version", "10.8.2");
            System.setProperty("os.arch", "x86_64");

            BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
            Requirement req = new CapReqBuilder("osgi.native").addDirective("filter", "(osgi.native.osname=MacOSX)").buildSyntheticRequirement();

            List<Capability> providers = context.findProviders(req);
            assertEquals(1, providers.size());
            assertEquals(new File("testdata/repo3/org.apache.felix.framework-4.0.2.jar").toURI(), findContentURI(providers.get(0).getResource()));
        }
        finally {
            System.setProperty("os.name", origOsName);
            System.setProperty("os.version", origOsVersion);
            System.setProperty("os.arch", origOsArch);
        }

    }

}
