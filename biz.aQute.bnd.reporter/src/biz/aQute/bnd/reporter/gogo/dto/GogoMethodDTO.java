package biz.aQute.bnd.reporter.gogo.dto;

import java.util.List;

public class GogoMethodDTO {
	/**
	 * Title of this gogo method.
	 */
	public String			title;

	/**
	 * Short description of this gogo method (optional).
	 */
	public String			description;

	/**
	 * List of parameters of this method.
	 */
	List<GogoParameterDTO>	parameters;
}
