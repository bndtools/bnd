package bndtools.bindex;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.service.obr.Resource;

public class FileIndexer extends AbstractIndexer {

    private final File[] files;
    private final String category;

    public FileIndexer(File[] files, String category) {
        this.files = files;
        this.category = category;
    }

    @Override
    protected String getTaskLabel() {
        return "Indexing files";
    }

    @Override
    protected void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, files.length);

        for (File file : files) {
            BundleInfo info = new BundleInfo(bindex, file);
            ResourceImpl resource = info.build();

            if (category != null)
                resource.addCategory(category);
            result.add(resource);
            progress.worked(1);
        }
    }

    @Override
    public String getCategory() {
        return category;
    }

}
