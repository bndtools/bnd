package aQute.bnd.metadata.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A representation of a bundle symbolic name.
 */
public class BundleSymbolicNameDTO extends DTO {

	/**
	 * The symbolic name of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, must not be
	 * {@code null}.
	 * </p>
	 */
	public String				symbolicName;

	/**
	 * Indicates if the bundle must be singleton
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, if it is not
	 * specified this field must be set to the default value.
	 * </p>
	 */
	public Boolean				singleton;

	/**
	 * The fragment attachment policy of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, if it is not
	 * specified this field must be set to the default value.
	 * </p>
	 */
	public String				fragmentAttachment;

	/**
	 * A list of mandatory attributes of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public List<String>			mandatory;

	/**
	 * A map of arbitrary attributes of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, if it is not
	 * specified this field must be empty.
	 * </p>
	 */
	public Map<String,String>	arbitraryAttributes;
}
