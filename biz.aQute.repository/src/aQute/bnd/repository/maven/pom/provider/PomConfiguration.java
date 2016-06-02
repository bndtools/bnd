package aQute.bnd.repository.maven.pom.provider;

public interface PomConfiguration {

	/**
	 * The url to the remote release repository.
	 */
	String releaseUrls();

	/**
	 * The url to the remote snapshot repository. If this is not specified, it
	 * falls back to the release repository or just local if this is also not
	 * specified.
	 */
	String snapshotUrls();

	/**
	 * The path to the local repository
	 */
	// default "~/.m2/repository"
	String local(String deflt);

	String revision();

	String location(String deflt);
}
