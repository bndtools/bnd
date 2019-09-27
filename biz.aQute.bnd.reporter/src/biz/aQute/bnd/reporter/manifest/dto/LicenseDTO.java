package biz.aQute.bnd.reporter.manifest.dto;

import org.osgi.dto.DTO;

/**
 * A representation of a license.
 */
public class LicenseDTO extends DTO {

	/**
	 * The name of the license.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String	name;

	/**
	 * The description of the license.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String	description;

	/**
	 * The URL to the license.
	 * <p>
	 * If the URL does not contain a scheme, the URL must be interpreted as
	 * relative to the bundle. If it is not specified this field must be
	 * {@code null}.
	 * </p>
	 */
	public String	link;
}
