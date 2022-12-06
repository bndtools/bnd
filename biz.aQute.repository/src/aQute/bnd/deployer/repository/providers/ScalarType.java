package aQute.bnd.deployer.repository.providers;

public enum ScalarType {
	String,
	Version,
	Long,
	Double;

	public Object parseString(String input) {
		Object result = switch (this) {
			case String -> input;
			case Long -> java.lang.Long.valueOf(input.trim());
			case Double -> java.lang.Double.valueOf(input.trim());
			case Version -> org.osgi.framework.Version.parseVersion(input.trim());
			default -> throw new IllegalArgumentException(
				"Cannot parse input for unknown attribute type '" + name() + "'");
		};

		return result;
	}
}
