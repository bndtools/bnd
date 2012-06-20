package biz.aQute.r5.resource;

import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class ResourceImpl implements Resource {

	private Map<String,List<Capability>>	capabilities;
	private Map<String,List<Requirement>>	requirements;

	void setCapabilities(Map<String,List<Capability>> capabilities) {
		this.capabilities = capabilities;
	}

	public List<Capability> getCapabilities(String namespace) {
		return capabilities.get(namespace);
	}

	void setRequirements(Map<String,List<Requirement>> requirements) {
		this.requirements = requirements;
	}

	public List<Requirement> getRequirements(String namespace) {
		return requirements.get(namespace);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourceImpl [caps=");
		builder.append(capabilities);
		builder.append(", reqs=");
		builder.append(requirements);
		builder.append("]");
		return builder.toString();
	}
	

}
