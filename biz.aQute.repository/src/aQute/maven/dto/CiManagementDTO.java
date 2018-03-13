package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * The <code>&lt;CiManagement&gt;</code> element contains informations required
 * to the continuous integration system of the project.
 */
public class CiManagementDTO extends DTO {

	/**
	 * The name of the continuous integration system, e.g.
	 * <code>continuum</code>.
	 */
	public String			system;

	/**
	 * URL for the continuous integration system used by the project if it has a
	 * web interface.
	 */
	public URI				url;

	/**
	 * Configuration for notifying developers/users when a build is
	 * unsuccessful, including user information and notification mode.
	 */
	public NotifierDTO[]	notifiers;

}
