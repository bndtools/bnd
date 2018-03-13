package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

public class IssueManagementDTO extends DTO {

	/**
	 * The name of the issue management system, e.g. Bugzilla
	 */
	public String	system;

	/**
	 * URL for the issue management system used by the project.
	 */
	public URI		url;
}
