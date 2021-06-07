package aQute.bnd.osgi.resource;

import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.conversions.CapabilityFormatter;

public class CapabilityImpl extends CapReq implements Capability {
	private static final CapabilityFormatter	cf	= new CapabilityFormatter();
	private transient String					msg	= null;

	CapabilityImpl(String namespace, Resource resource, Map<String, String> directives,
		Map<String, Object> attributes) {
		super(namespace, resource, directives, attributes);
	}

	@Override
	public String toString() {
		String m = msg;
		if (m != null) {
			return m;
		}
		return msg = cf.convert(this);
	}
}
