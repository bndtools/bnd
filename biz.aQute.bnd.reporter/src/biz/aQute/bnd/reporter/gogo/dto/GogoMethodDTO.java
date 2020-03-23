package biz.aQute.bnd.reporter.gogo.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

public class GogoMethodDTO extends DTO {

	/**
	 * Short description of this Gogo method.(optional)
	 */
	public String					description;

	/**
	 * List of options of this method.(optional)
	 */
	public List<GogoOptionDTO>		options		= new ArrayList<>();

	/**
	 * List of arguments of this method.(optional)
	 */
	public List<GogoArgumentDTO>	arguments	= new ArrayList<>();
}
