package biz.aQute.bnd.reporter.gogo.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

public class GogoFunctionDTO extends DTO {

	/**
	 * Name of this Gogo function.
	 */
	public String				name;

	/**
	 * List of methods that are provided by this function.
	 */
	public List<GogoMethodDTO>	methods	= new ArrayList<>();
}
