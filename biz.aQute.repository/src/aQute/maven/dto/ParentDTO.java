package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * The <code>&lt;parent&gt;</code> element contains information required to
 * locate the parent project from which this project will inherit from.
 * <strong>Note:</strong> The children of this element are not interpolated and
 * must be given as literal values.
 */
public class ParentDTO extends DTO {
	/**
	 * The group id of the parent project to inherit from.
	 */
	public String		groupId;

	/**
	 * The artifact id of the parent project to inherit from.
	 */
	public String		artifactId;

	/**
	 * The version of the parent project to inherit.
	 */
	public MavenVersion	version;

	/**
	 *
	 */
	public String		relativePath	= "../pom.xml";

}
