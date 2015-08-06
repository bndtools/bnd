package aQute.bnd.osgi.resource;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementBuilder extends CapReqBuilder {

	public RequirementBuilder(Resource resource, String namespace) {
		super(resource, namespace);
	}

	public RequirementBuilder(String namespace) {
		super(namespace);
	}

	public Requirement build() {
		return super.buildRequirement();
	}

	public Requirement synthetic() {
		return super.buildSyntheticRequirement();
	}

}
