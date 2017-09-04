package aQute.bnd.metadata.dto;

import java.util.List;

/**
 * A representation of a typed property
 */
public class TypedPropertyDTO {

	/**
	 * The name of the property.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String		name;

	/**
	 * The type of the property.
	 * <p>
	 * The type must be the name of the scalar type (eg: {@code String}). This field
	 * must not be {@code null}.
	 * </p>
	 */
	public String		type;

	/**
	 * Indicates if the property can have multiple values.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public Boolean		multipleValues;

	/**
	 * A list of values.
	 * <p>
	 * If it not specified this field must be empty.
	 * </p>
	 */
	public List<String>	values;
}
