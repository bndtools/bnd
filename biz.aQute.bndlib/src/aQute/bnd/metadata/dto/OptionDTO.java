package aQute.bnd.metadata.dto;

import java.util.Map;

/**
 * A representation of an {@code AttributeDefinition} option.
 */
public class OptionDTO extends LocalizableOptionDTO {

	/**
	 * The value of the option.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String							value;

	/**
	 * A map of localized option attributes whose key is the local.
	 * <p>
	 * The format of the local is defined in {@code java.util.Locale}, eg:
	 * en_GB_welsh. If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,LocalizableOptionDTO>	localizations;
}
