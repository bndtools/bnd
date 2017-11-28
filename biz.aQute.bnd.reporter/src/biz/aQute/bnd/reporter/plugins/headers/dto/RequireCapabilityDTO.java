package biz.aQute.bnd.reporter.plugins.headers.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a required capability.
 */
public class RequireCapabilityDTO extends DTO {

	/**
	 * The namespace of the capability.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String namespace;

	/**
	 * The time at which the requirement will be considered.
	 * <p>
	 * If it is not specified this field must be set to "resolve".
	 * </p>
	 */
	public String effective = "resolve";

	/**
	 * Indicates if the resolution is optional or mandatory.
	 * <p>
	 * If it is not specified this field must be set to "mandatory".
	 * </p>
	 */
	public String resolution = "mandatory";

	/**
	 * Indicates if the requirement can be wired a single time or multiple times.
	 * <p>
	 * If it is not specified this field must be set to "single".
	 * </p>
	 */
	public String cardinality = "single";

	/**
	 * A filter expression that is asserted on the Capabilities.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String filter;

	/**
	 * A list of attributes used by the filter.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<TypedAttributeDTO> typedAttributes = new LinkedList<>();
}
