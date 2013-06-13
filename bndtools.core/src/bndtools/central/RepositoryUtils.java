package bndtools.central;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bndtools.utils.repos.RepoUtils;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.api.ILogger;
import bndtools.api.Logger;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;

public class RepositoryUtils {
    private static final ILogger logger = Logger.getLogger(RepositoryUtils.class);

    private static final Object CACHE_REPO = "cache";
    private static final String VERSION_LATEST = "latest";

    public static List<RepositoryPlugin> listRepositories(boolean hideCache) {
        Workspace workspace;
        try {
            workspace = Central.getWorkspace();
        } catch (Exception e1) {
            return Collections.emptyList();
        }

        try {
            List<RepositoryPlugin> plugins = workspace.getPlugins(RepositoryPlugin.class);
            List<RepositoryPlugin> repos = new ArrayList<RepositoryPlugin>(plugins.size() + 1);

            repos.add(Central.getWorkspaceRepository());

            for (RepositoryPlugin plugin : plugins) {
                if (!hideCache || !CACHE_REPO.equals(plugin.getName()))
                    repos.add(plugin);
            }
            return repos;
        } catch (Exception e) {
            logger.logError("Error loading repositories", e);
            return Collections.emptyList();
        }
    }

    public static VersionedClause convertRepoBundle(RepositoryBundle bundle) {
        Attrs attribs = new Attrs();
        if (RepoUtils.isWorkspaceRepo(bundle.getRepo())) {
            attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
        }
        return new VersionedClause(bundle.getBsn(), attribs);
    }

    public static VersionedClause convertRepoBundleVersion(RepositoryBundleVersion bundleVersion, DependencyPhase phase) {
        Attrs attribs = new Attrs();
        if (RepoUtils.isWorkspaceRepo(bundleVersion.getBundle().getRepo()))
            attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
        else {
            StringBuilder builder = new StringBuilder();

            builder.append(bundleVersion.getVersion().getMajor());
            builder.append('.').append(bundleVersion.getVersion().getMinor());
            if (phase != DependencyPhase.Build)
                builder.append('.').append(bundleVersion.getVersion().getMicro());

            attribs.put(Constants.VERSION_ATTRIBUTE, builder.toString());
        }
        return new VersionedClause(bundleVersion.getBundle().getBsn(), attribs);
    }

}
