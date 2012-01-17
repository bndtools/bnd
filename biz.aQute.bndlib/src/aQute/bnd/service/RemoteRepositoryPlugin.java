package aQute.bnd.service;

import java.io.*;
import java.util.*;

public interface RemoteRepositoryPlugin extends RepositoryPlugin {
	/**
	 * Retrieve a resource handle from the repository. For all implementations of this interface, calling {@code getFile(bsn, range, strategy, props)}
	 * should always return the same result as {@code getResource(bsn, range, strategy, props).request()}.
	 * @param bsn
	 * @param range
	 * @param strategy
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	ResourceHandle getHandle(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception;
	
	File getCacheDirectory();
}
