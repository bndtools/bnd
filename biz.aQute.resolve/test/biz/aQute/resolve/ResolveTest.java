package biz.aQute.resolve;

import static test.lib.Utils.createRepo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import junit.framework.TestCase;
import test.lib.MockRegistry;

@SuppressWarnings({
		"restriction", "deprecation"
})
public class ResolveTest extends TestCase {

	private static final LogService log = new LogReporter(new ReporterAdapter(System.out));

	/**
	 * Observed at the OSGi Community Event. When specifying the following:
	 * -resolve.effective: active;skip:="osgi.service" OSGi service capabilities
	 * are skipped but other active-time requirements are used. This is correct,
	 * but it only works if the property is defined directly inside the bndrun
	 * being resolved. If the property is inherited from a parent bndrun then it
	 * no longer applies...
	 * <p>
	 * Tried this but seems to work in all combinations as expected
	 */
	public void testIncludeBndrun() throws Exception {
		assertInclude("intop.bndrun", "top");
		assertInclude("ininclude.bndrun", "include");
		assertInclude("inworkspace.bndrun", "workspace");
	}

	/*
	 * Create a BndrunResolveContext with a skip of 'workspace' in the
	 * workspace, and then use different files that get -resolve.effective from
	 * the include file or the bndrun file.
	 */
	private void assertInclude(String file, String value) throws Exception {
		LogService log = Mockito.mock(LogService.class);
		File f = IO.getFile("testdata/resolve/includebndrun/" + file);
		File wsf = IO.getFile("testdata/ws");

		Workspace ws = new Workspace(wsf);
		ws.setProperty("-resolve.effective", "active;skip:='workspace'");

		Run run = Run.createRun(ws, f);
		BndrunResolveContext context = new BndrunResolveContext(run, run, ws, log);
		context.init();
		Map<String,Set<String>> effectiveSet = context.getEffectiveSet();
		assertNotNull(effectiveSet.get("active"));
		assertTrue(effectiveSet.get("active").contains(value));
	}

	/**
	 * Missing default version
	 */

	public void testDefaultVersionsForJava() throws Exception {
		Run run = Run.createRun(null, IO.getFile("testdata/defltversions/run.bndrun"));
		try (ProjectResolver pr = new ProjectResolver(run);) {
			Map<Resource,List<Wire>> resolve = pr.resolve();
			assertTrue(pr.check());
			assertNotNull(resolve);
			assertTrue(resolve.size() > 0);
			System.out.println(resolve);
		}
	}

	/**
	 * The enRoute base guard resolved but is missing bundles, the runbundles do
	 * not run
	 */

	public void testenRouteGuard() throws Exception {
		MockRegistry registry = new MockRegistry();
		Repository repo = createRepo(IO.getFile("testdata/enroute/index.xml"), getName());
		registry.addPlugin(repo);

		List<Requirement> reqs = CapReqBuilder.getRequirementsFrom(
				new Parameters("osgi.wiring.package;filter:='(osgi.wiring.package=org.osgi.service.async)'"));
		Collection<Capability> pack = repo.findProviders(reqs).get(reqs.get(0));
		assertEquals(2, pack.size());

		ResourceBuilder b = new ResourceBuilder();
		File guard = IO.getFile("testdata/enroute/osgi.enroute.base.guard.jar");
		Domain manifest = Domain.domain(guard);
		b.addManifest(manifest);
		Repository resourceRepository = new ResourcesRepository(b.build());
		registry.addPlugin(resourceRepository);

		Processor model = new Processor();
		model.setRunfw("org.eclipse.osgi");
		model.setRunblacklist(
				"osgi.identity;filter:='(osgi.identity=osgi.enroute.base.api)',osgi.identity;filter:='(osgi.identity=osgi.cmpn)',osgi.identity;filter:='(osgi.identity=osgi.core)");
		model.setRunRequires("osgi.identity;filter:='(osgi.identity=osgi.enroute.base.guard)'");
		model.setRunee("JavaSE-1.8");
		try (ResolverLogger logger = new ResolverLogger(4)) {
			BndrunResolveContext context = new BndrunResolveContext(model, null, registry, log);
			Resolver resolver = new BndResolver(logger);

			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
		} catch (ResolutionException e) {
			String msg = e.getMessage().replaceAll("\\[caused by:", "\n->");
			System.out.println(msg);
			fail(msg);
		}

	}

	/**
	 * Test if we can augment
	 * 
	 * @throws Exception
	 */

