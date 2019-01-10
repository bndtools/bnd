package org.bndtools.core.templating.repobased;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;

public class RepoPluginsBundleLocator implements BundleLocator {

    private final List<RepositoryPlugin> plugins;

    public RepoPluginsBundleLocator(List<RepositoryPlugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public File locate(String bsn, String hash, String algo, URI location) throws Exception {
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

        // Fall back to direct download
        // TODO: need some kind of download/cache service to avoid repeated downloads
        File tempFile = File.createTempFile("download", "jar");
        tempFile.deleteOnExit();

        IO.copy(location.toURL(), tempFile);
        return tempFile;
    }

}
