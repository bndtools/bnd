package aQute.bnd.metadata.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of developer
 */
public class DeveloperDTO extends DTO {

	/**
	 * The display name of the developer.
	 * <p>
	 * If not specified this field must be {@code null}.
	 * </p>
	 */
	public String		name;

	/**
	 * The email of the developer, must not be {@code null}.
	 */
	public String		email;

	/**
	 * The roles of the developer.
	 * <p>
	 * If not specified this field must be empty.
	 * </p>
	 */
	public List<String>	roles;

	/**
	 * The organization name of the developer.
	 * <p>
	 * If not specified this field must be {@code null}.
	 * </p>
	 */
	public String		organization;

	/**
	 * The organization URL of the developer.
	 * <p>
	 * If not specified this field must be {@code null}.
	 * </p>
	 */
	public String		organizationUrl;

	/**
	 * The timezone of the developer.
	 * <p>
	 * If not specified this field must be {@code null}.
	 * </p>
	 */
	public Integer		timezone;
}
