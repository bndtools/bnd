package aQute.bnd.osgi.resource;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CapabilityBuilder extends CapReqBuilder {

	public CapabilityBuilder(Resource resource, String namespace) {
		super(resource, namespace);
	}

	public CapabilityBuilder(String namespace) {
		super(namespace);
	}

	public Capability build() {
		return super.buildCapability();
	}

	public Capability synthetic() {
		return super.buildSyntheticCapability();
	}

}
