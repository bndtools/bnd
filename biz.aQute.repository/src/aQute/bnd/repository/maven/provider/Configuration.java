package aQute.bnd.repository.maven.provider;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Configuration {

	/**
	 * The url to the remote release repository.
	 */
	String releaseUrl();

	/**
	 * The url to the remote snapshot repository.
	 */
	String snapshotUrl();

	/**
	 * The path to the local repository
	 */
	// default "~/.m2/repository"
	String local(String deflt);

	// default false
	boolean readOnly();

	String name(String deflt);

	String index(String deflt);

	boolean noupdateOnRelease();

	/**
	 * Sets the time in seconds when to check for changes in the pom-files
	 */
	// default: 5 seconds
	int poll_time(int pollTimeInSecs);

	/**
	 * Allow redeploy
	 */
	boolean redeploy();

	/**
	 * Ignore maven information in META-INF/maven/....
	 */

	boolean ignore_metainf_maven();

}
