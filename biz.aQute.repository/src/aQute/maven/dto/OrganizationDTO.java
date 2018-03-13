package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * Specifies the organization that produces this project.
 */
public class OrganizationDTO extends DTO {

	/**
	 * The full name of the organization.
	 */
	public String	name;

	/**
	 * The URL to the organization's home page.
	 */
	public URI		url;
}
