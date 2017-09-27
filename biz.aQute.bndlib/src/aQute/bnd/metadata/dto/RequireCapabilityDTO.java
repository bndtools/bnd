package aQute.bnd.metadata.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a required capability.
 */
public class RequireCapabilityDTO extends DTO {

	/**
	 * The name space of the capability.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String					nameSpace;

	/**
	 * The time at which the requirement will be considered.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String					effective;

	/**
	 * Indicates if the resolution is optional or mandatory.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String					resolution;

	/**
	 * Indicates if the requirement can be wired a single time or multiple times.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String					cardinality;

	/**
	 * A filter expression that is asserted on the Capabilities.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String					filter;

	/**
	 * A list of attributes used by the filter.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<TypedPropertyDTO>	typedAttributes;
}
