package bndtools.model.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import aQute.libg.version.Version;
import bndtools.Plugin;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		List<Object> result = new ArrayList<Object>();
		Workspace workspace = (Workspace) inputElement;
		
		result.addAll(workspace.getPlugins(RepositoryPlugin.class));
		try {
			result.addAll(workspace.getAllProjects());
		} catch (Exception e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying workspace Bnd projects.", e));
		}
		
		return result.toArray(new Object[result.size()]);
	}
	public void dispose() {
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	public Object[] getChildren(Object parentElement) {
		Object[] result = null;
		
		if(parentElement instanceof RepositoryPlugin) {
			RepositoryPlugin repo = (RepositoryPlugin) parentElement;
			List<String> bsns = repo.list(null);
			if(bsns != null) {
				result = new Object[bsns.size()];
				int i=0; for (String bsn : bsns) {
					result[i++] = new RepositoryBundle(repo, bsn);
				} 
			}
		} else if(parentElement instanceof RepositoryBundle) {
			RepositoryBundle bundle = (RepositoryBundle) parentElement;
			List<Version> versions = bundle.getRepo().versions(bundle.getBsn());
			if(versions != null) {
				result = new Object[versions.size()];
				int i=0; for(Version version : versions) {
					result[i++] = new RepositoryBundleVersion(bundle, version);
				}
			}
		} else if(parentElement instanceof Project) {
			Project project = (Project) parentElement;
			try {
                Collection<? extends Builder> builders = project.getSubBuilders();
				result = new Object[builders.size()];
				
				int i = 0;
				for (Builder builder : builders) {
					ProjectBundle bundle = new ProjectBundle(project, builder.getBsn());
					result[i++] = bundle;
				}
			} catch (Exception e) {
				Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Error querying sub-bundles for project %s.", project.getName()), e));
			}
		}
		
		return result;
	}
	public Object getParent(Object element) {
		if(element instanceof RepositoryBundle) {
			return ((RepositoryBundle) element).getRepo();
		}
		if(element instanceof RepositoryBundleVersion) {
			return ((RepositoryBundleVersion) element).getBundle();
		}
		return null;
	}
	public boolean hasChildren(Object element) {
		return element instanceof RepositoryPlugin || element instanceof RepositoryBundle || element instanceof Project;
	}

}
