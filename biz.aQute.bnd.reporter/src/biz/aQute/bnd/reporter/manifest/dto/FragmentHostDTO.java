package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation a fragment host clause.
 */
public class FragmentHostDTO extends DTO {

	/**
	 * The bundle symbolic name of the host.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String				bundleSymbolicName;

	/**
	 * The extension type, framework or boot class path extension.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public String				extension;

	/**
	 * The version range to select the host bundle.
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
