package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * Section for management of default dependency information for use in a group
 * of POMs.
 */
public class DependencyManagementDTO extends DTO {

	/**
	 * The dependencies specified here are not used until they are referenced in
	 * a POM within the group. This allows the specification of a "standard"
	 * version for a particular dependency.
	 */

	public DependencyDTO[] dependencies;
}
