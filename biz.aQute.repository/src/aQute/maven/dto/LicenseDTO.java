package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * Describes the licenses for this project. This is used to generate the license
 * page of the project's web site, as well as being taken into consideration in
 * other reporting and validation. The licenses listed for the project are that
 * of the project itself, and not of dependencies.
 */
public class LicenseDTO extends DTO {
	/**
	 * The full legal name of the license.
	 */
	public String	name;

	/**
	 * The official url for the license text.
	 */
	public URI		url;

	/**
	 * The primary method by which this project may be distributed.
	 * <dl>
	 * <dt>repo</dt>
	 * <dd>may be downloaded from the Maven repository</dd>
	 * <dt>manual</dt>
	 * <dd>user must manually download and install the dependency.</dd>
	 * </dl>
	 */

	public String	distribution;

	/**
	 * Addendum information pertaining to this license.<
	 */

	public String	comments;
}
