package aQute.bnd.osgi.resource;

import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class WireImpl implements Wire {

	private final Capability	capability;
	private final Requirement	requirement;

	public WireImpl(Capability capability, Requirement requirement) {
		if (capability == null || requirement == null) {
			throw new IllegalArgumentException(
				"Both a capabability and a requirement are required. The following were supplied. Cap: "
					+ String.valueOf(capability) + " Req: " + String.valueOf(requirement));
		}
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
		builder.append("WireImpl [")
			.append(requirement.toString())
			.append("  -->  ")
			.append(capability)
			.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(capability, requirement);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof Wire) {
			Wire w = (Wire) obj;

			if (capability.equals(w.getCapability()) && requirement.equals(w.getRequirement())) {
				Resource provider = getProvider();
				Resource requirer = getRequirer();

				return (provider == null ? w.getProvider() == null : provider.equals(w.getProvider()))
					&& (requirer == null ? w.getRequirer() == null : requirer.equals(w.getRequirer()));
			}
		}
		return false;
	}

}
