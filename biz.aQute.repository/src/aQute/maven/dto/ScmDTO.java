package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * The <code>&lt;scm&gt;</code> element contains informations required to the
 * SCM (Source Control Management) of the project.
 */
public class ScmDTO extends DTO {
	/**
	 * The source control management system URL that describes the repository
	 * and how to connect to the repository. For more information, see the
	 * <a href="http://maven.apache.org/scm/scm-url-format.html">URL format</a>
	 * and <a href="http://maven.apache.org/scm/scms-overview.html">list of
	 * supported SCMs</a>. This connection is read-only. <br />
	 * <b>Default value is</b>: parent value [+ path adjustment] + artifactId
	 */
	public String	connection;

	/**
	 * Just like <code>connection</code>, but for developers, i.e. this scm
	 * connection will not be read only. <br />
	 * <b>Default value is</b>: parent value [+ path adjustment] + artifactId
	 */
	public String	developerConnection;

	/**
	 * The tag of current code. By default, it's set to HEAD during development.
	 */
	public String	tag	= "HEAD";

	/**
	 * The URL to the project's browsable SCM repository, such as ViewVC or
	 * Fisheye. <br />
	 * <b>Default value is</b>: parent value [+ path adjustment] + artifactId
	 */
	public URI		url;
}
