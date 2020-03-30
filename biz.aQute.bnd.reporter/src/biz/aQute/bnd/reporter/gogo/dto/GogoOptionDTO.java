package biz.aQute.bnd.reporter.gogo.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

public class GogoOptionDTO extends DTO {

	/**
	 * List of names of this option
	 */
	public List<String>	names		= new ArrayList<>();

	/**
	 * Description of this option.(optional)
	 */
	public String		description;

	/**
	 * Indicates if this option is a flag.
	 */
	public boolean		isFlag		= false;

	/**
	 * Indicates if this option can have multiple values.
	 */
	public boolean		multiValue	= false;
}
