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
	 * @param will be used if no name set
	 */
	String name(String defaultName);

	/**
	 * The URL to either the P2 repository (a directory) or an Eclipse target
	 * platform
	 */
	URI url();

	/**
	 * The location to store the index file.
	 */
	String location();

	/**
	 * The location to store the index file with a default passed.
	 */
	String location(String string);

	/**
	 * If not set or false, this assumes a P2 repository, i.e. the url points to
	 * a P2 repository directory. If set to true, the url is assumed to point to
	 * an Eclipse Target platform.
	 */
	boolean targetPlatform();
}
