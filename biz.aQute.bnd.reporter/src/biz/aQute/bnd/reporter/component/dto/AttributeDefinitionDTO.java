package biz.aQute.bnd.reporter.component.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of an {@code AttributeDefinition}.
 */
public class AttributeDefinitionDTO extends DTO {

	/**
	 * The id of the attribute.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String			id;

	/**
	 * The name of the AD.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String			name;

	/**
	 * The description of the AD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String			description;

	/**
	 * The type of the attribute.
	 * <p>
	 * The type must be the name of the scalar type (eg: {@code String}). The
	 * default is "String".
	 * </p>
	 */
	public String			type		= "String";

	/**
	 * A list of values.
	 * <p>
	 * If it not specified this field must be empty.
	 * </p>
	 */
	public List<String>		values		= new LinkedList<>();

	/**
	 * The cardinality of the AD.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public int				cardinality	= 0;

	/**
	 * The minimal value of the AD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String			min;

	/**
	 * The maximal value of the AD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String			max;

	/**
	 * Indicates if the attribute is required.
	 * <p>
	 * If it is not specified this field must be set to true.
	 * </p>
	 */
	public boolean			required	= true;

	/**
	 * A list of the option entry of the AD.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<OptionDTO>	options		= new LinkedList<>();
}
