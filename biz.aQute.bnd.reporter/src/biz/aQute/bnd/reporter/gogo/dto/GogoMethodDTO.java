package biz.aQute.bnd.reporter.gogo.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class GogoMethodDTO extends DTO {
	/**
	 * Title of this gogo method.
	 */
	public String			title;

	/**
	 * Short description of this gogo method.(optional)
	 */
	public String			description;

	/**
	 * List of parameters of this method.(optional)
	 */
	public List<GogoParameterDTO>	parameters;
}
