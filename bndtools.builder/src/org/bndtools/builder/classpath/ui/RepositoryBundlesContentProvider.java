package org.bndtools.builder.classpath.ui;

import java.util.Collections;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

public class RepositoryBundlesContentProvider implements ITreeContentProvider {

	private static final ILogger logger = Logger.getLogger(RepositoryBundlesContentProvider.class);

	@Override
	public Object[] getElements(Object inputElement) {
		Workspace workspace = (Workspace) inputElement;

		List<RepositoryPlugin> repos = workspace.getPlugins(RepositoryPlugin.class);
		return repos.toArray(new RepositoryPlugin[0]);
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	@Override
	public Object[] getChildren(Object parentElement) {
		RepositoryPlugin repoPlugin = (RepositoryPlugin) parentElement;

		List<String> bsns;
		try {
			bsns = repoPlugin.list(null);
		} catch (Exception e) {
			logger.logError("Error querying repository " + repoPlugin.getName(), e);
			bsns = Collections.emptyList();
		}
		return bsns.toArray(new String[0]);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof RepositoryPlugin;
	}
}
