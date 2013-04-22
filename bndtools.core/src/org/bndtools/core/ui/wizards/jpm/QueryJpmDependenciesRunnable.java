package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.lib.hex.Hex;
import bndtools.Central;

public class QueryJpmDependenciesRunnable implements IRunnableWithProgress {

    private final URI origin;

    private String error = null;
    private Set<ResourceDescriptor> directResources;
    private Set<ResourceDescriptor> indirectResources;

    public QueryJpmDependenciesRunnable(URI origin) {
        this.origin = origin;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        SubMonitor progress = SubMonitor.convert(monitor, 10);
        progress.setTaskName("Querying dependencies...");

        Workspace workspace;
        try {
            workspace = Central.getWorkspace();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
        progress.worked(1);

        SearchableRepository repository = workspace.getPlugin(SearchableRepository.class);
        if (repository == null) {
            error = "No searchable repository is configured in the workspace. Try adding the JPM4J plugin.";
            return;
        }
        progress.worked(1);

        try {
            directResources = repository.getResources(origin, false);
            progress.worked(4);

            indirectResources = repository.getResources(origin, true);
            removeDirectResourcesFromIndirect();
            progress.worked(4);
        } catch (Exception e) {
            error = "Error searching repository: " + e.getMessage();
        }
    }

    private void removeDirectResourcesFromIndirect() {
        Set<String> ids = new HashSet<String>();

        for (ResourceDescriptor descriptor : directResources) {
            ids.add(Hex.toHexString(descriptor.id));
        }

        for (Iterator<ResourceDescriptor> iter = indirectResources.iterator(); iter.hasNext();) {
            ResourceDescriptor descriptor = iter.next();
            if (ids.contains(Hex.toHexString(descriptor.id)))
                iter.remove();
        }
    }

    public String getError() {
        return error;
    }

    public Set<ResourceDescriptor> getDirectResources() {
        return directResources;
    }

    public Set<ResourceDescriptor> getIndirectResources() {
        return indirectResources;
    }

}
