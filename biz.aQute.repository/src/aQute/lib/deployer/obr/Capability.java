package aQute.lib.deployer.obr;

import java.util.*;

public class Capability {
	
	private final String name;
	private final List<Property> properties;

	private Capability(String name, List<Property> properties) {
		this.name = name;
		this.properties = properties;
	}
	
	public static class Builder {
		private String name;
		private final List<Property> properties = new LinkedList<Property>();
		
		public Builder setName(String name) {
			this.name = name;
			return this;
		}
		
		public Builder addProperty(Property property) {
			this.properties.add(property);
			return this;
		}
		
		public Capability build() {
			if (name == null) throw new IllegalStateException("'name' field is not initialised.");
			return new Capability(name, Collections.unmodifiableList(properties));
		}
	}

	public String getName() {
		return name;
	}

	public List<Property> getProperties() {
		return properties;
	}
	
	public Property findProperty(String propertyName) {
		assert propertyName != null;
		for (Property prop : properties) {
			if (propertyName.equals(prop.getName()))
				return prop;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Capability [name=").append(name).append(", properties=").append(properties).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
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
		Capability other = (Capability) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

}
