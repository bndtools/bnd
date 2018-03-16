package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * The conditions within the build runtime environment which will trigger the
 * automatic inclusion of the build profile. Multiple conditions can be defined,
 * which must be all satisfied to activate the profile.
 */
public class ActivationDTO extends DTO {
	/**
	 * If set to true, this profile will be active unless another profile in
	 * this pom is activated using the command line -P option or by one of that
	 * profile's activators.
	 */
	public boolean					activeByDefault	= false;

	/**
	 * Specifies that this profile will be activated when a matching JDK is
	 * detected. For example, <code>1.4</code> only activates on JDKs versioned
	 * 1.4, while <code>!1.4</code> matches any JDK that is not version 1.4.
	 * Ranges are supported too: <code>[1.5,)</code> activates when the JDK is
	 * 1.5 minimum.
	 */

	public String					jdk;

	/**
	 * Specifies that this profile will be activated when matching operating
	 * system attributes are detected.
	 */

	public ActivationOSDTO			os;

	/**
	 * Specifies that this profile will be activated when this system property
	 * is specified.
	 */

	public ActivationPropertyDTO	property;

	/**
	 * Specifies that this profile will be activated based on existence of a
	 * file.
	 */

	public ActivationFileDTO		file;
}
