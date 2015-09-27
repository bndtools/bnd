package org.bndtools.templating.load;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;

public class RepoPluginsBundleLocator implements BundleLocator {
	
	private final List<RepositoryPlugin> plugins;

	public RepoPluginsBundleLocator(List<RepositoryPlugin> plugins) {
		this.plugins = plugins;
	}

	@Override
	public File locate(String bsn, String hash, String algo) throws Exception {
		Map<String, String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", algo + ":" + hash);
		
		for (RepositoryPlugin plugin : plugins) {
			try {
				File file = plugin.get(bsn, null, searchProps);
				if (file != null) {
					return file;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return null;
	}

}
