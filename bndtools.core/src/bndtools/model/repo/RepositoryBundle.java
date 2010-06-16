package bndtools.model.repo;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;

public class RepositoryBundle implements IAdaptable {

	private final RepositoryPlugin repo;
	private final String bsn;

	RepositoryBundle(RepositoryPlugin repo, String bsn) {
		this.repo = repo;
		this.bsn = bsn;
	}
	public RepositoryPlugin getRepo() {
		return repo;
	}
	public String getBsn() {
		return bsn;
	}
	@Override
	public String toString() {
		return "RepositoryBundle [repo=" + repo + ", bsn=" + bsn + "]";
	}
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        Object result = null;

        if(IFile.class.equals(adapter) || IResource.class.equals(adapter)) {
            try {
                File file = getFile();
                if(file != null) {
                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                    result = root.getFileForLocation(new Path(file.getAbsolutePath()));
                }
            } catch (Exception e) {
                Plugin.logError(MessageFormat.format("Failed to query repository {0} for bundle {1}.", repo.getName(), bsn), e);
            }
        } else if(File.class.equals(adapter)) {
            result = getFile();
        }

        return result;
    }

    private File getFile() {
        File[] files = null;
        try {
            files = repo.get(bsn, "latest");
        } catch (Exception e) {
            Plugin.logError(MessageFormat.format("Failed to query repository {0} for bundle {1}.", repo.getName(), bsn), e);
        }
        if(files != null && files.length > 0) {
            return files[files.length - 1];
        }

        return null;
    }
}

