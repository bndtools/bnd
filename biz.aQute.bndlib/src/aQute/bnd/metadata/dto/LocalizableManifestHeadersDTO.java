package aQute.bnd.metadata.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A localizable representation of a bundle.
 */
public class LocalizableManifestHeadersDTO extends DTO {

	/**
	 * A list of category names.
	 * <p>
	 * This is declared in the {@code Bundle-Category} header, if the bundle does
	 * not declare categories this field must be empty.
	 * </p>
	 */
	public List<String>			categories;

	/**
	 * The contact address of the vendor.
	 * <p>
	 * This is declared in the {@code Bundle-ContactAddress} header, if the bundle
	 * does not declare a contact address this field must be {@code null}.
	 * </p>
	 */
	public ContactAddressDTO	contactAddress;

	/**
	 * The copyright of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Copyright} header, if the bundle does
	 * not declare a copyright this field must be {@code null}.
	 * </p>
	 */
	public String				copyright;

	/**
	 * The description of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Description} header, if the bundle does
	 * not declare a description this field must be {@code null}.
	 * </p>
	 */
	public String				description;

	/**
	 * The URL pointing to the documentation of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-DocURL} header, if the bundle does not
	 * declare a URL this field must be {@code null}.
	 * </p>
	 */
	public String				docUrl;

	/**
	 * A list of icons.
	 * <p>
	 * This is declared in the {@code Bundle-Icon} header, if the bundle does not
	 * declare icons this field must be empty.
	 * </p>
	 */
	public List<IconDTO>		icons;

	/**
	 * A list of the licenses.
	 * <p>
	 * This is declared in the {@code Bundle-License} header, if the bundle does not
	 * declare licenses or an '<<EXTERNAL>>' license this field must be empty.
	 * </p>
	 */
	public List<LicenseDTO>		licenses;

	/**
	 * The name of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Name} header, if the bundle does not
	 * declare a name this field must be {@code null}.
	 * </p>
	 */
	public String				name;

	/**
	 * The update location of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-UpdateLocation} header, if the bundle
	 * does not declare an URL location this field must be {@code null}.
	 * </p>
	 */
	public String				updateLocation;

	/**
	 * The name of the vendor.
	 * <p>
	 * This is declared in the {@code Bundle-Vendor} header, if the bundle does not
	 * declare a vendor description this field must be {@code null}.
	 * </p>
	 */
	public String				vendor;

	/**
	 * A list of the developers of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Developers} header, if the bundle does
	 * not declare developers this field must be empty.
	 * </p>
	 */
	public List<DeveloperDTO>	developers;
}
