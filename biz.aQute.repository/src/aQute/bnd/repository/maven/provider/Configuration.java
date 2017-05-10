package aQute.bnd.repository.maven.provider;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Configuration {

	/**
	 * The url to the remote release repository.
	 */
	String releaseUrl();

	/**
	 * The url to the remote snapshot repository. If this is not specified,
	 * it falls back to the release repository or just local if this is also
	 * not specified.
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
}