package aQute.maven.dto;

/**
 * This is the property specification used to activate a profile. If the value
 * field is empty, then the existence of the named property will activate the
 * profile, otherwise it does a case-sensitive match against the property value
 * as well.
 */
public class ActivationPropertyDTO {
	/**
	 * The name of the property to be used to activate a profile.
	 */
	public String	name;

	/**
	 * The value of the property required to activate a profile.
	 */

	public String	value;
}
