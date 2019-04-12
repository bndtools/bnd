package aQute.bnd.annotation;

/**
 * For use in the creation of custom bundle annotations wishing to control the
 * cardinality of generated requirements.
 * <p>
 * The {@link #name} will automatically be converted to lower case during macro
 * processing.
 */
public enum Cardinality {
	/**
	 * Indicates if the requirement can only be wired a single time.
	 */
	SINGLE("single"), // Namespace.CARDINALITY_SINGLE

	/**
	 * Indicates if the requirement can be wired multiple times.
	 */
	MULTIPLE("multiple"), // Namespace.CARDINALITY_MULTIPLE

	/**
	 * Default element value for annotation. This is used to distinguish the
	 * default value for an element and should not otherwise be used.
	 */
	DEFAULT("<<default>>");

	private final String value;

	Cardinality(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
