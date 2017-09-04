package aQute.bnd.metadata.dto;

import org.osgi.dto.DTO;

/**
 * A localizable representation of an {@code ObjecClassDefinition}
 */
public class LocalizableObjectClassDefinitionDTO extends DTO {

	/**
	 * The name of the OCD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * <p>
	 */
	public String			name;

	/**
	 * The description of the OCD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * <p>
	 */
	public String			description;

}
