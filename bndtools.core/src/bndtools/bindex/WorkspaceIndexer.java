package bndtools.bindex;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.service.obr.Resource;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import bndtools.Central;

public class WorkspaceIndexer extends AbstractIndexer {

    private final String category;

    public WorkspaceIndexer(String category) {
        this.category = category;
    }

    @Override
    protected String getTaskLabel() {
        return "Indexing workspace";
    }

    @Override
    protected void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception {
        Collection<Project> projects = Central.getWorkspace().getAllProjects();
        processProjects(projects, bindex, result, monitor);
    }


    @Override
    public String getCategory() {
        return category;
    }

    private void processProjects(Collection<Project> projects, RepositoryImpl bindex, List<Resource> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, projects.size());
        for (Project project : projects) {
            Collection<? extends Builder> builders = project.getSubBuilders();
            processProjectBuilders(project, builders, bindex, resources, progress.newChild(1));
        }
    }

    private void processProjectBuilders(Project project, Collection<? extends Builder> builders, RepositoryImpl bindex, List<Resource> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, builders.size());
        for (Builder builder : builders) {
            File bundleFile = new File(project.getTarget(), builder.getBsn() + ".jar");
            BundleInfo info = new BundleInfo(bindex, bundleFile);
            ResourceImpl resource = info.build();

            resource.setURL(bundleFile.toURI().toURL());
            resource.addCategory(category);
            customizeResourceEntry(resource);

            resources.add(resource);
            progress.worked(1);
        }
    }

}
