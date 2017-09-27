package aQute.bnd.metadata.dto;

import org.osgi.dto.DTO;

/**
 * A representation of a version range.
 */
public class VersionRangeDTO extends DTO {

	/**
	 * Indicates if the floor is included in the range.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public Boolean		includeFloor;

	/**
	 * The floor of the range.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public VersionDTO	floor;

	/**
	 * The ceiling of the range.
	 * <p>
	 * If this field is {@code null} the ceiling must be interpreted as infinite and
	 * the includeCeiling must be false.
	 * </p>
	 */
	public VersionDTO	ceiling;

	/**
	 * Indicates if the ceiling is included in the range.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public Boolean		includeCeiling;
}
