package bndtools.editor.model;

public enum UpperVersionMatchType {
	Exact("${@}"),
	NextMicro("${version;==+;${@}}"),
	NextMinor("${version;=+;${@}}"),
	NextMajor("${version;+;${@}}");

	private final String representation;

	private UpperVersionMatchType(String representation) {
		this.representation = representation;
	}

	public String getRepresentation() {
		return representation;
	}

	public static UpperVersionMatchType parse(String string) throws IllegalArgumentException {
		for (UpperVersionMatchType type : UpperVersionMatchType.class.getEnumConstants()) {
			if (type.getRepresentation()
				.equals(string)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Failed to parse version match type.");
	}

}
