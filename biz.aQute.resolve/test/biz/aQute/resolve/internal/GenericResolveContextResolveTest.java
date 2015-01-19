package biz.aQute.resolve.internal;

import static test.lib.Utils.*;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;
import org.osgi.service.resolver.*;

import test.lib.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.resource.*;
import aQute.lib.io.*;
import biz.aQute.resolve.*;

public class GenericResolveContextResolveTest extends TestCase {

	private static final LogService	log	= new NullLogService();

	public static void testSimpleResolve() {

		Repository repository = createRepo(IO.getFile("testdata/repo3.index.xml"));
		GenericResolveContext grc = new GenericResolveContext(log);
		Resource framework = grc.getFrameworkResource(Arrays.asList(repository), "org.apache.felix.framework", null);

		List<Requirement> systemRequirements = new ArrayList<Requirement>();
		List<Capability> systemCapabilities = new ArrayList<Capability>();

		systemRequirements.addAll(framework.getRequirements(null));
		systemCapabilities.addAll(framework.getCapabilities(null));

		systemCapabilities.addAll(GenericResolveContext.getEECapabilities(EE.JavaSE_1_7));

		GenericResolveContext context = new GenericResolveContext(systemCapabilities, systemRequirements, log);

		context.addRepository(repository);

		Requirement requirement = GenericResolveContext.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
		context.addInputRequirement(GenericResolveContext.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)"));

		Resolver resolver = new BndResolver(new ResolverLogger(4));

		try {
			Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			Set<Resource> resources = resolved.keySet();
			Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.10");
			assertNotNull(resource);
		}
		catch (ResolutionException e) {
			fail("Resolve failed");
		}
	}

	public static void testResolveRequirementNoDirective() {

		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"));
		GenericResolveContext grc = new GenericResolveContext(log);

		GenericResolveContext context = new GenericResolveContext(Collections.<Capability> emptyList(),
				Collections.<Requirement> emptyList(), log);

		context.addRepository(repository);

		List<Capability> providers = context.findProviders(new CapReqBuilder("osgi.service").addDirective("filter",
				"(objectClass=org.osgi.service.log.LogService)").buildSyntheticRequirement());

		assertEquals(2, providers.size());

		Set<String> resourceNames = new HashSet<String>();
		for(Capability cap : providers) {
			resourceNames.add(cap.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
					.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString());
		}
		
		Set<String> expectedResourceNames = new HashSet<String>(Arrays.asList("test.a", "test.b"));
		
		assertEquals(expectedResourceNames, resourceNames);
	}

	public static void testResolveRequirementResolveDirective() {

		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"));
		GenericResolveContext grc = new GenericResolveContext(log);

		GenericResolveContext context = new GenericResolveContext(Collections.<Capability> emptyList(),
				Collections.<Requirement> emptyList(), log);

		context.addRepository(repository);

		List<Capability> providers = context.findProviders(new CapReqBuilder("osgi.service")
				.addDirective("filter", "(objectClass=org.osgi.service.log.LogService)")
				.addDirective("effective", "resolve").buildSyntheticRequirement());

		assertEquals(2, providers.size());

		Set<String> resourceNames = new HashSet<String>();
		for (Capability cap : providers) {
			resourceNames.add(cap.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0)
					.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString());
		}

		Set<String> expectedResourceNames = new HashSet<String>(Arrays.asList("test.a", "test.b"));

		assertEquals(expectedResourceNames, resourceNames);
	}

	public static void testResolveRequirementActiveDirective() {

		Repository repository = createRepo(IO.getFile("testdata/repo6/index.xml"));
		GenericResolveContext grc = new GenericResolveContext(log);

		GenericResolveContext context = new GenericResolveContext(Collections.<Capability> emptyList(),
				Collections.<Requirement> emptyList(), log);

		context.addRepository(repository);

		List<Capability> providers = context.findProviders(new CapReqBuilder("osgi.service")
				.addDirective("filter", "(objectClass=org.osgi.service.log.LogService)")
				.addDirective("effective", "active").buildSyntheticRequirement());

		assertEquals(3, providers.size());

		Set<String> resourceNames = new HashSet<String>();
		for (Capability cap : providers) {
			resourceNames.add(cap.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0)
					.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString());
		}

		Set<String> expectedResourceNames = new HashSet<String>(Arrays.asList("test.a", "test.b", "test.c"));

		assertEquals(expectedResourceNames, resourceNames);
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
