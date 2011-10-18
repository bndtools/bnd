package bndtools.wizards.repo;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import bndtools.Central;
import bndtools.bindex.IRepositoryIndexProvider;

public class PluginIndexProvider implements IRepositoryIndexProvider {

    private final List<URL> indexes = new ArrayList<URL>();
    private final EnumSet<OBRResolutionMode> resolutionModes;

    public PluginIndexProvider(EnumSet<OBRResolutionMode> resolutionModes) {
        this.resolutionModes = resolutionModes;
    }

    public void initialise(IProgressMonitor monitor) throws Exception {
        indexes.clear();

        List<OBRIndexProvider> plugins = Central.getWorkspace().getPlugins(OBRIndexProvider.class);
        for (OBRIndexProvider provider : plugins) {
            Set<OBRResolutionMode> supportedModes = provider.getSupportedModes();
            for (OBRResolutionMode mode : resolutionModes) {
                if (supportedModes.contains(mode)) {
                    indexes.addAll(provider.getOBRIndexes());
                    break;
                }
            }
        }
    }

    public URL[] getUrls() {
        return indexes.toArray(new URL[indexes.size()]);
    }

}
