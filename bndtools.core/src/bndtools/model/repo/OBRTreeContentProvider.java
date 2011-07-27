package bndtools.model.repo;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.framework.InvalidSyntaxException;

import aQute.bnd.service.OBRIndexProvider;
import bndtools.Plugin;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.wizards.repo.DummyBundleContext;

public class OBRTreeContentProvider implements ITreeContentProvider {

    final Map<Object, RepositoryAdmin> repoMap = new IdentityHashMap<Object, RepositoryAdmin>();
    Object[] providers;

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        repoMap.clear();

        if (newInput instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> coll = (Collection<Object>) newInput;
            providers = coll.toArray();
        } else if (newInput instanceof Object[]) {
            providers = (Object[]) newInput;
        } else {
            throw new IllegalArgumentException("Invalid input element type");
        }

        for (Object provider : providers) {
            URL[] indexUrls = new URL[0];
            if (provider instanceof OBRIndexProvider) {
                try {
                    Collection<URL> coll = ((OBRIndexProvider) provider).getOBRIndexes();
                    indexUrls = coll.toArray(new URL[coll.size()]);
                } catch (IOException e) {
                    Plugin.logError("Error getting repository index URLs.", e);
                }
            } else if (provider instanceof IRepositoryIndexProvider)
                indexUrls = ((IRepositoryIndexProvider) provider).getUrls();
            else
                continue;

            DummyBundleContext bc = new DummyBundleContext();
            RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bc, new Logger(bc));
            for (URL indexUrl : indexUrls) {
                try {
                    repoAdmin.addRepository(indexUrl);
                } catch (Exception e) {
                    Plugin.logError("Error adding index URL to repository", e);
                }
            }
            repoMap.put(provider, repoAdmin);
        }
    }

    public Object[] getElements(Object inputElement) {
        return providers;
    }

    public void dispose() {
    }


    public Object[] getChildren(Object parentElement) {
        Object[] result = new Object[0];
        try {
            RepositoryAdmin repoAdmin = repoMap.get(parentElement);
            if (repoAdmin == null)
                result = null;
            else
                result = repoAdmin.discoverResources("");
        } catch (InvalidSyntaxException e) {
            // Can't happen?
        }
        return result;
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        return !(element instanceof Resource);
    }

}
