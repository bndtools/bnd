package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of an export package clause.
 */
public class ExportPackageDTO extends DTO {

	/**
	 * The package name.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public String				packageName;

	/**
	 * A list of package names that are used by the exported package.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>			uses				= new LinkedList<>();

	/**
	 * A list of mandatory attributes.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>			mandatories			= new LinkedList<>();

	/**
	 * A list of class names that must be visible to an importer.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>			includes			= new LinkedList<>();

	/**
	 * A list of class names that must be invisible to an importer.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public List<String>			excludes			= new LinkedList<>();

	/**
	 * The version of the exported package.
	 * <p>
	 * If it is not specified this field must be set to the default value.
	 * </p>
	 */
	public VersionDTO			version;

	/**
	 * A map of arbitrary attributes.
	 * <p>
	 * If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String, String>	arbitraryAttributes	= new LinkedHashMap<>();
}
