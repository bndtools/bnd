package bndtools.bindex;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.service.obr.Resource;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import bndtools.model.clauses.VersionedClause;

public class BundleSelectionIndexer extends AbstractIndexer {

    private static final String CATEGORY = "__temporary_selection__";

    private List<? extends VersionedClause> selection = null;

    private final Project project;

    public BundleSelectionIndexer(Project project) {
        this.project = project;

    }

    @Override
    protected String getTaskLabel() {
        return "Indexing selected bundles...";
    }

    @Override
    protected void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception {
        if (selection != null && project != null) {
            int work = selection.size();
            SubMonitor progress = SubMonitor.convert(monitor, work);
            for (VersionedClause bundle : selection) {
                Container container = project.getBundle(bundle.getName(), bundle.getVersionRange(), Strategy.HIGHEST, null);

                if (container.getType() != TYPE.ERROR) {
                    File file = container.getFile();
                    BundleInfo info = new BundleInfo(bindex, file);
                    ResourceImpl resource = info.build();

                    resource.addCategory(CATEGORY);

                    result.add(resource);
                    progress.worked(1);
                }
            }
            progress.setWorkRemaining(--work);
        }
    }

    public void setSelection(List<? extends VersionedClause> selection) {
        this.selection = selection;
        reset();
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

}
