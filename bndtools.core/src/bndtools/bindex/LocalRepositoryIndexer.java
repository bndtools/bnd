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
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import bndtools.Central;

public class LocalRepositoryIndexer extends AbstractIndexer {

    private static final String CATEGORY = "__local_repo__";

    private final boolean includeWorkspace;

    private String workspaceCategory = null;

    public LocalRepositoryIndexer(boolean includeWorkspace) {
        this.includeWorkspace = includeWorkspace;
    }

    public void setWorkspaceCategory(String workspaceCategory) {
        this.workspaceCategory = workspaceCategory;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    protected void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, 6);

        List<RepositoryPlugin> bndRepos = Central.getWorkspace().getRepositories();
        if (bndRepos != null) {
            processRepos(bndRepos, bindex, result, progress.newChild(4));
        }
        progress.setWorkRemaining(2);

        if (includeWorkspace) {
            Collection<Project> projects = Central.getWorkspace().getAllProjects();
            processProjects(projects, bindex, result, progress.newChild(2));
        }
    }

    private void processRepos(List<RepositoryPlugin> bndRepos, RepositoryImpl bindex, List<Resource> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, bndRepos.size());
        for (RepositoryPlugin bndRepo : bndRepos) {
            List<String> bsns = bndRepo.list(null);
            if (bsns != null) {
                processRepoBundles(bndRepo, bsns, bindex, resources, progress.newChild(1));
            }
        }
    }

    private void processRepoBundles(RepositoryPlugin bndRepo, List<String> bsns, RepositoryImpl bindex, List<Resource> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, bsns.size());
        for (String bsn : bsns) {
            File[] files = bndRepo.get(bsn, null);
            if (files != null) {
                processRepoFiles(files, bindex, resources, progress.newChild(1));
            }
        }
    }

    private void processRepoFiles(File[] files, RepositoryImpl bindex, List<Resource> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, files.length);
        for (File bundleFile : files) {
            if (bundleFile.getName().toLowerCase().endsWith(".jar")) {
                BundleInfo info = new BundleInfo(bindex, bundleFile);
                ResourceImpl resource = info.build();
                if (isValidRuntimeBundle(resource)) {
                    resource.setURL(bundleFile.toURI().toURL());
                    resource.addCategory(CATEGORY);
                    resources.add(resource);
                }
            }
            progress.worked(1);
        }
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
            if (isValidRuntimeBundle(resource)) {
                resource.setURL(bundleFile.toURI().toURL());

                resource.addCategory(CATEGORY);
                if(workspaceCategory != null)
                    resource.addCategory(workspaceCategory);
                resources.add(resource);
            }
            progress.worked(1);
        }
    }

    @Override
    protected String getTaskLabel() {
        return "Indexing local repository";
    }
}
