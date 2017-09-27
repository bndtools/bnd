package aQute.bnd.metadata.dto;

import org.osgi.dto.DTO;

/**
 * A representation of a version.
 */
public class VersionDTO extends DTO {

	/**
	 * The major part of the version.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public Integer	major;

	/**
	 * The minor part of the version.
	 * <p>
	 * Must be {@code null} if the version does not contain a minor part.
	 * </p>
	 */
	public Integer	minor;

	/**
	 * The micro part of the version.
	 * <p>
	 * Must be {@code null} if the version does not contain a minor or a micro part.
	 * </p>
	 */
	public Integer	micro;

	/**
	 * The qualifier part of the version.
	 * <p>
	 * Must be {@code null} if the version does not contain a minor, a micro or a
	 * qualifier part.
	 * </p>
	 */
	public String	qualifier;
}
