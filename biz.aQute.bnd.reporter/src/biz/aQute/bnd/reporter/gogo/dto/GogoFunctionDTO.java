package biz.aQute.bnd.reporter.gogo.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class GogoFunctionDTO extends DTO {

	/**
	 * Title of this gogo funktion.
	 */
	public String		title;

	/**
	 * List of methods that are provided by this funktion.
	 */
	public List<GogoMethodDTO>	methods;
}
