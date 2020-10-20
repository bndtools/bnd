package bndtools.model.repo;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.Repository;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;

abstract class VersionFinder {
	String		versionSpec;
	Strategy	strategy;

	VersionFinder(String versionSpec, Strategy strategy) {
		this.versionSpec = versionSpec;
		this.strategy = strategy;
	}

	abstract Version findVersion() throws Exception;

	@Override
	public String toString() {
		return versionSpec;
	}
}

public abstract class RepositoryEntry implements IAdaptable, ResourceProvider {

	private static final ILogger	logger	= Logger.getLogger(RepositoryEntry.class);

	private final RepositoryPlugin	repo;
	private final String			bsn;
	private final VersionFinder		versionFinder;

	protected RepositoryEntry(RepositoryPlugin repo, String bsn, VersionFinder versionFinder) {
		this.repo = repo;
		this.bsn = bsn;
		this.versionFinder = versionFinder;
	}

	public final RepositoryPlugin getRepo() {
		return repo;
	}

	public final String getBsn() {
		return bsn;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// Avoid getting the file if the requested adapter type is not
		// supported.
		boolean adaptable = IFile.class.equals(adapter) || File.class.equals(adapter) || URI.class.equals(adapter);
		if (!adaptable)
			return null;

		if (IFile.class.equals(adapter)) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
				.getRoot();
			File file = getFile(false);
			return file != null ? (T) root.getFileForLocation(new Path(file.getAbsolutePath())) : null;
		}

		if (File.class.equals(adapter))
			return (T) getFile(false);

		if (URI.class.equals(adapter)) {
			File file = getFile(false);
			return file != null ? (T) file.toURI() : null;
		}

		return null;
	}

	public final boolean isLocal() {
		boolean result = true;
		try {
			if (repo instanceof RemoteRepositoryPlugin) {
				ResourceHandle handle = ((RemoteRepositoryPlugin) repo).getHandle(bsn, versionFinder.versionSpec,
					versionFinder.strategy, Collections.<String, String> emptyMap());
				Location location = handle.getLocation();
				result = location == Location.local || location == Location.remote_cached;
			}
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Failed to query repository {0} for bundle {1} version {2}.",
				repo.getName(), bsn, versionFinder), e);
		}
		return result;
	}

	public final File getFile(boolean forceDownload) {
		try {
			if (repo instanceof RemoteRepositoryPlugin) {
				ResourceHandle handle = ((RemoteRepositoryPlugin) repo).getHandle(bsn, versionFinder.versionSpec,
					versionFinder.strategy, Collections.emptyMap());

				switch (handle.getLocation()) {
					case local :
					case remote_cached :
						return handle.request();
					default :
						return forceDownload ? handle.request() : null;
				}
			}
			Version version = versionFinder.findVersion();
			if (version == null) {
				return null;
			}
			return repo.get(bsn, version, Collections.emptyMap());
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Failed to query repository {0} for bundle {1} version {2}.",
				repo.getName(), bsn, versionFinder), Exceptions.unrollCause(e, InvocationTargetException.class));
			return null;
		}
	}

	@Override
	public Resource getResource() {
		RepositoryPlugin repositoryPlugin = getRepo();
		try {
			if (repositoryPlugin instanceof Repository) {
				Repository repository = (Repository) repositoryPlugin;
				ExpressionCombiner combiner = repository.getExpressionCombiner();

				RequirementBuilder identBuilder = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
				identBuilder.addFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn, versionFinder.findVersion()
					.toString(), null);
				RequirementBuilder infoBuilder = new RequirementBuilder("bnd.info");
				infoBuilder.addFilter("name", bsn, versionFinder.findVersion()
					.toString(), null);
				OrExpression expression = combiner.or( //
					combiner.identity(identBuilder.buildSyntheticRequirement()),
					combiner.identity(infoBuilder.buildSyntheticRequirement()));

				Promise<Collection<Resource>> promise = repository.findProviders(expression);

				Throwable failure = promise.getFailure();

				if (failure != null) {
					throw Exceptions.duck(failure);
				}

				return promise.getValue()
					.stream()
					.findFirst()
					.orElse(null);
			}

			File file = repositoryPlugin.get(bsn, versionFinder.findVersion(), null);

			if (file != null && file.exists()) {
				ResourceBuilder rb = new ResourceBuilder();
				rb.addFile(file, file.toURI());
				return rb.build();
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		return null;
	}

}
