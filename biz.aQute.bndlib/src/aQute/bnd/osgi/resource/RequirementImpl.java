package aQute.bnd.osgi.resource;

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.conversions.RequirementFormatter;

public class RequirementImpl extends CapReq implements Requirement {
	private static final RequirementFormatter	rf	= new RequirementFormatter();
	private String								msg	= null;

	RequirementImpl(String namespace, Resource resource, Map<String, String> directives,
		Map<String, Object> attributes) {
		super(MODE.Requirement, namespace, resource, directives, attributes);
	}

	@Override
	public String toString() {
		String m = msg;
		if (m != null) {
			return m;
		}
		return msg = rf.convert(this);
	}
}
