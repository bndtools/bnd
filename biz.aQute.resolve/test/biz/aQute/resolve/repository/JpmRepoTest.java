package biz.aQute.resolve.repository;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.felix.resolver.*;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;
import org.osgi.service.resolver.*;

import test.lib.*;
import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.repository.*;
import aQute.lib.io.*;
import biz.aQute.resolve.*;
import biz.aQute.resolve.internal.*;

public class JpmRepoTest extends TestCase {
	File							tmp	= new File("tmp");
	private Workspace				ws;
	private static final LogService	log	= new NullLogService();

	public void setUp() throws Exception {
		tmp.mkdirs();
		IO.delete(tmp);
		tmp.mkdirs();
		IO.copy(IO.getFile("testdata/ws"), tmp);
		ws = new Workspace(tmp);
		assertTrue(ws.check());
	}

	public void tearDown() throws Exception {
		IO.delete(tmp);
	}

	public void testWrapper() throws Exception {
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

//	public void testResolveProviderWithRunpath() throws Exception {
//		try {
//			Project provider = ws.getProject("provider");
//			provider.build();
//			assertTrue(provider.check());
//
//			Project requirer = ws.getProject("requirer");
//			requirer.build();
//			assertTrue(requirer.check());
//
//			BndEditModel model = new BndEditModel();
//			model.setProject(requirer);
//			BndrunResolveContext context = new BndrunResolveContext(model, ws, log);
//
//			Resolver resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));
//
//			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
//			Set<Resource> resources = resolved.keySet();
//			Resource resource = getResource(resources, "requirer", "0");
//			assertNotNull(resource);
//		}
//		catch (ResolutionException e) {
//			fail("Resolve failed " + e);
//		}
//	}

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

		Resolver resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));

		try {
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.12");
			assertNotNull(resource);
		}
		catch (ResolutionException e) {
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

		Resolver resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));

		try {
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			fail("Resolve did not fail");
		}
		catch (ResolutionException e) {
			assertTrue(e.getUnresolvedRequirements().size() == 1);
			ResolutionException augmented = ResolveProcess.augment(new BndrunResolveContext(model, ws, log), e);
			assertTrue(augmented.getUnresolvedRequirements().size() == 2);

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
