package bndtools.wizards.repo;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.service.OBRIndexProvider;
import bndtools.Central;
import bndtools.bindex.IRepositoryIndexProvider;

public class PluginIndexProvider implements IRepositoryIndexProvider {

    private final List<URL> indexes = new ArrayList<URL>();

    public void initialise(IProgressMonitor monitor) throws Exception {
        indexes.clear();

        List<OBRIndexProvider> plugins = Central.getWorkspace().getPlugins(OBRIndexProvider.class);
        for (OBRIndexProvider provider : plugins) {
            indexes.addAll(provider.getOBRIndexes());
        }
    }

    public URL[] getUrls() {
        return indexes.toArray(new URL[indexes.size()]);
    }

}
