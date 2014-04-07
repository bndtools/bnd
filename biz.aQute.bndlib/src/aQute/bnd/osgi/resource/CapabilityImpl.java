package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

public class CapabilityImpl extends CapReq implements Capability {

	CapabilityImpl(String namespace, Resource resource, Map<String,String> directives,
			Map<String,Object> attributes) {
		super(MODE.Capability, namespace, resource, directives, attributes);
	}

}
