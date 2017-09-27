package aQute.bnd.metadata.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a lazy activation clause.
 */
public class LazyActivationDTO extends DTO {

	/**
	 * A list of package names.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public List<String>	include;

	/**
	 * A list of package names.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>	exclude;
}
