package biz.aQute.bnd.reporter.gogo.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of an Gogo Scope.
 */
public class GogoScopeDTO extends DTO {


	/**
	 * Title of this gogo scope.
	 */
	public String	title;

	/**
	 * List of functions that are grouped by this scope.
	 */
	public List<GogoFunctionDTO>	functions;
}
