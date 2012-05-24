package bndtools.model.obr;

import java.util.Collection;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;

public class PotentialMatch {

    private Requirement req;
    private Collection<Resource> resources;

    PotentialMatch(Requirement req, Collection<Resource> resources) {
        this.req = req;
        this.resources = resources;
    }

    public Requirement getRequirement() {
        return req;
    }

    public Collection<Resource> getResources() {
        return resources;
    }

}
