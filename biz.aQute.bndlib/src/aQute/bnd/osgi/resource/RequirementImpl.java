package aQute.bnd.osgi.resource;

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementImpl extends CapReq implements Requirement {
	static FilterParser	fp	= new FilterParser();
	String				msg	= null;

	RequirementImpl(String namespace, Resource resource, Map<String,String> directives, Map<String,Object> attributes) {
		super(MODE.Requirement, namespace, resource, directives, attributes);
	}

	public String toString() {
		if (msg == null) {
			msg = fp.parse(this).toString();
		}
		return msg;
	}
}
