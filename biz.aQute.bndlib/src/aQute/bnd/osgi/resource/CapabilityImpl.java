package aQute.bnd.osgi.resource;

import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CapabilityImpl extends CapReq implements Capability {

	CapabilityImpl(String namespace, Resource resource, Map<String, String> directives,
		Map<String, Object> attributes) {
		super(MODE.Capability, namespace, resource, directives, attributes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Provide");
		super.toString(sb);
		return sb.toString();
	}
}
