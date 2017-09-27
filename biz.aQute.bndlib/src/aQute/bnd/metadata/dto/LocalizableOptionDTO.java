package aQute.bnd.metadata.dto;

import org.osgi.dto.DTO;

/**
 * A localizable representation of an {@code AttributeDefinition} option.
 */
public class LocalizableOptionDTO extends DTO {

	/**
	 * The label of the option.
	 * <p>
	 * If it is not specified this field must not be {@code null}.
	 * </p>
	 */
	public String label;
}
