package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * Describes a build extension to utilise.
 */
public class ExtensionDTO extends DTO {

	/**
	 * The group ID of the extension's artifact.
	 */
	public String		groupId;

	/**
	 * The artifact ID of the extension.
	 */
	public String		artifactId;

	/**
	 * The version of the extension.
	 */
	public MavenVersion	version;
}
