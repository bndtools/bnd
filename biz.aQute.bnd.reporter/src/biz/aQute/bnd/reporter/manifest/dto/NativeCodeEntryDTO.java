package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a native code entry.
 */
public class NativeCodeEntryDTO extends DTO {

	/**
	 * A list of paths.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, this list must
	 * contain at least one path.
	 * </p>
	 */
	public List<String>				paths				= new LinkedList<>();

	/**
	 * A list of operating system names.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<String>				osnames				= new LinkedList<>();

	/**
	 * A list of operating system version ranges.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<VersionRangeDTO>	osversions			= new LinkedList<>();

	/**
	 * A list of processors.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<String>				processors			= new LinkedList<>();

	/**
	 * A list of languages.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<String>				languages			= new LinkedList<>();

	/**
	 * A list of selection filters.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<String>				selectionFilters	= new LinkedList<>();
}
