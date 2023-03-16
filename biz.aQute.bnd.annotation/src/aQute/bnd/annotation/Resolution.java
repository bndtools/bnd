package aQute.bnd.annotation;

/**
 * For use in the creation of custom bundle annotations wishing to control the
 * resolution of generated requirements.
 */
public interface Resolution {
	/**
	 * A mandatory requirement forbids the bundle to resolve when the
	 * requirement is not satisfied.
	 */
	String	MANDATORY	= "mandatory";	// Namespace.RESOLUTION_MANDATORY

	/**
	 * An optional requirement allows a bundle to resolve even if the
	 * requirement is not satisfied.
	 */
	String	OPTIONAL	= "optional";	// Namespace.RESOLUTION_OPTIONAL

	/**
	 * Default element value for annotation. This is used to distinguish the
	 * default value for an element and should not otherwise be used.
	 */
	String	DEFAULT		= "default";
}
