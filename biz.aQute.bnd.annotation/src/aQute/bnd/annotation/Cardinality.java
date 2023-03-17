package aQute.bnd.annotation;

/**
 * For use in the creation of custom bundle annotations wishing to control the
 * cardinality of generated requirements.
 */
public interface Cardinality {
	/**
	 * Indicates if the requirement can only be wired a single time.
	 */
	String	SINGLE		= "single";		// Namespace.CARDINALITY_SINGLE

	/**
	 * Indicates if the requirement can be wired multiple times.
	 */
	String	MULTIPLE	= "multiple";	// Namespace.CARDINALITY_MULTIPLE

	/**
	 * Default element value for annotation. This is used to distinguish the
	 * default value for an element and should not otherwise be used.
	 */
	String	DEFAULT		= "default";

}
