package biz.aQute.resolve.internal;

import static test.lib.Utils.createRepo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.resolver.ResolverImpl;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import test.lib.MockRegistry;
import test.lib.NullLogService;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class ResolveTest extends TestCase {

    private static final LogService log = new NullLogService();

    public static void testSimpleResolve() {

        MockRegistry registry = new MockRegistry();
        registry.addPlugin(createRepo(new File("testdata/repo3.index.xml")));

        BndEditModel model = new BndEditModel();

        model.setRunFw("org.apache.felix.framework");

        List<Requirement> requires = new ArrayList<Requirement>();
        CapReqBuilder capReq = CapReqBuilder.createBundleRequirement("org.apache.felix.gogo.shell", "[0,1)");
        requires.add(capReq.buildSyntheticRequirement());

        model.setRunRequires(requires);
        BndrunResolveContext context = new BndrunResolveContext(model, registry, log);

        Resolver resolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));

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
}
