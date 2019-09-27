package aQute.bnd.deployer.repository.providers;

public enum ScalarType {
	String,
	Version,
	Long,
	Double;

	public Object parseString(String input) {
		Object result;

		switch (this) {
			case String :
				result = input;
				break;
			case Long :
				result = java.lang.Long.parseLong(input.trim());
				break;
			case Double :
				result = java.lang.Double.parseDouble(input.trim());
				break;
			case Version :
				result = org.osgi.framework.Version.parseVersion(input.trim());
				break;
			default :
				throw new IllegalArgumentException("Cannot parse input for unknown attribute type '" + name() + "'");
		}

		return result;
	}
}
