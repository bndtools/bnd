package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import bndtools.bindex.AbstractIndexer;
import bndtools.model.clauses.VersionedClause;
import bndtools.utils.Requestor;

public class BundleResourceRequestor implements Requestor<Collection<? extends Resource>> {

    private final RepositoryAdmin repoAdmin;
    private final AbstractIndexer indexer;
    private final Collection<? extends VersionedClause> bundles;
    private final Project project;

    public BundleResourceRequestor(RepositoryAdmin repoAdmin, AbstractIndexer indexer, Collection<? extends VersionedClause> bundles, Project project) {
        this.repoAdmin = repoAdmin;
        this.indexer = indexer;
        this.bundles = bundles;
        this.project = project;
    }

    public Collection<? extends Resource> request(IProgressMonitor monitor) throws InvocationTargetException {
        Collection<Resource> result = new ArrayList<Resource>(bundles.size());
        try {
            indexer.initialise(monitor);
            Repository repo = repoAdmin.addRepository(indexer.getUrl().toExternalForm());

            Resource[] resources = repo.getResources();
            Map<String, Resource> urisToResources = new HashMap<String, Resource>();
            for (Resource resource : resources) {
                urisToResources.put(resource.getURI(), resource);
            }

            for (VersionedClause bundle : bundles) {
                Container container = project.getBundle(bundle.getName(), bundle.getVersionRange(), Project.STRATEGY_HIGHEST, null);

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
        } finally {
            repoAdmin.removeRepository(indexer.getUrl().toExternalForm());
        }
        return result;
    }

}
