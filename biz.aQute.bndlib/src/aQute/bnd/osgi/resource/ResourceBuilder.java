package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

public class ResourceBuilder {

	private final ResourceImpl		resource		= new ResourceImpl();
	private final List<Capability>	capabilities	= new LinkedList<Capability>();
	private final List<Requirement>	requirements	= new LinkedList<Requirement>();

	private boolean					built			= false;

	public ResourceBuilder addCapability(Capability capability) {
		CapReqBuilder builder = CapReqBuilder.clone(capability);
		return addCapability(builder);
	}
	
	public ResourceBuilder addCapability(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");

		Capability cap = builder.setResource(resource).buildCapability();
		capabilities.add(cap);

		return this;
	}
	
	public ResourceBuilder addRequirement(Requirement requirement) {
		CapReqBuilder builder = CapReqBuilder.clone(requirement);
		return addRequirement(builder);
	}
	
	public ResourceBuilder addRequirement(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");

		Requirement req = builder.setResource(resource).buildRequirement();
		requirements.add(req);

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
