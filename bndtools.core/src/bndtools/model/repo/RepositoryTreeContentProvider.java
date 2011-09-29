package bndtools.model.repo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import aQute.libg.version.Version;
import bndtools.Plugin;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

    private static final String CACHE_REPOSITORY = "cache";

    private final OBRResolutionMode[] modes;

    public RepositoryTreeContentProvider() {
        this.modes = OBRResolutionMode.values();
    }

    public RepositoryTreeContentProvider(OBRResolutionMode mode) {
        this.modes = new OBRResolutionMode[] { mode };
    }

    public RepositoryTreeContentProvider(OBRResolutionMode[] modes) {
        this.modes = modes;
    }

    public Object[] getElements(Object inputElement) {
        List<Object> result = new ArrayList<Object>();
        Workspace workspace = (Workspace) inputElement;

        addRepositoryPlugins(result, workspace);
        addProjects(result, workspace);

//        Collections.sort(result, repositoryComparator);

        return result.toArray(new Object[result.size()]);
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getChildren(Object parentElement) {
        Object[] result = null;

        if (parentElement instanceof RepositoryPlugin) {
            RepositoryPlugin repo = (RepositoryPlugin) parentElement;
            result = getRepositoryBundles(repo);
        } else if (parentElement instanceof RepositoryBundle) {
            RepositoryBundle bundle = (RepositoryBundle) parentElement;
            result = getRepositoryBundleVersions(bundle);
        } else if (parentElement instanceof Project) {
            Project project = (Project) parentElement;
            result = getProjectBundles(project);
        }

        return result;
    }

    public Object getParent(Object element) {
        if (element instanceof RepositoryBundle) {
            return ((RepositoryBundle) element).getRepo();
        }
        if (element instanceof RepositoryBundleVersion) {
            return ((RepositoryBundleVersion) element).getBundle();
        }
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof RepositoryPlugin || element instanceof RepositoryBundle || element instanceof Project;
    }

    void addRepositoryPlugins(List<Object> result, Workspace workspace) {
        workspace.getErrors().clear();
        List<RepositoryPlugin> repoPlugins = workspace.getPlugins(RepositoryPlugin.class);
        for (String error : workspace.getErrors()) {
            Plugin.logError(error, null);
        }
        for (RepositoryPlugin repoPlugin : repoPlugins) {
            if (CACHE_REPOSITORY.equals(repoPlugin.getName()))
                continue;
            if (repoPlugin instanceof OBRIndexProvider) {
                OBRIndexProvider indexProvider = (OBRIndexProvider) repoPlugin;
                if (!supportsMode(indexProvider))
                    continue;
            }
            result.add(repoPlugin);
        }
    }

    boolean supportsMode(OBRIndexProvider provider) {
        Set<OBRResolutionMode> supportedModes = provider.getSupportedModes();
        for (OBRResolutionMode mode : modes) {
            if (supportedModes.contains(mode)) return true;
        }
        return false;
    }

    void addProjects(List<Object> result, Workspace workspace) {
        try {
            result.addAll(workspace.getAllProjects());
        } catch (Exception e) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying workspace Bnd projects.", e));
        }
    }

    ProjectBundle[] getProjectBundles(Project project) {
        ProjectBundle[] result = null;
        try {
            Collection<? extends Builder> builders = project.getSubBuilders();
            result = new ProjectBundle[builders.size()];

            int i = 0;
            for (Builder builder : builders) {
                ProjectBundle bundle = new ProjectBundle(project, builder.getBsn());
                result[i++] = bundle;
            }
        } catch (Exception e) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Error querying sub-bundles for project %s.", project.getName()), e));
        }
        return result;
    }

    RepositoryBundleVersion[] getRepositoryBundleVersions(RepositoryBundle bundle) {
        RepositoryBundleVersion[] result = null;

        List<Version> versions = null;
        try {
            versions = bundle.getRepo().versions(bundle.getBsn());
        } catch (Exception e) {
            Plugin.logError(MessageFormat.format("Error querying versions for bundle {0} in repository {1}.", bundle.getBsn(), bundle.getRepo().getName()), e);
        }
        if (versions != null) {
            result = new RepositoryBundleVersion[versions.size()];
            int i = 0;
            for (Version version : versions) {
                result[i++] = new RepositoryBundleVersion(bundle, version);
            }
        }
        return result;
    }

    RepositoryBundle[] getRepositoryBundles(RepositoryPlugin repo) {
        RepositoryBundle[] result = null;

        List<String> bsns = null;
        try {
            bsns = repo.list(null);
        } catch (Exception e) {
            Plugin.logError("Error querying repository " + repo.getName(), e);
        }
        if (bsns != null) {
            Collections.sort(bsns);
            result = new RepositoryBundle[bsns.size()];
            int i = 0;
            for (String bsn : bsns) {
                result[i++] = new RepositoryBundle(repo, bsn);
            }
        }
        return result;
    }
}
