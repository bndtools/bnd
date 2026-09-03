package aQute.bnd.repository.p2.provider;

import java.net.URI;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Configuration for P2 repositories.
 */
@ProviderType
public interface P2Config {
	/**
	 * Name of the repository
	 *
	 * @param defaultName will be used if no name set
	 */
	String name(String defaultName);

	/**
	 * The URL to either the P2 repository (a directory) or an Eclipse target
	 * platform
	 */
	URI url();

	/**
	 * The location to store the index file and downloaded bundles. A location ending
	 * in {@code /} is a directory in which {@code index.xml.gz} is stored. Otherwise,
	 * the location specifies the complete index filename and bundles are stored in
	 * its parent directory.
	 */
	String location();

	/**
	 * The location to store the index file and downloaded bundles with a default
	 * passed. A location ending in {@code /} is a directory in which
	 * {@code index.xml.gz} is stored. Otherwise, the location specifies the complete
	 * index filename and bundles are stored in its parent directory.
	 */
	String location(String string);

	/**
	 * @return a comma separated list of tags.
	 */
	String tags();

	/**
	 * If not set or false, this assumes a P2 repository, i.e. the url points to
	 * a P2 repository directory. If set to true, the url is assumed to point to
	 * an Eclipse Target platform.
	 *
	 * @deprecated
	 */
	@Deprecated
	boolean targetPlatform();
}
