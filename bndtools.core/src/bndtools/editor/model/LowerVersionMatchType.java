package bndtools.editor.model;

public enum LowerVersionMatchType {
	Exact("${@}"),
	Micro("${version;===;${@}}"),
	Minor("${version;==;${@}}"),
	Major("${version;=;${@}}");

	private final String representation;

	private LowerVersionMatchType(String representation) {
		this.representation = representation;
	}

	public String getRepresentation() {
		return representation;
	}

	public static LowerVersionMatchType parse(String string) throws IllegalArgumentException {
		for (LowerVersionMatchType type : LowerVersionMatchType.class.getEnumConstants()) {
			if (type.getRepresentation()
				.equals(string)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Failed to parse version match type.");
	}
}
