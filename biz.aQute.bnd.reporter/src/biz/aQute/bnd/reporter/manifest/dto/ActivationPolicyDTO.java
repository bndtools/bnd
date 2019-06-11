package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a lazy activation clause.
 */
public class ActivationPolicyDTO extends DTO {

	/**
	 * The bundle policy. Must not be {@code null}.
	 */
	public String		policy;

	/**
	 * A list of package names.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public List<String>	includes	= new LinkedList<>();

	/**
	 * A list of package names.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>	excludes	= new LinkedList<>();
}
