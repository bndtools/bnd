package aQute.bnd.metadata.dto;

import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of an import package clause.
 */
public class ImportPackageDTO extends DTO {

	/**
	 * The package name.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public String				packageName;

	/**
	 * Indicates if the resolution is optional or mandatory.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String				resolution;

	/**
	 * The bundle symbolic name of the exporting bundle.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * </p>
	 */
	public String				bsn;

	/**
	 * The version range to select the version of an export definition.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public VersionRangeDTO		version;

	/**
	 * The version range to select the bundle version of the exporting bundle.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public VersionRangeDTO		bundleVersion;

	/**
	 * A map of arbitrary attributes.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,String>	arbitraryAttributes;
}
