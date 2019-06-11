package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of a provided capability.
 */
public class ProvideCapabilityDTO extends DTO {

	/**
	 * The namespace of the capability.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String								namespace;

	/**
	 * The time at which a capability will be available.
	 * <p>
	 * If it is not specified this field must be set to "resolve".
	 * </p>
	 */
	public String								effective			= "resolve";

	/**
	 * A lists of package names that are used by the capability.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>							uses				= new LinkedList<>();

	/**
	 * A map of attributes that define the capability.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String, TypedAttributeValueDTO>	typedAttributes		= new LinkedHashMap<>();

	/**
	 * A map of arbitrary directives.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String, String>					arbitraryDirectives	= new LinkedHashMap<>();
}
