package biz.aQute.bnd.reporter.gogo.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a Gogo Scope.
 */
public class GogoScopeDTO extends DTO {

	/**
	 * Name of this gogo scope.
	 */
	public String					name;

	/**
	 * List of functions that are grouped by this scope.
	 */
	public List<GogoFunctionDTO>	functions	= new ArrayList<>();
}
