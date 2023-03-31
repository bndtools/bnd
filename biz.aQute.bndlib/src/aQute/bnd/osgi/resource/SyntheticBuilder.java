package aQute.bnd.osgi.resource;

import org.osgi.resource.Resource;

public class SyntheticBuilder extends ResourceBuilder {

	public SyntheticBuilder(Resource source) throws Exception {
		super(source);
	}

	public SyntheticBuilder() {
		super();
	}

	@Override
	protected CapabilityImpl buildCapability(CapReqBuilder builder) {
		return builder.buildSyntheticCapability();
	}

	@Override
	protected RequirementImpl buildRequirement(CapReqBuilder builder) {
		return builder.buildSyntheticRequirement();
	}

}
