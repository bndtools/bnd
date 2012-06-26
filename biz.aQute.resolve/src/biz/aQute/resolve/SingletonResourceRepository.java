package biz.aQute.resolve;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.lib.deployer.repository.CapabilityIndex;

public class SingletonResourceRepository implements Repository {

    private final CapabilityIndex capIndex = new CapabilityIndex();

    public SingletonResourceRepository(Resource resource) {
        capIndex.addResource(resource);
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
