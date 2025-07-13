package aQute.bnd.repository.maven.provider;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Configuration {

	/**
	 * The urls to the remote release repository.
	 */
	String releaseUrl();

	/**
	 * The url of the staging release repository.
	 */
	String stagingUrl();

	/**
	 * The urls to the remote snapshot repository.
	 */
	String snapshotUrl();

	/**
	 * The path to the local repository
	 */
	// default "~/.m2/repository"
	String local(String deflt);

	// default false
	boolean readOnly();

	/**
	 * The name of this repository
	 *
	 * @param deflt
	 */
	String name(String deflt);

	/**
	 * The path to the index file
	 *
	 * @param deflt
	 */
	String index(String deflt);

	/**
	 * Content added to the index file. Content maybe one line without CR/LF as
	 * long as there is a comma or whitespace separating the GAVs. Further same
	 * format as the index file.
	 */
	String source();

	/**
	 * Do not update the index when a file is released
	 */
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

	/**
	 * Extensions for files that contain multiple JARs
	 */
	String multi();

	/**
	 * @return a comma separated list of tags.
	 */
	String tags();

	/**
	 * @return if Sonatype releases should be autopublished
	 */
	boolean autopublish();
}
