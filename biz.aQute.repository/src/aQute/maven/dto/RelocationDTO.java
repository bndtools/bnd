package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * Describes where an artifact has moved to. If any of the values are omitted,
 * it is assumed to be the same as it was before.
 */
public class RelocationDTO extends DTO {

	/**
	 * The group ID the artifact has moved to.
	 */
	public String		groupId;

	/**
	 * The new artifact ID of the artifact.
	 */

	public String		artifactId;

	/**
	 * The new version of the artifact.
	 */

	public MavenVersion	version;

	/**
	 * An additional message to show the user about the move, such as the
	 * reason.
	 */

	public String		message;
}
