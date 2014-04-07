package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

public class RequirementImpl extends CapReq implements Requirement {

	RequirementImpl(String namespace, Resource resource, Map<String,String> directives, Map<String,Object> attributes) {
		super(MODE.Requirement, namespace, resource, directives, attributes);
	}

}
