package bndtools.classpath.ui;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;

public class RepositoryBundlesContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object inputElement) {
        Workspace workspace = (Workspace) inputElement;

        List<RepositoryPlugin> repos = workspace.getPlugins(RepositoryPlugin.class);
        return repos.toArray(new RepositoryPlugin[repos.size()]);
    }

    public void dispose() {}

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

    public Object[] getChildren(Object parentElement) {
        RepositoryPlugin repoPlugin = (RepositoryPlugin) parentElement;

        List<String> bsns;
        try {
            bsns = repoPlugin.list(null);
        } catch (Exception e) {
            Plugin.getDefault().getLogger().logError("Error querying repository " + repoPlugin.getName(), e);
            bsns = Collections.emptyList();
        }
        return bsns.toArray(new String[bsns.size()]);
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof RepositoryPlugin;
    }
}
