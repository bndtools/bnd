package bndtools.central;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.PopulatedRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Workspace;
import aQute.bnd.memoize.Memoize;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;

public class RepositoryUtils {
	private static final ILogger												logger	= Logger
		.getLogger(RepositoryUtils.class);
	private static final Memoize<ServiceTracker<RepositoryPlugin, RepositoryPlugin>>	pluginTracker;
	static {
		pluginTracker = Memoize.predicateSupplier(() -> {
			Optional<ServiceTracker<RepositoryPlugin, RepositoryPlugin>> tracker = Optional
				.ofNullable(FrameworkUtil.getBundle(RepositoryUtils.class))
				.map(Bundle::getBundleContext)
				.map(context -> new ServiceTracker<>(context, RepositoryPlugin.class, null));
			tracker.ifPresent(ServiceTracker::open);
			return tracker.orElse(null);
		}, Objects::nonNull);
	}

	public static List<RepositoryPlugin> listRepositories(boolean hideCache) {
		Workspace workspace;
		try {
			workspace = Central.getWorkspace();
		} catch (Exception e1) {
			return Collections.emptyList();
		}
		return listRepositories(workspace, hideCache);
	}

	public static List<RepositoryPlugin> listRepositories(final Workspace localWorkspace, final boolean hideCache) {
		try {
			return localWorkspace.readLocked(() -> {
				List<RepositoryPlugin> plugins = localWorkspace.getPlugins(RepositoryPlugin.class);
				plugins.addAll(getAdditionalPlugins());
				List<RepositoryPlugin> repos = new ArrayList<>(plugins.size() + 1);

				// Add the workspace repo if the provided workspace == the
				// global bnd workspace
				Workspace bndWorkspace = Central.getWorkspaceIfPresent();
				if ((bndWorkspace == localWorkspace) && !bndWorkspace.isDefaultWorkspace())
					repos.add(Central.getWorkspaceRepository());

				// Add the repos from the provided workspace
				for (RepositoryPlugin plugin : plugins) {
					if ((plugin instanceof PopulatedRepository) && ((PopulatedRepository) plugin).isEmpty()) {
						continue;
					}
					if (hideCache == false || !Workspace.BND_CACHE_REPONAME.equals(plugin.getName())) {
						repos.add(plugin);
					}
				}

				for (RepositoryPlugin repo : repos) {
					if (repo instanceof RegistryPlugin) {
						RegistryPlugin registry = (RegistryPlugin) repo;
						registry.setRegistry(bndWorkspace);
					}
				}

				return repos;
			});
		} catch (Exception e) {
			logger.logError("Error loading repositories: " + e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	private static Collection<RepositoryPlugin> getAdditionalPlugins() {
		return Optional.ofNullable(pluginTracker.get())
			.map(ServiceTracker::getTracked)
			.map(Map::values)
			.orElse(Collections.emptyList());
	}
}
