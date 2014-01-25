package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

public class RequirementVariable implements Requirement {

	private String	namespace;
	private String	variableText;

	public RequirementVariable(String variableText) {
		// this.namespace = "osgi.identity";
		this.namespace = variableText;
		this.variableText = variableText;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String,String> getDirectives() {
		HashMap<String,String> ret = new HashMap<String,String>();
		ret.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, variableText);
		return ret;
	}

	public Map<String,Object> getAttributes() {
		return new HashMap<String,Object>();
	}

	public Resource getResource() {
		return null;
	}

	@Override
	public String toString() {
		return namespace;
	}

}