	public void testResolveWithAugments() throws Exception {
		// Add requirement
		assertAugmentResolve(
				"org.apache.felix.gogo.shell;capability:='foo;foo=gogo';requirement:='foo;filter:=\"(foo=*)\"'",
				"foo;filter:='(foo=gogo)'", null);

		// Default effective
		assertAugmentResolve("org.apache.felix.gogo.shell;capability:='foo;foo=gogo'", "foo;filter:='(foo=*)'", null);

		// Wildcard name
		assertAugmentResolve("*.shell;capability:='foo;foo=gogo'", "foo;filter:='(foo=gogo)'", null);
		assertAugmentResolve("org.apache.felix.gogo.*;capability:='foo;foo=gogo'", "foo;filter:='(foo=*)'", null);
		assertAugmentResolveFails("gogo.*;capability:='foo;foo=gogo'", "foo;filter:='(foo=*)'", null);

		// Version range
		assertAugmentResolve("org.apache.felix.gogo.*;version='[0,1)';capability:='foo;foo=gogo'",
				"foo;filter:='(foo=*)'", null);

		assertAugmentResolveFails("org.apache.felix.gogo.*;version='[1,2)';capability:='foo;foo=gogo'",
				"foo;filter:='(foo=*)'", null);

		// Effective
		assertAugmentResolve("org.apache.felix.gogo.shell;capability:='foo;foo=gogo;effective:=foo'",
				"foo;filter:='(foo=gogo)';effective:=foo", "foo");
		assertAugmentResolveFails("org.apache.felix.gogo.shell;capability:='foo;foo=gogo;effective:=bar'",
				"foo;filter:='(foo=*)';effective:=foo", "foo");

	}

	private void assertAugmentResolveFails(String augment, String require, String effective) throws Exception {
		try {
			assertAugmentResolve(augment, require, effective);
			fail("Failed to fail augment=" + augment + ", require=" + require + ", effective=" + effective);
		} catch (AssertionError | ResolutionException e) {
			// Yup, expected
		}
	}

