package aQute.bnd.metadata.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of a provided capability.
 */
public class ProvideCapabilityDTO extends DTO {

	/**
	 * The name space of the capability.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String					nameSpace;

	/**
	 * The time at which a capability will be available.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String					effective;

	/**
	 * A lists of package names that are used by the capability.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>				uses;

	/**
	 * A list of attributes that define the capability.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<TypedPropertyDTO>	typedAttributes;

	/**
	 * A map of arbitrary directives.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,String>		arbitraryDirectives;
}
