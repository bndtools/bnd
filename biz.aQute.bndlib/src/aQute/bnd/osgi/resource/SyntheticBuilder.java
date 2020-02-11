package aQute.bnd.osgi.resource;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class SyntheticBuilder extends ResourceBuilder {

	public SyntheticBuilder(Resource source) throws Exception {
		super(source);
	}

	public SyntheticBuilder() {
		super();
	}

	@Override
	protected Capability buildCapability(CapReqBuilder builder) {
		Capability cap = builder.buildSyntheticCapability();
		return cap;
	}

	@Override
	protected Requirement buildRequirement(CapReqBuilder builder) {
		Requirement req = builder.buildSyntheticRequirement();
		return req;
	}

}
