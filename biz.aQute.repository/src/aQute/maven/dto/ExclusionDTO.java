package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * The <code>&lt;exclusion&gt;</code> element contains informations required to
 * exclude an artifact to the project.
 */
public class ExclusionDTO extends DTO {
	/**
	 * The group ID of the project to exclude.
	 */
	public String	groupId;

	/**
	 * The artifact ID of the project to exclude.
	 */
	public String	artifactId;
}
