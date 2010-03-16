package bndtools.classpath.ui;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

public class RepositoryBundlesContentProvider implements ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		Workspace workspace = (Workspace) inputElement;
		
		List<RepositoryPlugin> repos = workspace.getPlugins(RepositoryPlugin.class);
		return (RepositoryPlugin[]) repos.toArray(new RepositoryPlugin[repos.size()]);
	}
	public void dispose() {
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	public Object[] getChildren(Object parentElement) {
		RepositoryPlugin repoPlugin = (RepositoryPlugin) parentElement;
		
		List<String> bsns = repoPlugin.list(null);
		return (String[]) bsns.toArray(new String[bsns.size()]);
	}
	public Object getParent(Object element) {
		return null;
	}
	public boolean hasChildren(Object element) {
		return element instanceof RepositoryPlugin;
	}
}
