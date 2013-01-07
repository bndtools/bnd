package bndtools.model.repo;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.Logger;
import bndtools.api.ILogger;

public class RepositoryBundleVersion implements IAdaptable, Actionable {
    private static final ILogger logger = Logger.getLogger();

    private final Version version;
    private final RepositoryBundle bundle;

    public RepositoryBundleVersion(RepositoryBundle bundle, Version version) {
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
        return "RepositoryBundleVersion [version=" + version + ", bundle=" + bundle + "]";
    }

    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        Object result = null;

        File file = getFile();
        if (file != null) {
            if (IFile.class.equals(adapter)) { // ||
                // IResource.class.equals(adapter))
                // {
                // Note that if the file is outside the workspace the IFile result will be null
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                result = root.getFileForLocation(new Path(file.getAbsolutePath()));
            } else if (File.class.equals(adapter)) {
                result = file;
            } else if (URI.class.equals(adapter)) {
                result = file.toURI();
            }
        }

        return result;
    }

    private File getFile() {
        try {
            return bundle.getRepo().get(bundle.getBsn(), version, null);
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Failed to query repository {0} for bundle {1} version {2}.", bundle.getRepo().getName(), bundle.getBsn(), version), e);
            return null;
        }
    }

    public Map<String,Runnable> actions(Object... t) throws Exception {
        RepositoryPlugin p = bundle.getRepo();
        if (p instanceof Actionable)
            return ((Actionable) p).actions(bundle.getBsn(), version);
        return null;
    }

    public String tooltip(Object... target) throws Exception {
        RepositoryPlugin p = bundle.getRepo();
        if (p instanceof Actionable)
            return ((Actionable) p).tooltip(bundle.getBsn(), version);
        return null;
    }

    public String title(Object... target) throws Exception {
        RepositoryPlugin p = bundle.getRepo();
        if (p instanceof Actionable)
            return ((Actionable) p).title(bundle.getBsn(), version);
        return null;
    }
}
