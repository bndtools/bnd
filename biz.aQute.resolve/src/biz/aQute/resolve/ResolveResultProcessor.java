package biz.aQute.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class ResolveResultProcessor {

    private BndrunResolveContext context;
    private Map<Resource,List<Wire>> wirings;

    public ResolveResultProcessor(BndrunResolveContext context, Map<Resource,List<Wire>> wirings) {
        this.context = context;
        this.wirings = wirings;
    }

    public List<Resource> getMandatory() {
        List<Resource> resources = new ArrayList<Resource>(wirings.size());
        for (Entry<Resource,List<Wire>> entry : wirings.entrySet()) {
            Resource resource = entry.getKey();

            if (resource != context.inputRequirementsResource && resource != context.frameworkResource) {
                resources.add(resource);
            }
        }
        return resources;
    }
}
