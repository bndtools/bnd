package test;

import static test.Utils.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.BndEditModel;
import aQute.lib.osgi.resource.CapReqBuilder;
import biz.aQute.resolve.BndrunResolveContext;

public class BndrunResolveContextTest extends TestCase {

    public void testEffective() {
        BndrunResolveContext context = new BndrunResolveContext(new BndEditModel(), new MockRegistry());

        Requirement resolveReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_RESOLVE).buildSyntheticRequirement();
        Requirement activeReq = new CapReqBuilder("dummy.ns").addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE).buildSyntheticRequirement();
        Requirement noEffectiveDirectiveReq = new CapReqBuilder("dummy.ns").buildSyntheticRequirement();

        assertTrue(context.isEffective(resolveReq));
        assertFalse(context.isEffective(activeReq));
        assertTrue(context.isEffective(noEffectiveDirectiveReq));
    }

    public void testEmptyInitialWirings() {
        assertEquals(0, new BndrunResolveContext(new BndEditModel(), new MockRegistry()).getWirings().size());
    }

    public void testBasicFindProviders() {
        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml")));

        BndEditModel runModel = new BndEditModel();
        BndrunResolveContext context = new BndrunResolveContext(runModel, registry);

        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)").buildSyntheticRequirement();
        List<Capability> providers = context.findProviders(req);
        assertEquals(1, providers.size());
        Resource resource = providers.get(0).getResource();

        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

    public void testProviderPreference() {
        Requirement req = new CapReqBuilder("osgi.wiring.package").addDirective("filter", "(osgi.wiring.package=org.apache.felix.gogo.api)").buildSyntheticRequirement();

        MockRegistry registry;
        BndrunResolveContext context;
        List<Capability> providers;
        Resource resource;

        // First try it with repo1 first
        registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo1.index.xml")));
        registry.addPlugin(createRepo(new File("testdata/repo2.index.xml")));

        context = new BndrunResolveContext(new BndEditModel(), registry);
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

        context = new BndrunResolveContext(new BndEditModel(), registry);
        providers = context.findProviders(req);
        assertEquals(2, providers.size());
        resource = providers.get(0).getResource();
        assertEquals(new File("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
        resource = providers.get(1).getResource();
        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

    public void testReorderRepositories() {
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

        context = new BndrunResolveContext(runModel, registry);
        providers = context.findProviders(req);
        assertEquals(2, providers.size());
        resource = providers.get(0).getResource();
        assertEquals(new File("testdata/repo2/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
        resource = providers.get(1).getResource();
        assertEquals(new File("testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar").toURI(), findContentURI(resource));
    }

}
