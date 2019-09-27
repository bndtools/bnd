package biz.aQute.bnd.reporter.codesnippet.dto;

import org.osgi.dto.DTO;

/**
 * A code snippet.
 */
public class CodeSnippetDTO extends DTO {

	/**
	 * Id of this code snippet.
	 */
	public String	id;

	/**
	 * Title of this code snippet (optional).
	 */
	public String	title;

	/**
	 * Short description of this code snippet (optional).
	 */
	public String	description;
}
