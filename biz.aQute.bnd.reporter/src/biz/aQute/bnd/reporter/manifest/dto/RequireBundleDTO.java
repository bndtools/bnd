package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of a require bundle clause.
 */
public class RequireBundleDTO extends DTO {

	/**
	 * The bundle symbolic name of the required bundle.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String				bundleSymbolicName;

	/**
	 * Indicates if the bundle will transitively have access to these required
	 * bundle's exported packages.
	 * <p>
	 * If it is not specified this field must be set to "private".
	 * </p>
	 */
	public String				visibility			= "private";

	/**
	 * Indicates if the resolution is optional or mandatory.
	 * <p>
	 * If it is not specified this field must be set to "mandatory".
	 * </p>
	 */
	public String				resolution			= "mandatory";

	/**
	 * The version range to select the required bundle.
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
	public Map<String, String>	arbitraryAttributes	= new LinkedHashMap<>();
}
