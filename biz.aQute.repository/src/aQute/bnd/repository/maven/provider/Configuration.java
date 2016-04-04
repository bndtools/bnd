package aQute.bnd.repository.maven.provider;

import java.util.Set;

public interface Configuration {

	/**
	 * The url to the remote release repository. If this is not specified,
	 * the repository is only local.
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

	/**
	 * The classifiers to release. These will be generated automatically if
	 * sufficient information is available.
	 */
	// default { Classifiers.BINARY}
	Set<Classifiers> generate();

	// default false
	boolean readOnly();

	String name(String deflt);

	String index(String deflt);
}