package bndtools.model.repo;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;

import aQute.libg.version.Version;
import bndtools.Plugin;

public class RepositoryBundleVersion implements IAdaptable {

	private final Version version;
	private final RepositoryBundle bundle;

	RepositoryBundleVersion(RepositoryBundle bundle, Version version) {
		this.bundle = bundle;
		this.version = version;
	}
	public Version getVersion() {
		return version;
	}
	public RepositoryBundle getBundle() {
		return bundle;
	}
	@Override
	public String toString() {
		return "RepositoryBundleVersion [version=" + version + ", bundle="
				+ bundle + "]";
	}

    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        Object result = null;

        if(IFile.class.equals(adapter)) { // || IResource.class.equals(adapter)) {
            File file = getFile();
            if(file != null) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                result = root.getFileForLocation(new Path(file.getAbsolutePath()));
            }
        } else if(File.class.equals(adapter)) {
            result = getFile();
        }

        return result;
    }

    private File getFile() {
        StringBuilder range = new StringBuilder();
        range.append("[");
        range.append(version).append(',').append(version);
        range.append("]");

        File[] files = null;
        try {
            files = bundle.getRepo().get(bundle.getBsn(), range.toString());
        } catch (Exception e) {
            Plugin.logError(MessageFormat.format("Failed to query repository {0} for bundle {1} version {2}.", bundle.getRepo().getName(), bundle.getBsn(), version), e);
        }
        if(files != null && files.length > 0) {
            return files[files.length - 1];
        }

        return null;
    }
}
