package test;

import static test.lib.Utils.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;

import test.lib.MockRegistry;
import test.lib.NullLogService;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.resource.CapReqBuilder;
import biz.aQute.resolve.internal.BndrunResolveContext;

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

}
