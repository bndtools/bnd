package aQute.bnd.service;

import java.io.File;
import java.util.Map;

public interface RemoteRepositoryPlugin extends RepositoryPlugin {
	/**
	 * Retrieve a resource handle from the repository. For all implementations
	 * of this interface, calling {@code getFile(bsn, range, strategy, props)}
	 * should always return the same result as
	 * {@link RepositoryPlugin#get(String, aQute.bnd.version.Version, Map, aQute.bnd.service.RepositoryPlugin.DownloadListener...)}
	 *
	 * @param bsn the bsn of the revision
	 * @param version the version of the revision
	 * @param strategy strategy
	 * @param properties any properties
	 * @return a {@link ResourceHandle}
	 * @throws Exception
	 */
	ResourceHandle getHandle(String bsn, String version, Strategy strategy, Map<String, String> properties)
		throws Exception;

	File getCacheDirectory();
}
