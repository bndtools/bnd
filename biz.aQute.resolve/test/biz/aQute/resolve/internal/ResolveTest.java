package biz.aQute.resolve.internal;

import static test.lib.Utils.*;

import java.util.*;

import junit.framework.*;

import org.apache.felix.resolver.*;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.service.log.*;
import org.osgi.service.resolver.*;

import test.lib.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.osgi.resource.*;
import aQute.lib.io.*;
import biz.aQute.resolve.*;

@SuppressWarnings("restriction")
public class ResolveTest extends TestCase {

    private static final LogService log = new NullLogService();

    public static void testSimpleResolve() {

        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(IO.getFile("testdata/repo3.index.xml")));

        BndEditModel model = new BndEditModel();

        model.setRunFw("org.apache.felix.framework");

        List<Requirement> requires = new ArrayList<Requirement>();
        CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
        requires.add(capReq.buildSyntheticRequirement());

        model.setRunRequires(requires);
        BndrunResolveContext context = new BndrunResolveContext(model, registry, log);

        Resolver resolver = new BndResolver(new ResolverLogger(4));

        try {
            Map<Resource,List<Wire>> resolved = resolver.resolve(context);
            Set<Resource> resources = resolved.keySet();
            Resource resource = getResource(resources, "org.apache.felix.gogo.runtime", "0.10");
            assertNotNull(resource);
        } catch (ResolutionException e) {
            fail("Resolve failed");
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

	/**
	 * Simple test that resolves a requirement
	 * 
	 * @throws ResolutionException
	 */
	public static void testMultipleOptionsNotDuplicated() throws ResolutionException {

		// Resolve against repo 5
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(createRepo(IO.getFile("testdata/repo5/index.xml"), "Test-5"));

		// Set up a simple Java 7 Felix requirement as per Issue #971
		BndEditModel runModel = new BndEditModel();
		runModel.setRunFw("org.apache.felix.framework;version='4.2.1'");
		runModel.setEE(EE.JavaSE_1_7);
		runModel.setSystemPackages(Collections.singletonList(new ExportedPackage("org.w3c.dom.traversal", null)));
		runModel.setGenericString("-resolve.effective", "active");

		// Require the log service, GoGo shell and GoGo commands
		List<Requirement> requirements = new ArrayList<Requirement>();

		requirements.add(new CapReqBuilder("osgi.identity").addDirective("filter",
				"(osgi.identity=org.apache.felix.log)").buildSyntheticRequirement());
		requirements.add(new CapReqBuilder("osgi.identity").addDirective("filter",
				"(osgi.identity=org.apache.felix.gogo.shell)").buildSyntheticRequirement());
		requirements.add(new CapReqBuilder("osgi.identity").addDirective("filter",
				"(osgi.identity=org.apache.felix.gogo.command)").buildSyntheticRequirement());

		runModel.setRunRequires(requirements);

		// Resolve the bndrun
		BndrunResolveContext context = new BndrunResolveContext(runModel, registry, log);
		Resolver resolver = new ResolverImpl(null);
		Collection<Resource> resolvedResources = new ResolveProcess().resolveRequired(runModel, registry, resolver,
				Collections.<ResolutionCallback> emptyList(), log).keySet();

		Map<String,Resource> mandatoryResourcesBySymbolicName = new HashMap<String,Resource>();
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
