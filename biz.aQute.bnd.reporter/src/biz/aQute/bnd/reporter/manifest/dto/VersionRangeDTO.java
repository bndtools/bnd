package biz.aQute.bnd.reporter.manifest.dto;

import org.osgi.dto.DTO;

/**
 * A representation of a version range.
 */
public class VersionRangeDTO extends DTO {

	/**
	 * The floor of the range.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public VersionInRangeDTO	floor;

	/**
	 * The ceiling of the range.
	 * <p>
	 * If this field is {@code null} the ceiling must be interpreted as
	 * infinite.
	 * </p>
	 */
	public VersionInRangeDTO	ceiling;
}
