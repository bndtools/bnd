package biz.aQute.bnd.reporter.manifest.dto;

import org.osgi.dto.DTO;

/**
 * A representation of an icon.
 */
public class IconDTO extends DTO {

	/**
	 * The URL of the icon.
	 * <p>
	 * If the URL does not contain a scheme, the URL must be interpreted as
	 * relative to the bundle. Must not be {@code null}.
	 * </p>
	 */
	public String	url;

	/**
	 * The horizontal size of the image in pixel.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public Integer	size;
}
