package bndtools.model.repo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
	import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import aQute.libg.glob.Glob;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.version.Version;
import aQute.p2.provider.Feature;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.EclipseWorkspaceRepository;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

	private static final String									CACHE_REPOSITORY			= "cache";
	private static final ILogger								logger						= Logger
		.getLogger(RepositoryTreeContentProvider.class);

	private final EnumSet<ResolutionPhase>						phases;

	private String												rawFilter					= null;
	private String												wildcardFilter				= null;
	private Glob												wildcardGlob				= null;
	/**
	 * Number of filter results to keep per repo. This is to avoid memory leaks
	 * if you search with lots of different filter strings.
	 */
	private static final int									MAX_CACHED_FILTER_RESULTS	= 10;
	private static final int									MAX_CACHED_REPOSITORIES		= 16;
	private boolean												showRepos					= true;
	private volatile boolean showFeatures = true;

	private Requirement											requirementFilter			= null;

	private static final Map<RepositoryPlugin, Map<String, Object[]>>	repoPluginListResults	= new LinkedHashMap<>(16,
		0.75f, true);
	private static final Map<RepositoryPlugin, Integer> activeRepositoryLoads = new HashMap<>();
	private static final Map<Map<String, Object[]>, Map<String, RepositoryLoadRequest>> inFlightRepositoryLoads = new IdentityHashMap<>();
	private StructuredViewer									structuredViewer;
	private boolean									waitForFastResult		= true;
	private static final String LOCAL_CONTENT_CACHE_KEY = "\u0000local-content";
	private boolean									localFiltering;
	private boolean									localFilterUseLabels;
	private record RepositoryLoadState(Map<String, Object[]> cache, Object[] elements) {}
	private record RepositoryLoadWaiter(RepositoryTreeContentProvider provider, RepositoryPlugin repository,
		Map<String, Object[]> cache, String key, long generation) {}
	private static final class RepositoryLoadRequest {
		final List<RepositoryLoadWaiter> waiters = new ArrayList<>();
	}

	private final Map<RepositoryPlugin, Map<String, RepositoryLoadState>> pendingResults = new HashMap<>();
	private final Map<RepositoryPlugin, Map<String, RepositoryLoadState>> failedResults = new HashMap<>();
	private volatile long inputGeneration;
	private volatile boolean disposed;

	public RepositoryTreeContentProvider() {
		this.phases = EnumSet.allOf(ResolutionPhase.class);
	}

	public RepositoryTreeContentProvider(ResolutionPhase mode) {
		this.phases = EnumSet.of(mode);
	}

	public RepositoryTreeContentProvider(EnumSet<ResolutionPhase> modes) {
		this.phases = modes;
	}

	public String getFilter() {
		return rawFilter;
	}

	public void setFilter(String filter) {
		failedResults.clear();
		String normalized = filter == null ? null : filter.trim();
		if (normalized == null || normalized.isEmpty() || normalized.equals("*")) {
			rawFilter = filter;
			wildcardFilter = null;
			wildcardGlob = null;
			return;
		}
		String candidate = "*" + normalized + "*";
		try {
			Glob candidateGlob = new Glob(candidate, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			rawFilter = filter;
			wildcardFilter = candidate;
			wildcardGlob = candidateGlob;
		} catch (PatternSyntaxException e) {
			// Keep the last valid pattern while the user is still typing.
		}
	}

	public void setRequirementFilter(Requirement requirement) {
		this.requirementFilter = requirement;
	}

	public void setShowRepos(boolean showRepos) {
		this.showRepos = showRepos;
	}

	public boolean isShowRepos() {
		return showRepos;
	}

	public boolean isShowFeatures() {
		return showFeatures;
	}

	public void setShowFeatures(boolean showFeatures) {
		this.showFeatures = showFeatures;
	}

	public void setWaitForFastResult(boolean waitForFastResult) {
		this.waitForFastResult = waitForFastResult;
	}

	public void setLocalFiltering(boolean localFiltering) {
		this.localFiltering = localFiltering;
	}

	public void setLocalFilterUseLabels(boolean localFilterUseLabels) {
		this.localFilterUseLabels = localFilterUseLabels;
	}

	public static void invalidateRepositoryCache(RepositoryPlugin repository) {
		synchronized (repoPluginListResults) {
			if (repository == null) {
				repoPluginListResults.clear();
			} else {
				repoPluginListResults.remove(repository);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		Collection<Object> result;
		if (inputElement instanceof Workspace) {
			result = new ArrayList<>();
			Workspace workspace = (Workspace) inputElement;
			addRepositoryPlugins(result, workspace);
		} else if (inputElement instanceof Collection) {
			result = new ArrayList<>();
			addCollection(result, (Collection<Object>) inputElement);
		} else if (inputElement instanceof Object[]) {
			result = new ArrayList<>();
			addCollection(result, Arrays.asList(inputElement));
		} else {
			result = Collections.emptyList();
		}

		return result.toArray();
	}

	@Override
	public void dispose() {
		disposed = true;
		pendingResults.clear();
		failedResults.clear();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof StructuredViewer) {
			this.structuredViewer = (StructuredViewer) viewer;
			if (oldInput != null && oldInput != newInput) {
				inputGeneration++;
				pendingResults.clear();
				failedResults.clear();
			}
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] result = null;

		if (parentElement instanceof RepositoryPlugin) {
			RepositoryPlugin repo = (RepositoryPlugin) parentElement;
			result = getRepositoryBundles(repo);
		} else if (parentElement instanceof RepositoryFeature) {
			// Check RepositoryFeature BEFORE RepositoryBundle since both extend
			// RepositoryEntry
			RepositoryFeature feature = (RepositoryFeature) parentElement;
			result = new Object[] {
				new FeatureVersionNode(feature)
			};
		} else if (parentElement instanceof FeatureVersionNode) {
			FeatureVersionNode versionNode = (FeatureVersionNode) parentElement;
			result = getFeatureChildren(versionNode);
		} else if (parentElement instanceof RepositoryBundle) {
			RepositoryBundle bundle = (RepositoryBundle) parentElement;
			result = getRepositoryBundleVersions(bundle);
		} else if (parentElement instanceof Project) {
			Project project = (Project) parentElement;
			result = getProjectBundles(project);
		} else if (parentElement instanceof FeatureFolderNode) {
			FeatureFolderNode folder = (FeatureFolderNode) parentElement;
			result = folder.getChildren()
				.toArray();
		}

		return result;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof RepositoryBundle) {
			return ((RepositoryBundle) element).getRepo();
		}
		if (element instanceof RepositoryBundleVersion) {
			return ((RepositoryBundleVersion) element).getParentBundle();
		}
		if (element instanceof RepositoryFeature) {
			return ((RepositoryFeature) element).getRepo();
		}
		if (element instanceof FeatureVersionNode) {
			return ((FeatureVersionNode) element).getParent();
		}
		if (element instanceof FeatureFolderNode) {
			return ((FeatureFolderNode) element).getParent();
		}
		if (element instanceof IncludedFeatureItem) {
			return ((IncludedFeatureItem) element).getParent();
		}
		if (element instanceof RequiredFeatureItem) {
			return ((RequiredFeatureItem) element).getParent();
		}
		if (element instanceof IncludedBundleItem) {
			return ((IncludedBundleItem) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof RepositoryPlugin || element instanceof RepositoryBundle || element instanceof Project
			|| element instanceof RepositoryFeature || element instanceof FeatureVersionNode
			|| element instanceof FeatureFolderNode;
	}

	private void addRepositoryPlugins(Collection<Object> result, Workspace workspace) {
		workspace.getErrors()
			.clear();
		List<RepositoryPlugin> repoPlugins = workspace.getPlugins(RepositoryPlugin.class);
		for (String error : workspace.getErrors()) {
			logger.logError(error, null);
		}
		for (RepositoryPlugin repoPlugin : repoPlugins) {
			if (CACHE_REPOSITORY.equals(repoPlugin.getName()))
				continue;
			if (repoPlugin instanceof IndexProvider) {
				IndexProvider indexProvider = (IndexProvider) repoPlugin;
				if (!supportsPhase(indexProvider))
					continue;
			}
			if (showRepos)
				result.add(repoPlugin);
			else
				result.addAll(Arrays.asList(getRepositoryBundles(repoPlugin)));
		}
	}

	private void addCollection(Collection<Object> result, Collection<Object> inputs) {
		for (Object input : inputs) {
			if (input instanceof RepositoryPlugin) {
				RepositoryPlugin repo = (RepositoryPlugin) input;
				if (repo instanceof IndexProvider) {
					if (!supportsPhase((IndexProvider) repo))
						continue;
				}

				if (showRepos) {
					result.add(repo);
				} else {
					Object[] bundles = getRepositoryBundles(repo);
					if (bundles != null && bundles.length > 0)
						result.addAll(Arrays.asList(bundles));
				}
			}
		}
	}

	private boolean supportsPhase(IndexProvider provider) {
		Set<ResolutionPhase> supportedPhases = provider.getSupportedPhases();
		for (ResolutionPhase phase : phases) {
			if (supportedPhases.contains(phase))
				return true;
		}
		return false;
	}

	Object[] getProjectBundles(Project project) {
		ProjectBundle[] result = null;
		try (ProjectBuilder pb = project.getBuilder(null)) {
			List<Builder> builders = pb.getSubBuilders();
			result = new ProjectBundle[builders.size()];

			int i = 0;
			for (Builder builder : builders) {
				ProjectBundle bundle = new ProjectBundle(project, builder.getBsn());
				result[i++] = bundle;
			}
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Error querying sub-bundles for project {0}.", project.getName()), e);
		}
		return result;
	}

	Object[] getRepositoryBundleVersions(RepositoryBundle bundle) {
		SortedSet<Version> versions = null;
		try {
			versions = bundle.getRepo()
				.versions(bundle.getBsn());
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Error querying versions for bundle {0} in repository {1}.",
				bundle.getBsn(), bundle.getRepo()
					.getName()),
				e);
		}
		if (versions != null) {
			Stream<RepositoryBundleVersion> resultStream = versions.stream()
				.map(version -> new RepositoryBundleVersion(bundle, version));
			// If the RepositoryBundle represents a pseudo-BSN of the form
			// group:artifact, then we don't want to display the true bundles
			// under this node.
			if (bundle.getBsn()
				.indexOf(":") != -1) {
				resultStream = resultStream.filter(rbv -> rbv.getText()
					.contains("Not a bundle"));
			}
			return resultStream.toArray(RepositoryBundleVersion[]::new);
		}
		return null;
	}

	Object[] getFeatureChildren(FeatureVersionNode versionNode) {
		// Create three folder nodes for the feature hierarchy
		List<FeatureFolderNode> folders = new ArrayList<>();

		FeatureFolderNode includedFeaturesFolder = new FeatureFolderNode(versionNode,
			FeatureFolderNode.FolderType.INCLUDED_FEATURES);
		if (includedFeaturesFolder.hasChildren()) {
			folders.add(includedFeaturesFolder);
		}

		FeatureFolderNode requiredFeaturesFolder = new FeatureFolderNode(versionNode,
			FeatureFolderNode.FolderType.REQUIRED_FEATURES);
		if (requiredFeaturesFolder.hasChildren()) {
			folders.add(requiredFeaturesFolder);
		}

		FeatureFolderNode includedBundlesFolder = new FeatureFolderNode(versionNode,
			FeatureFolderNode.FolderType.INCLUDED_BUNDLES);
		if (includedBundlesFolder.hasChildren()) {
			folders.add(includedBundlesFolder);
		}

		return folders.toArray();
	}

	Object[] getRepositoryBundles(final RepositoryPlugin repoPlugin) {
		if (requirementFilter != null) {
			if (repoPlugin instanceof Repository) {
				return searchR5Repository(repoPlugin, (Repository) repoPlugin);
			} else if (repoPlugin instanceof WorkspaceRepository) {
				try {
					EclipseWorkspaceRepository workspaceRepo = Central.getEclipseWorkspaceRepository();
					return searchR5Repository(repoPlugin, workspaceRepo);
				} catch (Exception e) {
					logger.logError("Error querying workspace repository", e);
				}
			}
			return null;
		}
		if (localFiltering) {
			Map<String, Object[]> listResults = getRepositoryListResults(repoPlugin);
			String filteredKey = localFilterCacheKey();
			Object[] filtered = listResults.get(filteredKey);
			if (filtered != null) {
				return filtered;
			}
			RepositoryLoadState failedFilter = getLoadState(failedResults, repoPlugin, filteredKey);
			if (failedFilter != null && failedFilter.cache() == listResults) {
				return failedFilter.elements();
			}
			Object[] fullContents = listResults.get(LOCAL_CONTENT_CACHE_KEY);
			if (fullContents == null) {
				RepositoryLoadState failedContents = getLoadState(failedResults, repoPlugin,
					LOCAL_CONTENT_CACHE_KEY);
				if (failedContents != null && failedContents.cache() == listResults) {
					return filterLocalRepositoryBundles(repoPlugin, listResults, filteredKey,
						failedContents.elements());
				}
				return loadRepositoryBundles(repoPlugin, null, null, true, LOCAL_CONTENT_CACHE_KEY);
			}
			return filterLocalRepositoryBundles(repoPlugin, listResults, filteredKey, fullContents);
		}
		String filter = wildcardFilter;
		boolean includeFeatures = showFeatures;
		String cacheKey = cacheKey(filter, includeFeatures);
		Map<String, Object[]> listResults = getRepositoryListResults(repoPlugin);
		Object[] contents = listResults.get(cacheKey);
		if (contents != null) {
			return contents;
		}
		return loadRepositoryBundles(repoPlugin, filter, wildcardGlob, includeFeatures, cacheKey);
	}

	private Object[] loadRepositoryBundles(RepositoryPlugin repoPlugin, String filter, Glob filterGlob,
		boolean includeFeatures, String cacheKey) {
		Map<String, Object[]> currentCache = getRepositoryListResults(repoPlugin);
		RepositoryLoadState error = getLoadState(failedResults, repoPlugin, cacheKey);
		if (error != null && error.cache() == currentCache) {
			return error.elements();
		}
		if (error != null) {
			removeLoadState(failedResults, repoPlugin, cacheKey);
		}
		RepositoryLoadState pending = getLoadState(pendingResults, repoPlugin, cacheKey);
		if (pending != null && pending.cache() == currentCache) {
			return pending.elements();
		}
		if (pending != null) {
			removeLoadState(pendingResults, repoPlugin, cacheKey);
		}

		Map<String, Object[]> resultMap = beginRepositoryLoad(repoPlugin);
		Object[] justLoaded = resultMap.get(cacheKey);
		if (justLoaded != null) {
			releaseRepositoryLoad(repoPlugin);
			return localFiltering && LOCAL_CONTENT_CACHE_KEY.equals(cacheKey)
				? filterLocalRepositoryBundles(repoPlugin, resultMap, localFilterCacheKey(), justLoaded)
				: justLoaded;
		}
		Object[] loading = new Object[] {
			new LoadingContentElement(repoPlugin.getName())
		};
		setLoadState(pendingResults, repoPlugin, cacheKey, new RepositoryLoadState(resultMap, loading));
		long generation = inputGeneration;
		if (!registerRepositoryLoad(this, repoPlugin, resultMap, cacheKey, generation)) {
			return loading;
		}
		Job job = new Job("Loading " + repoPlugin.getName() + " content...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<String> bsns;
				try {
					bsns = repoPlugin.list(filter);
				} catch (Exception e) {
					String message = MessageFormat.format("Error querying repository {0}.", repoPlugin.getName());
					logger.logError(message, e);
					String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
					completeRepositoryLoad(repoPlugin, resultMap, cacheKey, null, detail, null);
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, message, e);
				}
				if (bsns == null) {
					completeRepositoryLoad(repoPlugin, resultMap, cacheKey, null, "No bundle list returned", null);
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "No bundle list returned");
				}
				bsns = new ArrayList<>(bsns);
				Collections.sort(bsns);
				List<Object> items = new ArrayList<>();
				Set<String> featureIds = new LinkedHashSet<>();
				String featureError = null;
				if (repoPlugin instanceof Repository repository) {
					try {
						for (Resource resource : getFeatureResources(repository)) {
							featureIds.add(aQute.bnd.osgi.resource.ResourceUtils.getIdentity(resource));
						}
						items.addAll(getRepositoryFeatures(repoPlugin, repository, filter, filterGlob, includeFeatures));
					} catch (Exception e) {
						String message = "Error querying features from repository " + repoPlugin.getName();
						logger.logError(message, e);
						featureError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
					}
				}
				for (String bsn : bsns) {
					if (featureIds.contains(bsn)) {
						try {
							if (repoPlugin.versions(bsn).isEmpty()) {
								continue;
							}
						} catch (Exception e) {
							logger.logError("Error querying bundle versions for " + bsn, e);
						}
					}
					items.add(new RepositoryBundle(repoPlugin, bsn));
				}
				if (featureError != null) {
					items.add(LoadingContentElement.failure(repoPlugin.getName(), featureError));
					completeRepositoryLoad(repoPlugin, resultMap, cacheKey, null, featureError, items.toArray());
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
						"Feature data unavailable for repository " + repoPlugin.getName());
				}

				boolean published = completeRepositoryLoad(repoPlugin, resultMap, cacheKey, items.toArray(), null, null);
				return published ? Status.OK_STATUS : Status.CANCEL_STATUS;
		}
		};
		job.schedule();
		if (waitForFastResult) {
			try {
				job.join(100, null);
			} catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
			} catch (OperationCanceledException e) {
				// Keep the loading row visible.
			}
			Object[] loaded = getRepositoryListResults(repoPlugin).get(cacheKey);
			if (loaded != null) {
				removeLoadState(pendingResults, repoPlugin, cacheKey);
				return localFiltering && LOCAL_CONTENT_CACHE_KEY.equals(cacheKey)
					? filterLocalRepositoryBundles(repoPlugin, resultMap, localFilterCacheKey(), loaded)
					: loaded;
			}
		}
		return loading;
	}

	// UI thread only: sole writer of pendingResults/failedResults after scheduling
	private void finishRepositoryLoad(RepositoryPlugin repoPlugin, Map<String, Object[]> resultMap, String cacheKey,
		String error, Object[] partialElements, long generation) {
		try {
			if (disposed || generation != inputGeneration) {
				return;
			}
			RepositoryLoadState pending = getLoadState(pendingResults, repoPlugin, cacheKey);
			if (pending != null && pending.cache() == resultMap) {
				removeLoadState(pendingResults, repoPlugin, cacheKey);
			}
			if (getRepositoryListResults(repoPlugin) != resultMap) {
				if (structuredViewer != null && !structuredViewer.getControl().isDisposed()) {
					structuredViewer.refresh();
				}
				return;
			}
			if (error == null) {
				RepositoryLoadState failed = getLoadState(failedResults, repoPlugin, cacheKey);
				if (failed != null && failed.cache() == resultMap) {
					removeLoadState(failedResults, repoPlugin, cacheKey);
				}
			} else {
				Object[] elements = partialElements != null ? partialElements : new Object[] {
					LoadingContentElement.failure(repoPlugin.getName(), error)
				};
				setLoadState(failedResults, repoPlugin, cacheKey, new RepositoryLoadState(resultMap, elements));
			}
			if (structuredViewer == null || structuredViewer.getControl().isDisposed()) {
				return;
			}
			if (showRepos) {
				structuredViewer.refresh(repoPlugin, true);
			} else {
				structuredViewer.refresh();
			}
		} finally {
			releaseRepositoryLoad(repoPlugin);
		}
	}

	void scheduleRepositoryLoadFinish(RepositoryPlugin repoPlugin, Map<String, Object[]> resultMap,
		String cacheKey, String error, Object[] partialElements, long generation) {
		try {
			Display display = Display.getDefault();
			if (display.isDisposed()) {
				releaseRepositoryLoad(repoPlugin);
				return;
			}
			display.asyncExec(() -> finishRepositoryLoad(repoPlugin, resultMap, cacheKey, error, partialElements,
				generation));
		} catch (RuntimeException e) {
			releaseRepositoryLoad(repoPlugin);
			logger.logError("Unable to refresh repository " + repoPlugin.getName(), e);
		}
	}

	private Object[] filterLocalRepositoryBundles(RepositoryPlugin repoPlugin, Map<String, Object[]> resultMap,
		String filteredKey, Object[] contents) {
		RepositoryLoadState failed = getLoadState(failedResults, repoPlugin, filteredKey);
		if (failed != null && failed.cache() == resultMap) {
			return failed.elements();
		}
		RepositoryLoadState pending = getLoadState(pendingResults, repoPlugin, filteredKey);
		if (pending != null && pending.cache() == resultMap) {
			return pending.elements();
		}
		Map<String, Object[]> activeCache = beginRepositoryLoad(repoPlugin);
		if (activeCache != resultMap) {
			releaseRepositoryLoad(repoPlugin);
			return loadRepositoryBundles(repoPlugin, null, null, true, LOCAL_CONTENT_CACHE_KEY);
		}
		Object[] loading = { new LoadingContentElement(repoPlugin.getName()) };
		setLoadState(pendingResults, repoPlugin, filteredKey, new RepositoryLoadState(resultMap, loading));
		String filter = wildcardFilter;
		Glob glob = wildcardGlob;
		boolean features = showFeatures;
		boolean labels = localFilterUseLabels;
		long generation = inputGeneration;
		if (!registerRepositoryLoad(this, repoPlugin, resultMap, filteredKey, generation)) {
			return loading;
		}
		Job job = new Job("Filtering " + repoPlugin.getName() + " content...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Object[] visible = filterLocalRepositoryContents(contents, filter, glob, features, labels);
					if (containsFailure(contents)) {
						completeRepositoryLoad(repoPlugin, resultMap, filteredKey, null, failureMessage(contents), visible);
					} else {
						completeRepositoryLoad(repoPlugin, resultMap, filteredKey, visible, null, null);
					}
					return Status.OK_STATUS;
				} catch (Exception e) {
					String message = "Error filtering repository " + repoPlugin.getName();
					logger.logError(message, e);
					String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
					completeRepositoryLoad(repoPlugin, resultMap, filteredKey, null, detail, null);
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, message, e);
				}
			}
		};
		job.schedule();
		return loading;
	}

	private static Object[] filterLocalRepositoryContents(Object[] contents, String filter, Glob glob,
		boolean includeFeatures, boolean matchLabels) {
		List<Object> visible = new ArrayList<>(contents.length);
		for (Object element : contents) {
			if (element instanceof LoadingContentElement status && status.isFailure()) {
				visible.add(element);
				continue;
			}
			if (!includeFeatures && (element instanceof RepositoryFeatureResource || element instanceof RepositoryFeature)) {
				continue;
			}
			if (filter == null) {
				visible.add(element);
				continue;
			}
			String name = null;
			if (element instanceof RepositoryBundle bundle) {
				name = bundle.getBsn();
			} else if (element instanceof RepositoryFeatureResource feature) {
				name = feature.getId();
			} else if (element instanceof RepositoryFeature feature) {
				name = feature.getBsn();
			}
			if (matchesWildcard(name, filter, glob)) {
				visible.add(element);
				continue;
			}
			if (matchLabels && matchesLocalFilterLabel(element, filter, glob)) {
				visible.add(element);
			}
		}
		return visible.toArray();
	}

	private static boolean containsFailure(Object[] contents) {
		return Arrays.stream(contents).anyMatch(element -> element instanceof LoadingContentElement status
			&& status.isFailure());
	}

	private static String failureMessage(Object[] contents) {
		return Arrays.stream(contents)
			.filter(LoadingContentElement.class::isInstance)
			.map(LoadingContentElement.class::cast)
			.filter(LoadingContentElement::isFailure)
			.map(LoadingContentElement::toString)
			.findFirst()
			.orElse("Repository content is incomplete");
	}

	static boolean matchesLocalFilterLabel(Object element, String filter, Glob glob) {
		if (element instanceof RepositoryBundle bundle) {
			return matchesWildcard(bundle.getText(), filter, glob);
		}
		if (element instanceof RepositoryFeatureResource feature) {
			return matchesWildcard(feature.getLabel() + " " + feature.getVersion(), filter, glob);
		}
		if (element instanceof RepositoryFeature feature) {
			return matchesWildcard(feature.getText(), filter, glob);
		}
		return false;
	}

	private String localFilterCacheKey() {
		return cacheKey(wildcardFilter, showFeatures) + (localFilterUseLabels ? ":labels" : "");
	}

	private static RepositoryLoadState getLoadState(Map<RepositoryPlugin, Map<String, RepositoryLoadState>> states,
		RepositoryPlugin repo,
		String key) {
		Map<String, RepositoryLoadState> repoStates = states.get(repo);
		return repoStates == null ? null : repoStates.get(key);
	}

	private static void setLoadState(Map<RepositoryPlugin, Map<String, RepositoryLoadState>> states,
		RepositoryPlugin repo, String key, RepositoryLoadState state) {
		states.computeIfAbsent(repo, ignored -> new HashMap<>())
			.put(key, state);
	}

	private static void removeLoadState(Map<RepositoryPlugin, Map<String, RepositoryLoadState>> states,
		RepositoryPlugin repo, String key) {
		Map<String, RepositoryLoadState> repoStates = states.get(repo);
		if (repoStates != null) {
			repoStates.remove(key);
			if (repoStates.isEmpty()) {
				states.remove(repo);
			}
		}
	}

	private static String cacheKey(String filter, boolean showFeatures) {
		return (showFeatures ? "features:" : "bundles:") + (filter == null ? "" : filter);
	}

	List<RepositoryFeatureResource> getRepositoryFeatures(RepositoryPlugin plugin, Repository repository) {
		return getRepositoryFeatures(plugin, repository, wildcardFilter, wildcardGlob, showFeatures);
	}

	List<RepositoryFeatureResource> getRepositoryFeatures(RepositoryPlugin plugin, Repository repository,
		String filter, Glob filterGlob, boolean includeFeatures) {
		if (!includeFeatures)
			return List.of();
		return getFeatureResources(repository).stream()
			.map(resource -> new RepositoryFeatureResource(plugin, resource))
			.filter(item -> filter == null || matchesWildcard(item.getId(), filter, filterGlob))
			.sorted(Comparator.comparing(RepositoryFeatureResource::getId, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(item -> Version.parseVersion(item.getVersion())))
			.toList();
	}

	private List<Resource> getFeatureResources(Repository repository) {
		Requirement requirement = new aQute.bnd.osgi.resource.CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective("filter", "(" + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "=" + Feature.TYPE + ")")
			.buildSyntheticRequirement();
		return repository.findProviders(Collections.singleton(requirement))
			.getOrDefault(requirement, Collections.emptyList())
			.stream()
			.map(Capability::getResource)
			.distinct()
			.toList();
	}

	private static Map<String, Object[]> getRepositoryListResults(RepositoryPlugin repoPlugin) {
		synchronized (repoPluginListResults) {
			return repositoryResultsLocked(repoPlugin);
		}
	}

	private static Map<String, Object[]> beginRepositoryLoad(RepositoryPlugin repoPlugin) {
		synchronized (repoPluginListResults) {
			Map<String, Object[]> results = repositoryResultsLocked(repoPlugin);
			activeRepositoryLoads.merge(repoPlugin, 1, Integer::sum);
			return results;
		}
	}

	private static Map<String, Object[]> repositoryResultsLocked(RepositoryPlugin repoPlugin) {
		Map<String, Object[]> results = repoPluginListResults.get(repoPlugin);
		if (results == null) {
			trimRepositoryCacheLocked(MAX_CACHED_REPOSITORIES - 1);
			results = createLRUMap(MAX_CACHED_FILTER_RESULTS);
			repoPluginListResults.put(repoPlugin, results);
		}
		return results;
	}

	static boolean registerRepositoryLoad(RepositoryTreeContentProvider provider, RepositoryPlugin repoPlugin,
		Map<String, Object[]> results, String cacheKey, long generation) {
		synchronized (repoPluginListResults) {
			Map<String, RepositoryLoadRequest> requests = inFlightRepositoryLoads.computeIfAbsent(results,
				ignored -> new HashMap<>());
			RepositoryLoadRequest request = requests.get(cacheKey);
			boolean owner = request == null;
			if (owner) {
				request = new RepositoryLoadRequest();
				requests.put(cacheKey, request);
			}
			request.waiters.add(new RepositoryLoadWaiter(provider, repoPlugin, results, cacheKey, generation));
			return owner;
		}
	}

	static boolean completeRepositoryLoad(RepositoryPlugin repoPlugin, Map<String, Object[]> results,
		String cacheKey, Object[] content, String error, Object[] partialElements) {
		List<RepositoryLoadWaiter> waiters = List.of();
		boolean current;
		synchronized (repoPluginListResults) {
			current = repoPluginListResults.get(repoPlugin) == results;
			if (current && content != null) {
				results.put(cacheKey, content);
			}
			Map<String, RepositoryLoadRequest> requests = inFlightRepositoryLoads.get(results);
			if (requests != null) {
				RepositoryLoadRequest request = requests.remove(cacheKey);
				if (request != null) {
					waiters = new ArrayList<>(request.waiters);
				}
				if (requests.isEmpty()) {
					inFlightRepositoryLoads.remove(results);
				}
			}
		}
		for (RepositoryLoadWaiter waiter : waiters) {
			waiter.provider().scheduleRepositoryLoadFinish(waiter.repository(), waiter.cache(), waiter.key(), error,
				partialElements, waiter.generation());
		}
		return current;
	}

	private static void releaseRepositoryLoad(RepositoryPlugin repoPlugin) {
		synchronized (repoPluginListResults) {
			Integer loads = activeRepositoryLoads.get(repoPlugin);
			if (loads != null) {
				if (loads <= 1) {
					activeRepositoryLoads.remove(repoPlugin);
				} else {
					activeRepositoryLoads.put(repoPlugin, loads - 1);
				}
			}
			trimRepositoryCacheLocked(MAX_CACHED_REPOSITORIES);
		}
	}

	private static void trimRepositoryCacheLocked(int maximumSize) {
		while (repoPluginListResults.size() > maximumSize) {
			Iterator<RepositoryPlugin> iterator = repoPluginListResults.keySet()
				.iterator();
			boolean removed = false;
			while (iterator.hasNext()) {
				RepositoryPlugin candidate = iterator.next();
				if (!activeRepositoryLoads.containsKey(candidate)) {
					iterator.remove();
					removed = true;
					break;
				}
			}
			if (!removed) {
				return;
			}
		}
	}

	Object[] searchR5Repository(RepositoryPlugin repoPlugin, Repository osgiRepo) {
		Object[] result;
		Set<Object> resultSet = new LinkedHashSet<>();
		Map<Requirement, Collection<Capability>> providers = osgiRepo
			.findProviders(Collections.singleton(requirementFilter));

		for (Entry<Requirement, Collection<Capability>> providersEntry : providers.entrySet()) {
			for (Capability providerCap : providersEntry.getValue()) {
				Resource resource = providerCap.getResource();
				// Separate features from bundles
				if (isFeatureResource(resource)) {
					if (showFeatures)
						resultSet.add(new RepositoryFeatureResource(repoPlugin, resource));
				} else {
					resultSet.add(new RepositoryResourceElement(repoPlugin, resource));
				}
			}
		}

		result = resultSet.toArray();
		return result;
	}

	/**
	 * Check if a resource represents an Eclipse feature rather than a bundle.
	 * Features have type="org.eclipse.update.feature" in their identity
	 * capability.
	 */
	private boolean isFeatureResource(Resource resource) {
		List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		for (Capability identity : identities) {
			Object type = identity.getAttributes()
				.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
			if ("org.eclipse.update.feature".equals(type)) {
				return true;
			}
		}
		return false;
	}

	// Define a LRU-like inner map (max n entries)
	private static Map<String, Object[]> createLRUMap(int n) {
		return Collections.synchronizedMap(new LinkedHashMap<String, Object[]>(n + 1, 1.0f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Object[]> eldest) {
				if (size() > n) {
					Iterator<String> iterator = keySet().iterator();
					while (iterator.hasNext()) {
						String key = iterator.next();
						if (key == null || LOCAL_CONTENT_CACHE_KEY.equals(key) || "features:".equals(key)
							|| "bundles:".equals(key)) {
							continue;
						}
						iterator.remove();
						break;
					}
				}
				return false;
			}
		});
	}

	private static boolean matchesWildcard(String str, String wildcardPattern, Glob capturedGlob) {
		if (str == null || wildcardPattern == null) {
			return false;
		}
		Glob glob = capturedGlob == null
					? new Glob(wildcardPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
					: capturedGlob;
		return glob.matches(str);
	}
}
