package bndtools;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.util.tracker.ServiceTracker;

public class ResourceIndexerTracker extends ServiceTracker implements ResourceIndexer {

    private final long timeout;

    public ResourceIndexerTracker(BundleContext context, long timeout) {
        super(context, ResourceIndexer.class.getName(), null);
        this.timeout = timeout;
    }

    public void index(Set<File> files, OutputStream out, Map<String, String> config) throws Exception {
        ResourceIndexer indexer = doGetIndexer();
        indexer.index(files, out, config);
    }

    public void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception {
        ResourceIndexer indexer = doGetIndexer();
        indexer.indexFragment(files, out, config);
    }

    private ResourceIndexer doGetIndexer() throws InterruptedException {
        ResourceIndexer indexer = (ResourceIndexer) waitForService(timeout);
        if (indexer == null)
            throw new IllegalStateException("Resource indexer service not available.");
        return indexer;
    }

}
