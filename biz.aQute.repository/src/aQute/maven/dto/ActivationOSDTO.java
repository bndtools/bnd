package aQute.maven.dto;

/**
 * This is an activator which will detect an operating system's attributes in
 * order to activate its profile.
 */
public class ActivationOSDTO {

	/**
	 * The name of the operating system to be used to activate the profile. This
	 * must be an exact match of the <code>${os.name}</code> Java property, such
	 * as <code>Windows XP</code>.
	 */
	public String	name;

	/**
	 * The general family of the OS to be used to activate the profile, such as
	 * <code>windows</code> or <code>unix</code>.
	 */
	public String	family;

	/**
	 * The architecture of the operating system to be used to activate the
	 * profile.
	 */

	public String	arch;

	/**
	 * The version of the operating system to be used to activate the profile.
	 */

	public String	version;
}