	private void assertAugmentResolve(String augment, String require, String effective) throws Exception {

		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getName()));

		Processor model = new Processor();
		model.setRunfw("org.apache.felix.framework");
		model.setProperty("-augment", augment);
		model.setRunRequires(require);
		if (effective != null)
			model.setProperty("-resolve.effective", effective);

		BndrunResolveContext context = new BndrunResolveContext(model, null, registry, log);
		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(logger);

			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.10");
			assertNotNull(resource);
		}
	}

	/**
	 * Test minimal setup
	 * 
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public void testMinimalSetup() throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Map<String,String> map = new HashMap<>();
			map.put("locations", IO.getFile("testdata/repo3.index.xml").toURI().toString());
			map.put("name", getName());
			map.put("cache", new File("generated/tmp/test/cache/" + getName()).getAbsolutePath());
			repo.setProperties(map);
			Processor model = new Processor();
			model.addBasicPlugin(httpClient);
			repo.setRegistry(model);


			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.shell)'");
			BndrunResolveContext context = new BndrunResolveContext(model, null, model, log);
			context.setLevel(0);
			context.addRepository(repo);
			context.init();
			try (ResolverLogger logger = new ResolverLogger(4)) {
				Resolver resolver = new BndResolver(logger);
				Map<Resource,List<Wire>> resolved = resolver.resolve(context);
				Set<Resource> resources = resolved.keySet();
				Resource shell = getResource(resources, "org.apache.felix.gogo.shell", "0.10.0");
				assertNotNull(shell);
			} catch (ResolutionException e) {
				e.printStackTrace();
				fail("Resolve failed");
			}
		}
	}

	/**
	 * Test if we can resolve with a distro
	 * 
	 * @throws ResolutionException
	 */
	public void testResolveWithDistro() throws Exception {

		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getName()));

		BndEditModel model = new BndEditModel();
		model.setDistro(Arrays.asList("testdata/distro.jar;version=file"));
		List<Requirement> requires = new ArrayList<>();
		CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
		requires.add(capReq.buildSyntheticRequirement());

		model.setRunRequires(requires);
		BndrunResolveContext context = new BndrunResolveContext(model, registry, log);
		context.setLevel(0);
		context.init();
		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(logger);

			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource shell = getResource(resources, "org.apache.felix.gogo.shell", "0.10.0");
			assertNotNull(shell);
		}
	}

	/**
	 * This is a basic test of resolving. This test is paired with
	 * {@link #testResolveWithDistro()}. If you change the resources, make sure
	 * this is done in the same way. The {@link #testResolveWithDistro()} has a
	 * negative check while this one checks positive.
	 */
	public void testSimpleResolve() throws Exception {

		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml"), getName()));

		BndEditModel model = new BndEditModel();

		model.setRunFw("org.apache.felix.framework");

		List<Requirement> requires = new ArrayList<>();
		CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
		requires.add(capReq.buildSyntheticRequirement());

		model.setRunRequires(requires);
		BndrunResolveContext context = new BndrunResolveContext(model, registry, log);

		try (ResolverLogger logger = new ResolverLogger()) {
			Resolver resolver = new BndResolver(logger);
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.10");
			assertNotNull(resource);
		} catch (ResolutionException e) {
			fail("Resolve failed");
		}
	}

	/**
	 * Check if we can resolve against capabilities defined on the -provided
	 */
	// public static void testResolveWithProfile() throws Exception {
	// Resolver resolver = new BndResolver(new ResolverLogger(4));
	// MockRegistry registry = new MockRegistry();
	// registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml")));
	// BndEditModel model = new BndEditModel();
	//
	// //
	// // Provided capabilities
	// //
	//
	// model.setRunFw("org.apache.felix.framework");
	// model.setGenericString(Constants.DISTRO,
	// "testdata/repo1/org.apache.felix.gogo.runtime-0.10.0.jar;version=file");
	//
	// //
	// // We require gogo, but now the gogo runtime is on the runpath
	// // so should be excluded
	// //
	//
	// Requirement erq =
	// CapReqBuilder.createPackageRequirement("org.apache.felix.gogo.api",
	// "0.10.0")
	// .buildSyntheticRequirement();
	//
	// model.setRunRequires(Arrays.asList(erq));
	//
	// BndrunResolveContext context = new BndrunResolveContext(model, registry,
	// log);
	// context.setLevel(4);
	// context.init();
	//
	// Map<Resource,List<Wire>> resolved = resolver.resolve(context);
	// List<Wire> wires = resolved.get(context.getInputResource());
	// assertNotNull(wires);
	// boolean found = false;
	// for ( Wire wire : wires ) {
	// if (equals(wire.getRequirement(), erq)) {
	// found = true;
	// assertTrue(wire.getProvider().equals(context.getSystemResource()));
	// }
	// }
	// assertTrue("System resource not found for wire", found);
	//
	// Set<Resource> resources = resolved.keySet();
	// Resource resource = getResource(resources,
	// "org.apache.felix.gogo.runtime", "0.10");
	// assertNull(resource);
	//
	// }

	// private static boolean equals(Requirement a, Requirement b) {
	// if ( a== b)
	// return true;
	//
	// if ( a == null || b == null)
	// return false;
	//
	// if ( a.equals(b))
	// return true;
	//
	// if ( !a.getNamespace().equals(b.getNamespace()))
	// return false;
	//
	// return a.getDirectives().equals(b.getDirectives()) &&
	// a.getAttributes().equals(b.getAttributes());
	// }

	private static Resource getResource(Set<Resource> resources, String bsn, String versionString) {
		for (Resource resource : resources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			if (identities != null && identities.size() == 1) {
				Capability idCap = identities.get(0);
				Object id = idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
				Object version = idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (bsn.equals(id)) {
					if (versionString == null) {
						return resource;
					}
					Version requested = Version.parseVersion(versionString);
					Version current;
					if (version instanceof Version) {
						current = (Version) version;
					} else {
						current = Version.parseVersion((String) version);
					}
					if (requested.equals(current)) {
						return resource;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Simple test that resolves a requirement
	 * 
	 * @throws ResolutionException
	 */
	public void testMultipleOptionsNotDuplicated() throws Exception {

		// Resolve against repo 5
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo5/index.xml"), getName()));

		// Set up a simple Java 7 Felix requirement as per Issue #971
		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework;version='4.2.1'");
		runModel.setEE(EE.JavaSE_1_7);
		runModel.setSystemPackages(Collections.singletonList(new ExportedPackage("org.w3c.dom.traversal", null)));
		runModel.setGenericString("-resolve.effective", "active");

		// Require the log service, GoGo shell and GoGo commands
		List<Requirement> requirements = new ArrayList<>();

		requirements.add(new CapReqBuilder("osgi.identity")
				.addDirective("filter", "(osgi.identity=org.apache.felix.log)").buildSyntheticRequirement());
		requirements.add(new CapReqBuilder("osgi.identity")
				.addDirective("filter", "(osgi.identity=org.apache.felix.gogo.shell)").buildSyntheticRequirement());
		requirements.add(new CapReqBuilder("osgi.identity")
				.addDirective("filter", "(osgi.identity=org.apache.felix.gogo.command)").buildSyntheticRequirement());

		runModel.setRunRequires(requirements);

		// Resolve the bndrun
		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		Resolver resolver = new BndResolver(new org.apache.felix.resolver.Logger(4));
		Collection<Resource> resolvedResources = new ResolveProcess()
				.resolveRequired(runModel, registry, resolver, Collections.<ResolutionCallback> emptyList(), log)
				.keySet();

		Map<String,Resource> mandatoryResourcesBySymbolicName = new HashMap<>();
		for (Resource r : resolvedResources) {
			Capability cap = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
			// We shouldn't have more than one match for each symbolic name for
			// this resolve
			String symbolicName = (String) cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
			assertNull("Multiple results for " + symbolicName, mandatoryResourcesBySymbolicName.put(symbolicName, r));
		}
		assertEquals(4, resolvedResources.size());
	}

}
