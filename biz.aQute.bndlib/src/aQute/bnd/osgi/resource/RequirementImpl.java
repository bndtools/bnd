package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

public class RequirementImpl extends CapReq implements Requirement {

	RequirementImpl(String namespace, Resource resource, Map<String,String> directives, Map<String,Object> attributes) {
		super(MODE.Requirement, namespace, resource, directives, attributes);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Require");
		super.toString(sb);
		return sb.toString();
	}
}
