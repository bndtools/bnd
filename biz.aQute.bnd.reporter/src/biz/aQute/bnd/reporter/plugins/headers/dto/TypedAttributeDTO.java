package biz.aQute.bnd.reporter.plugins.headers.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A representation of a typed property
 */
public class TypedAttributeDTO {

	/**
	 * The name of the property.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String name;

	/**
	 * The type of the property.
	 * <p>
	 * The type must be the name of the scalar type (eg: {@code String}). This field
	 * must not be {@code null}.
	 * </p>
	 */
	public String type;

	/**
	 * Indicates if the property can have multiple values.
	 * <p>
	 * If it is not specified this field must be set to false.
	 * </p>
	 */
	public boolean multiValue = false;

	/**
	 * A list of values.
	 * <p>
	 * If it not specified this field must be empty.
	 * </p>
	 */
	public List<String> values = new LinkedList<>();
}
