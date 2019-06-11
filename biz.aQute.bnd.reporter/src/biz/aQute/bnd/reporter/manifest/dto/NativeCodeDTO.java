package biz.aQute.bnd.reporter.manifest.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of a native code.
 */
public class NativeCodeDTO extends DTO {

	/**
	 * A list of native code entries.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, this list must
	 * contain at least one native code entry.
	 * </p>
	 */
	public List<NativeCodeEntryDTO>	entries		= new LinkedList<>();

	/**
	 * Indicates if native codes are optional.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if it is not
	 * specified this field must be set to false.
	 * </p>
	 */
	public boolean					optional	= false;
}
