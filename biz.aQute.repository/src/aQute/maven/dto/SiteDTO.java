package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * Contains the information needed for deploying websites.
 */
public class SiteDTO extends DTO {
	/**
	 * A unique identifier for a deployment location. This is used to match the
	 * site to configuration in the <code>settings.xml</code> file, for example.
	 */
	public String	id;

	/**
	 * Human readable name of the deployment location.
	 */

	public String	name;

	/**
	 * The url of the location where website is deployed, in the form
	 * <code>protocol://hostname/path</code>. <br />
	 * <b>Default value is</b>: parent value [+ path adjustment] + artifactId
	 */

	public URI		url;
}
