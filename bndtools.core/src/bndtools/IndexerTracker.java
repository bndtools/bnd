package bndtools;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.bindex.BundleIndexer;
import org.osgi.util.tracker.ServiceTracker;

public class IndexerTracker extends ServiceTracker<BundleIndexer,BundleIndexer> implements BundleIndexer {
    public IndexerTracker(BundleContext context) {
        super(context, BundleIndexer.class.getName(), null);
    }

    public void index(Set<File> jarFiles, OutputStream out, Map<String,String> config) throws Exception {
        BundleIndexer service = waitForService(500);
        if (service == null)
            throw new IllegalStateException("Bundle Indexer service is not available.");

        service.index(jarFiles, out, config);
    }
}