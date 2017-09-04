package aQute.bnd.metadata.dto;

import java.util.List;
import java.util.Map;

/**
 * A representation of an {@code ObjectClassDefinition}
 */
public class ObjectClassDefinitionDTO extends LocalizableObjectClassDefinitionDTO {

	/**
	 * The name of the OCD.
	 * <p>
	 * Must not be {@code null}.
	 * <p>
	 */
	public String											name;

	/**
	 * The id of the OCD.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String											id;

	/**
	 * A list of pids.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<String>										pids;

	/**
	 * A list of factory pids.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<String>										factoryPids;

	/**
	 * A list of the icons of this OCD.
	 * <p>
	 * If it is not specified this field must be empty.
	 * <p>
	 */
	public List<IconDTO>									icons;

	/**
	 * A list of attributes.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<AttributeDefinitionDTO>						attributes;

	/**
	 * A map of localized {@code LocalizableObjectClassDefinitionDTO} attributes
	 * whose key is the local.
	 * <p>
	 * The format of the local is defined in {@code java.util.Locale}, eg:
	 * en_GB_welsh. If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,LocalizableObjectClassDefinitionDTO>	localizations;
}
