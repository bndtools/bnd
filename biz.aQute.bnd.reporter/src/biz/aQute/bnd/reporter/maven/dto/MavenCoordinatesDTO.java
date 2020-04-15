package biz.aQute.bnd.reporter.maven.dto;

import org.osgi.dto.DTO;

/**
 * A representation of a maven coordinates.
 */
public class MavenCoordinatesDTO extends DTO {

	/**
	 * The groupId of the maven artifact.
	 * <p>
	 * </p>
	 */
	public String	groupId;
	/**
	 * The artifactId of the maven artifact.
	 * <p>
	 * </p>
	 */
	public String artifactId;

	/**
	 * The version of the maven artifact.(optional)
	 * <p>
	 * </p>
	 */
	public String	version;

	/**
	 * The type of the maven artifact.(optional)
	 * <p>
	 * </p>
	 */
	public String	type;

	/**
	 * The classifier of the maven artifact.(optional)
	 * <p>
	 * </p>
	 */
	public String	classifier;


}
