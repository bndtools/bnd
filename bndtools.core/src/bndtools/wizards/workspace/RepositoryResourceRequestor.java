package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.model.clauses.VersionedClause;
import bndtools.utils.Requestor;

public class RepositoryResourceRequestor implements Requestor<Collection<? extends Resource>> {

    private final RepositoryAdmin repoAdmin;
    private final Collection<? extends IRepositoryIndexProvider> indexProviders;
    private final Collection<? extends VersionedClause> bundles;
    private final Project project;

    public RepositoryResourceRequestor(RepositoryAdmin repoAdmin, Collection<? extends IRepositoryIndexProvider> indexProviders, Collection<? extends VersionedClause> bundles, Project project) {
        this.repoAdmin = repoAdmin;
        this.indexProviders = indexProviders;
        this.bundles = bundles;
        this.project = project;
    }

    void processIndex(IRepositoryIndexProvider indexProvider, Map<String, Resource> urisToResources, IProgressMonitor monitor) throws Exception {
        indexProvider.initialise(monitor);
        for (URL repoUrl : indexProvider.getUrls()) {
            try {
                Repository repo = repoAdmin.addRepository(repoUrl.toExternalForm());
                Resource[] resources = repo.getResources();
                for (Resource resource : resources) {
                    if (!urisToResources.containsKey(resource.getURI()))
                        urisToResources.put(resource.getURI(), resource);
                }
            } finally {
                repoAdmin.removeRepository(repoUrl.toExternalForm());
            }
        }
    }

    public Collection<? extends Resource> request(IProgressMonitor monitor) throws InvocationTargetException {
        Collection<Resource> result = new ArrayList<Resource>(bundles.size());
        try {
            SubMonitor progress = SubMonitor.convert(monitor, indexProviders.size());
            Map<String, Resource> urisToResources = new HashMap<String, Resource>();
            for (IRepositoryIndexProvider indexProvider : indexProviders) {
                processIndex(indexProvider, urisToResources, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            }

            for (VersionedClause bundle : bundles) {
                Container container = project.getBundle(bundle.getName(), bundle.getVersionRange(), Strategy.HIGHEST, null);

                if (container.getType() != TYPE.ERROR) {
                    String uri = container.getFile().toURI().toString();
                    Resource resource = urisToResources.get(uri);
                    if (resource != null) {
                        result.add(resource);
                    }
                }
            }
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
        return result;
    }

}
