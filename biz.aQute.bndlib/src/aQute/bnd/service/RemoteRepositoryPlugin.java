package aQute.bnd.service;

import java.io.*;
import java.util.*;

public interface RemoteRepositoryPlugin extends RepositoryPlugin {
	/**
	 * Retrieve a resource handle from the repository. For all implementations
	 * of this interface, calling {@code getFile(bsn, range, strategy, props)}
	 * should always return the same result as {@link RepositoryPlugin#get(String, aQute.bnd.version.Version, Map)}
	 * 
	 * @param bsn the bsn of the revision
	 * @param version the version of the revision
	 * @param strategy strategy
	 * @param properties any properties
	 * @return
	 * @throws Exception
	 */
	ResourceHandle getHandle(String bsn, String version, Strategy strategy, Map<String,String> properties)
			throws Exception;

	File getCacheDirectory();
}
