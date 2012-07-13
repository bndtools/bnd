package biz.aQute.resolve.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.model.EE;
import aQute.bnd.header.Parameters;
import aQute.lib.deployer.repository.CapabilityIndex;
import aQute.lib.deployer.repository.MapToDictionaryAdapter;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class FrameworkResourceRepository implements Repository {

    private final CapabilityIndex capIndex = new CapabilityIndex();
    private final Resource framework;
    private final EE ee;

    public FrameworkResourceRepository(Resource frameworkResource, EE ee) {
        this.framework = frameworkResource;
        this.ee = ee;
        capIndex.addResource(frameworkResource);

        // Add EEs
        capIndex.addCapability(createEECapability(ee));
        for (EE compat : ee.getCompatible()) {
            capIndex.addCapability(createEECapability(compat));
        }

        // Add JRE packages
        loadJREPackages();
    }

    public void addFrameworkCapability(CapReqBuilder builder) {
        Capability cap = builder.setResource(framework).buildCapability();
        capIndex.addCapability(cap);
    }

    private Capability createEECapability(EE ee) {
        CapReqBuilder builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
        builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getEEName());
        builder.setResource(framework);
        return builder.buildCapability();
    }

    private void loadJREPackages() {
        InputStream stream = FrameworkResourceRepository.class.getResourceAsStream(ee.name() + ".properties");
        if (stream != null) {
            try {
                Properties properties = new Properties();
                properties.load(stream);

                Parameters params = new Parameters(properties.getProperty("org.osgi.framework.system.packages", ""));
                for (String packageName : params.keySet()) {
                    CapReqBuilder builder = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
                    builder.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, packageName);
                    builder.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(0, 0, 0));
                    Capability cap = builder.setResource(framework).buildCapability();
                    capIndex.addCapability(cap);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error loading JRE package properties", e);
            }
        }
    }

    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
        for (Requirement requirement : requirements) {
            List<Capability> matches = new LinkedList<Capability>();
            result.put(requirement, matches);

            capIndex.appendMatchingCapabilities(requirement, matches);
        }
        return result;
    }

}
