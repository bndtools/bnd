package aQute.maven.dto;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.util.dto.DTO;

/**
 * Description of a person who has contributed to the project, but who does not
 * have commit privileges. Usually, these contributions come in the form of
 * patches submitted.
 */
public class ContributorDTO extends DTO {
	/**
	 * The full name of the contributor.
	 */
	public String				name;

	/**
	 * The email address of the contributor.
	 */
	public String				email;

	/**
	 * The URL for the homepage of the contributor.
	 */
	public URI					url;

	/**
	 * The organization to which the contributor belongs.
	 */
	public String				organization;
	/**
	 * The URL of the organization.
	 */
	public URI					organizationUrl;

	/**
	 * The roles the contributor plays in the project. Each role is described by
	 * a <code>role</code> element, the body of which is a role name. This can
	 * also be used to describe the contribution.
	 */
	public String[]				roles;

	/**
	 * The timezone the contributor is in. Typically, this is a number in the
	 * range <a href="http://en.wikipedia.org/wiki/UTC%E2%88%9212:00">-12</a> to
	 * <a href="http://en.wikipedia.org/wiki/UTC%2B14:00">+14</a> or a valid
	 * time zone id like "America/Montreal" (UTC-05:00) or "Europe/Paris"
	 * (UTC+01:00).
	 */
	public String				timezone;

	/**
	 * Properties about the contributor, such as an instant messenger handle.
	 */
	public Map<String, String>	properties	= new HashMap<>();

}
