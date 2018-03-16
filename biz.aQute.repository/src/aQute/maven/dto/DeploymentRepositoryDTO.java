package aQute.maven.dto;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

/**
 * Repository contains the information needed for deploying to the remote
 * repository.
 */
public class DeploymentRepositoryDTO extends DTO {
	/**
	 * Whether to assign snapshots a unique version comprised of the timestamp
	 * and build number, or to use the same version each time
	 */
	public boolean				uniqueVersion	= true;

	/**
	 * How to handle downloading of releases from this repository.
	 */
	public RepositoryPolicyDTO	releases;

	/**
	 * How to handle downloading of snapshots from this repository.
	 */
	public RepositoryPolicyDTO	snapshots;

	/**
	 * A unique identifier for a repository. This is used to match the
	 * repository to configuration in the <code>settings.xml</code> file, for
	 * example. Furthermore, the identifier is used during POM inheritance and
	 * profile injection to detect repositories that should be merged.
	 */
	public String				id;

	/**
	 * Human readable name of the repository.
	 */
	public String				name;

	/**
	 * The url of the repository, in the form
	 * <code>protocol://hostname/path</code>.
	 */
	public URI					url;

	/**
	 * The type of layout this repository uses for locating and storing
	 * artifacts - can be <code>legacy</code> or <code>default</code>.
	 */
	public String				layout			= "default";

}
