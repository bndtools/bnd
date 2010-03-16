package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.libg.version.Version;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		List<RepositoryPlugin> plugins = ((Workspace) inputElement).getPlugins(RepositoryPlugin.class);
		
		return (RepositoryPlugin[]) plugins.toArray(new RepositoryPlugin[plugins.size()]);
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
		return element instanceof RepositoryPlugin || element instanceof RepositoryBundle;
	}

}
