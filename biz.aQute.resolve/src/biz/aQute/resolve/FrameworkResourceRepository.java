package biz.aQute.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.model.EE;
import aQute.lib.deployer.repository.CapabilityIndex;
import aQute.lib.deployer.repository.MapToDictionaryAdapter;
import aQute.lib.osgi.resource.CapReqBuilder;

public class FrameworkResourceRepository implements Repository {

    private final CapabilityIndex capIndex = new CapabilityIndex();
    private final List<Capability> eeCaps = new ArrayList<Capability>(EE.values().length);

    public FrameworkResourceRepository(Resource resource, EE ee) {
        capIndex.addResource(resource);

        eeCaps.add(createEECapability(resource, ee));
        for (EE compat : ee.getCompatible()) {
            eeCaps.add(createEECapability(resource, compat));
        }
    }

    private Capability createEECapability(Resource resource, EE ee) {
        CapReqBuilder builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
        builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getEEName());
        builder.setResource(resource);
        return builder.buildCapability();
    }

    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
        for (Requirement requirement : requirements) {
            List<Capability> matches = new LinkedList<Capability>();
            result.put(requirement, matches);

            appendEEMatches(requirement, matches);
            capIndex.appendMatchingCapabilities(requirement, matches);
        }
        return result;
    }

    private void appendEEMatches(Requirement requirement, Collection< ? super Capability> result) {
        if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(requirement.getNamespace())) {
            try {
                String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
                Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

                for (Capability eeCap : eeCaps) {
                    boolean match;
                    if (filter == null)
                        match = true;
                    else
                        match = filter.match(new MapToDictionaryAdapter(eeCap.getAttributes()));

                    if (match)
                        result.add(eeCap);
                }
            } catch (InvalidSyntaxException e) {
                // Assume not matches
            }
        }
    }

}
