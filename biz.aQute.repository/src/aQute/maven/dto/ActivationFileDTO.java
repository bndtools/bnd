package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * This is the file specification used to activate the profile. The
 * <code>missing</code> value is the location of a file that needs to exist, and
 * if it doesn't, the profile will be activated. On the other hand,
 * <code>exists</code> will test for the existence of the file and if it is
 * there, the profile will be activated.<br/>
 * Variable interpolation for these file specifications is limited to
 * <code>${basedir}</code>, System properties and request properties.
 */
public class ActivationFileDTO extends DTO {
	/**
	 * The name of the file that must be missing to activate the profile.
	 */
	public String	missing;

	/**
	 * The name of the file that must exist to activate the profile.
	 */

	public String	exists;
}
