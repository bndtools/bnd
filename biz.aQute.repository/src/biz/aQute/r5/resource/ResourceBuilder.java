package biz.aQute.r5.resource;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ResourceBuilder {

	private final ResourceImpl					resource		= new ResourceImpl();
	private final Map<String,List<Capability>>	capabilities	= new LinkedHashMap<String,List<Capability>>();
	private final Map<String,List<Requirement>>	requirements	= new LinkedHashMap<String,List<Requirement>>();

	private boolean								built			= false;

	public ResourceBuilder addCapability(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");
		Capability cap = builder.setResource(resource).buildCapability();
		List<Capability> caps = capabilities.get(cap.getNamespace());
		if (caps == null) {
			caps = new LinkedList<Capability>();
			capabilities.put(cap.getNamespace(), caps);
		}
		caps.add(cap);

		return this;
	}

	public ResourceBuilder addRequirement(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");
		Requirement req = builder.setResource(resource).buildRequirement();
		List<Requirement> reqs = requirements.get(req.getNamespace());
		if (reqs == null) {
			reqs = new LinkedList<Requirement>();
			requirements.put(req.getNamespace(), reqs);
		}
		reqs.add(req);

		return this;
	}

	public Resource build() {
		if (built)
			throw new IllegalStateException("Resource already built");
		built = true;

		resource.setCapabilities(capabilities);
		resource.setRequirements(requirements);
		return resource;
	}

}
