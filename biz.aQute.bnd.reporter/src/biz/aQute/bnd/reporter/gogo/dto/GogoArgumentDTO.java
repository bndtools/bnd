package biz.aQute.bnd.reporter.gogo.dto;

import org.osgi.dto.DTO;

public class GogoArgumentDTO extends DTO {

	/**
	 * Name of this argument.
	 */
	public String	name;

	/**
	 * Description of this argument.(optional)
	 */
	public String	description;

	/**
	 * Indicates if this argument can have multiple values.
	 */
	public boolean	multiValue	= false;
}
