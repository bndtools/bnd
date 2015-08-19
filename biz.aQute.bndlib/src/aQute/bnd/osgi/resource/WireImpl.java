package aQute.bnd.osgi.resource;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class WireImpl implements Wire {

	private final Capability	capability;
	private final Requirement	requirement;

	public WireImpl(Capability capability, Requirement requirement) {
		this.capability = capability;
		this.requirement = requirement;
	}

	@Override
	public Capability getCapability() {
		return capability;
	}

	@Override
	public Requirement getRequirement() {
		return requirement;
	}

	@Override
	public Resource getProvider() {
		return capability.getResource();
	}

	@Override
	public Resource getRequirer() {
		return requirement.getResource();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("WireImpl [").append(requirement.toString()).append("  -->  ").append(capability).append("]");
		return builder.toString();
	}

}
