package aQute.lib.deployer.obr;

public class Require {

	private final String name;
	private final String filter;
	private final boolean optional;

	public Require(String name, String filter, boolean optional) {
		this.name = name;
		this.filter = filter;
		this.optional = optional;
	}

	public String getName() {
		return name;
	}

	public String getFilter() {
		return filter;
	}

	public boolean isOptional() {
		return optional;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Require [");
		if (name != null)
			builder.append("name=").append(name).append(", ");
		if (filter != null)
			builder.append("filter=").append(filter).append(", ");
		builder.append("optional=").append(optional).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (optional ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Require other = (Require) obj;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (optional != other.optional)
			return false;
		return true;
	}

}
