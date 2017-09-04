package aQute.bnd.metadata.dto;

import java.util.List;
import java.util.Map;

/**
 * A representation of an {@code AttributeDefinition}.
 */
public class AttributeDefinitionDTO extends LocalizableAttributeDefinitionDTO {

	/**
	 * The name of the AD.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String											name;

	/**
	 * The typed property of the AD.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public TypedPropertyDTO									property;

	/**
	 * The cardinality of the AD.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public Integer											cardinality;

	/**
	 * The minimal value of the AD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String											min;

	/**
	 * The maximal value of the AD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String											max;

	/**
	 * Indicates if the attribute is required.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public Boolean											required;

	/**
	 * A list of the option entry of the AD.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<OptionDTO>									options;

	/**
	 * A map of localized {@code AttributeDefinition} attributes whose key is the
	 * local.
	 * <p>
	 * The format of the local is defined in {@code java.util.Locale}, eg:
	 * en_GB_welsh. If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,LocalizableAttributeDefinitionDTO>	localizations;
}
