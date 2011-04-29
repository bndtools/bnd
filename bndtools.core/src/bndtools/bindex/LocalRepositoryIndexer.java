package bndtools.bindex;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.service.obr.Resource;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.libg.version.Version;
import bndtools.Central;

public class LocalRepositoryIndexer extends AbstractIndexer {

    public static final String CATEGORY = "__local_repo__";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    protected void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception {
        List<RepositoryPlugin> bndRepos = Central.getWorkspace().getRepositories();
        if (bndRepos != null) {
            processRepos(bndRepos, bindex, result, monitor);
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
            List<Version> versions = bndRepo.versions(bsn);
            if (versions != null) {
                processRepoVersions(bndRepo, bsn, versions, bindex, resources, progress.newChild(1));
            }
        }
    }


    private void processRepoVersions(RepositoryPlugin bndRepo, String bsn, List<Version> versions, RepositoryImpl bindex, List<Resource> resources, SubMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, versions.size());
        for (Version version : versions) {
            try {
                File bundleFile = bndRepo.get(bsn, version.toString(), Strategy.HIGHEST, null);
                BundleInfo info = new BundleInfo(bindex, bundleFile);
                ResourceImpl resource = info.build();
                if (isValidRuntimeBundle(resource)) {
                    resource.setURL(bundleFile.toURI().toURL());
                    resource.addCategory(CATEGORY);
                    resources.add(resource);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                progress.worked(1);
            }
        }
    }

    @Override
    protected String getTaskLabel() {
        return "Indexing local repository";
    }
}
