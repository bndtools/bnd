package biz.aQute.bnd.reporter.manifest.dto;

import org.osgi.dto.DTO;

public class ContactAddressDTO extends DTO {

	/**
	 * The type of the address.
	 * <p>
	 * The type is either 'email', 'url' or 'postal'. This field must not be
	 * {@code null}.
	 * </p>
	 */
	public String	type;

	/**
	 * The contact address of the vendor.
	 * <p>
	 * This field must not be {@code null}.
	 * </p>
	 */
	public String	address;
}
