package bndtools.model.repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Constants;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;
import bndtools.model.clauses.VersionedClause;

public class RepositoryUtils {

    private static final Object CACHE_REPO = "cache";
    private static final String VERSION_LATEST = "latest";

    public static List<RepositoryPlugin> listRepositories(boolean hideCache) {
        try {
            Workspace workspace = Central.getWorkspace();
            List<RepositoryPlugin> plugins = workspace.getPlugins(RepositoryPlugin.class);
            List<RepositoryPlugin> repos = new ArrayList<RepositoryPlugin>(plugins.size() + 1);

            repos.add(new WrappingObrRepository(Central.getWorkspaceObrProvider(), null, workspace));

            for (RepositoryPlugin plugin : plugins) {
                if (!hideCache || !CACHE_REPO.equals(plugin.getName()))
                    repos.add(plugin);
            }
            return repos;
        } catch (Exception e) {
            Plugin.logError("Error loading repositories", e);
            return Collections.emptyList();
        }
    }

    public static VersionedClause convertRepoBundle(RepositoryBundle bundle) {
        Map<String, String> attribs = new HashMap<String, String>();
        if (isWorkspaceRepo(bundle.getRepo())) {
            attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
        }
        return new VersionedClause(bundle.getBsn(), attribs);
    }

    public static VersionedClause convertRepoBundleVersion(RepositoryBundleVersion bundleVersion) {
        Map<String, String> attribs = new HashMap<String, String>();
        if (isWorkspaceRepo(bundleVersion.getBundle().getRepo()))
            attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
        else
            attribs.put(Constants.VERSION_ATTRIBUTE, bundleVersion.getVersion().toString());
        return new VersionedClause(bundleVersion.getBundle().getBsn(), attribs);
    }

    public static boolean isWorkspaceRepo(RepositoryPlugin repo) {
        if (repo instanceof WrappingObrRepository) {
            OBRIndexProvider indexProvider = ((WrappingObrRepository) repo).getDelegate();
            return indexProvider instanceof WorkspaceObrProvider;
        }
        return false;
    }

}
