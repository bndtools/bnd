package biz.aQute.resolve.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.repository.InfoRepository;
import aQute.lib.io.IO;
import biz.aQute.resolve.BndResolver;
import biz.aQute.resolve.BndrunResolveContext;
import biz.aQute.resolve.ResolveProcess;
import junit.framework.TestCase;
import test.lib.NullLogService;

@SuppressWarnings({
		"restriction", "deprecation"
})
public class JpmRepoTest extends TestCase {
	File							tmp;
	private Workspace				ws;
	private static final LogService	log	= new NullLogService();

	public void setUp() throws Exception {
		tmp = new File("generated/tmp/test/" + getName());
		tmp.mkdirs();
		IO.copy(IO.getFile("testdata/ws"), tmp);
		ws = new Workspace(tmp);
		assertTrue(ws.check());
	}

	public void tearDown() throws Exception {
		ws.close();
		IO.delete(tmp);
	}

	public void testWrapper() throws Exception {
		ws.setProperty("-fixupmessages.jpmdeprecated", "aQute.bnd.jpm.Repository is deprecated");
		assertNotNull(ws.getRepositories());
		assertNotNull(ws.getPlugin(InfoRepository.class));
		assertNotNull(ws.getPlugin(Repository.class));
		assertTrue(ws.check());
		assertEquals(2, ws.getRepositories().size()); // cache + jpm

		InfoRepository plugin = ws.getPlugin(InfoRepository.class);
		for (String bsn : plugin.list(null)) {
			for (aQute.bnd.version.Version version : plugin.versions(bsn)) {
				System.out.println(bsn + ";version=" + version);
			}
		}

	}

	public void testScr() {
		Repository repo = ws.getPlugin(Repository.class);

		BndEditModel model = new BndEditModel();
		model.setRunFw("org.apache.felix.framework");

		List<Requirement> requires = new ArrayList<Requirement>();
		CapReqBuilder capReq = CapReqBuilder.createSimpleRequirement("osgi.extender", "osgi.component", "[1.1,2)");
		requires.add(capReq.buildSyntheticRequirement());

		Map<Requirement,Collection<Capability>> shell = repo.findProviders(requires);
		assertNotNull(shell);
		assertEquals(1, shell.size());
	}

	// public void testResolveProviderWithRunpath() throws Exception {
	// try {
	// Project provider = ws.getProject("provider");
	// provider.build();
	// assertTrue(provider.check());
	//
	// Project requirer = ws.getProject("requirer");
	// requirer.build();
	// assertTrue(requirer.check());
	//
	// BndEditModel model = new BndEditModel();
	// model.setProject(requirer);
	// BndrunResolveContext context = new BndrunResolveContext(model, ws, log);
	//
	// Resolver resolver = new ResolverImpl(new
	// org.apache.felix.resolver.Logger(4), null);
	//
	// Map<Resource,List<Wire>> resolved = resolver.resolve(context);
	// Set<Resource> resources = resolved.keySet();
	// Resource resource = getResource(resources, "requirer", "0");
	// assertNotNull(resource);
	// }
	// catch (ResolutionException e) {
	// fail("Resolve failed " + e);
	// }
	// }

	public void testSimpleResolve() {
		Repository repo = ws.getPlugin(Repository.class);

		BndEditModel model = new BndEditModel();
		model.setRunFw("org.apache.felix.framework");

		List<Requirement> requires = new ArrayList<Requirement>();
		CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
		requires.add(capReq.buildSyntheticRequirement());

		Map<Requirement,Collection<Capability>> shell = repo.findProviders(requires);
		assertNotNull(shell);
		assertEquals(1, shell.size());

		model.setRunRequires(requires);
		BndrunResolveContext context = new BndrunResolveContext(model, ws, log);

		Resolver resolver = new BndResolver(new org.apache.felix.resolver.Logger(4));

		try {
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.12");
			assertNotNull(resource);
		} catch (ResolutionException e) {
			fail("Resolve failed");
		}
	}

	public void testUnresolved() throws ResolutionException {
		Repository repo = ws.getPlugin(Repository.class);

		BndEditModel model = new BndEditModel();
		model.setRunFw("org.apache.felix.framework");

		List<Requirement> requires = new ArrayList<Requirement>();
		CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.webconsole", "[4,5)");
		requires.add(capReq.buildSyntheticRequirement());

		Map<Requirement,Collection<Capability>> shell = repo.findProviders(requires);
		assertNotNull(shell);
		assertEquals(1, shell.size());

		model.setRunRequires(requires);
		BndrunResolveContext context = new BndrunResolveContext(model, ws, log);

		Resolver resolver = new BndResolver(new org.apache.felix.resolver.Logger(4));

		try {
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			fail("Resolve did not fail");
		} catch (ResolutionException e) {
			assertEquals(1, e.getUnresolvedRequirements().size());
			ResolutionException augmented = ResolveProcess.augment(context, e);
			assertEquals(2, augmented.getUnresolvedRequirements().size());

		}
	}

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

}
