package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * Describes the prerequisites a project can have.
 */
public class PrerequisitesDTO extends DTO {

	/**
	 * For a plugin project, the minimum version of Maven required to use the
	 * resulting plugin.<br />
	 * For specifying the minimum version of Maven required to build a project,
	 * this element is <b>deprecated</b>. Use the Maven Enforcer Plugin's
	 * <a href=
	 * "https://maven.apache.org/enforcer/enforcer-rules/requireMavenVersion.html">
	 * <code>requireMavenVersion</code></a> rule instead.
	 */
	public MavenVersion maven = MavenVersion.parseMavenString("2.0");
}
