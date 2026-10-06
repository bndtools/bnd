package aQute.bnd.lsp.services;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.repository.p2.provider.P2Repository;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.version.Version;
import aQute.libg.glob.Glob;
import aQute.p2.provider.Feature;

/**
 * Backs the client Repositories view. Every request is a JSON object with a
 * {@code workspace} URI; repositories are addressed by {@code repo} index and
 * {@code repoName} as returned by {@link #list(JsonObject)}.
 */
public class BndRepositoriesService {
	public static final String			WORKSPACE_KIND	= "workspace";
	public static final String			PLUGIN_KIND		= "plugin";

	private final BndWorkspaceManager	workspaceManager;

	public BndRepositoriesService(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public Object list(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		List<Map<String, Object>> repos = new ArrayList<>();
		List<RepositoryPlugin> all = repositories(ws);
		for (int i = 0; i < all.size(); i++) {
			RepositoryPlugin repo = all.get(i);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("index", i);
			entry.put("name", repo.getName());
			entry.put("kind", repo instanceof WorkspaceRepository ? WORKSPACE_KIND : PLUGIN_KIND);
			entry.put("title", title(repo));
			entry.put("tooltip", tooltip(repo));
			entry.put("location", safe(repo::getLocation));
			entry.put("status", safe(repo::getStatus));
			entry.put("writable", !(repo instanceof WorkspaceRepository) && repo.canWrite());
			entry.put("remote", repo instanceof RemoteRepositoryPlugin);
			entry.put("refreshable", repo instanceof Refreshable);
			entry.put("actionable", repo instanceof Actionable);
			entry.put("searchable", repo instanceof SearchableRepository || repo instanceof Repository);
			entry.put("p2", repo instanceof P2Repository);
			entry.put("tags", List.copyOf(repo.getTags()));
			repos.add(entry);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("workspace", ws.getBase()
			.toURI()
			.toString());
		result.put("offline", ws.isOffline());
		result.put("repositories", repos);
		return result;
	}

	public Object bundles(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		String filter = string(request, "filter");
		String pattern = filter == null || filter.isBlank() ? null : "*" + filter.trim() + "*";
		Glob glob = pattern == null ? null : new Glob(pattern, Pattern.CASE_INSENSITIVE);
		List<Map<String, Object>> bundles = new ArrayList<>();
		if (repo instanceof WorkspaceRepository) {
			for (Project project : ws.getAllProjects()) {
				for (String bsn : project.getBsns()) {
					if (glob == null || glob.matches(bsn)) {
						Map<String, Object> entry = bundle(repo, bsn);
						entry.put("project", project.getName());
						bundles.add(entry);
					}
				}
			}
		} else {
			List<String> names = repo.list(pattern);
			if (names != null) {
				for (String bsn : names) {
					bundles.add(bundle(repo, bsn));
				}
			}
		}
		List<Map<String, Object>> features = new ArrayList<>();
		if (repo instanceof P2Repository p2) {
			for (Feature feature : p2.getFeatures()) {
				String id = feature.getId();
				if (glob != null && (id == null || !glob.matches(id))) {
					continue;
				}
				features.add(feature(feature));
			}
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("bundles", bundles);
		result.put("features", features);
		return result;
	}

	public Object versions(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		String bsn = required(request, "bsn");
		SortedSet<Version> versions = repo.versions(bsn);
		List<Map<String, Object>> result = new ArrayList<>();
		if (versions != null) {
			List<Version> descending = new ArrayList<>(versions);
			Collections.reverse(descending);
			for (Version version : descending) {
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("version", version.toString());
				entry.put("title", title(repo, bsn, version));
				entry.put("tooltip", tooltip(repo, bsn, version));
				result.add(entry);
			}
		}
		return Map.of("versions", result);
	}

	public Object feature(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		if (!(repo instanceof P2Repository p2)) {
			throw new IllegalArgumentException("Repository has no features: " + repo.getName());
		}
		Feature feature = p2.getFeature(required(request, "id"), required(request, "version"));
		if (feature == null) {
			throw new IllegalArgumentException("Feature not found: " + string(request, "id"));
		}
		Map<String, Object> result = feature(feature);
		List<Map<String, Object>> plugins = new ArrayList<>();
		for (Feature.Plugin plugin : feature.getPlugins()) {
			plugins.add(item(plugin.id, plugin.version));
		}
		List<Map<String, Object>> includes = new ArrayList<>();
		for (Feature.Includes include : feature.getIncludes()) {
			includes.add(item(include.id, include.version));
		}
		List<Map<String, Object>> requires = new ArrayList<>();
		for (Feature.Requires require : feature.getRequires()) {
			Map<String, Object> entry = item(require.feature != null ? require.feature : require.plugin,
				require.version);
			entry.put("type", require.feature != null ? "feature" : "bundle");
			entry.put("match", require.match);
			requires.add(entry);
		}
		result.put("plugins", plugins);
		result.put("includes", includes);
		result.put("requires", requires);
		return result;
	}

	public Object get(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		String bsn = required(request, "bsn");
		Version version = Version.parseVersion(required(request, "version"));
		File file = repo.get(bsn, version, Map.of());
		if (file == null) {
			throw new IllegalStateException("Not available: " + bsn + ";version=" + version);
		}
		return Map.of("file", file.getAbsolutePath(), "uri", file.toURI()
			.toString());
	}

	public Object search(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		String namespace = required(request, "namespace");
		String filter = required(request, "filter");
		Requirement requirement = new CapReqBuilder(namespace).filter(filter)
			.buildSyntheticRequirement();
		List<Map<String, Object>> results = new ArrayList<>();
		List<RepositoryPlugin> all = repositories(ws);
		for (int i = 0; i < all.size(); i++) {
			if (!(all.get(i) instanceof Repository osgi)) {
				continue;
			}
			Map<Requirement, Collection<Capability>> providers = osgi.findProviders(List.of(requirement));
			for (Capability capability : providers.getOrDefault(requirement, List.of())) {
				Resource resource = capability.getResource();
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("repo", i);
				entry.put("repoName", all.get(i)
					.getName());
				entry.put("bsn", safe(() -> ResourceUtils.getIdentity(resource)));
				entry.put("version", safe(() -> ResourceUtils.getIdentityVersion(resource)));
				entry.put("type", ResourceUtils.getType(resource)
					.orElse(null));
				if (!results.contains(entry)) {
					results.add(entry);
				}
			}
		}
		return Map.of("results", results);
	}

	public Object actions(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		Map<String, Runnable> actions = actionMap(repo, request);
		return Map.of("actions", actions == null ? List.of() : List.copyOf(actions.keySet()));
	}

	public Object runAction(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		String label = required(request, "label");
		Map<String, Runnable> actions = actionMap(repo, request);
		Runnable action = actions == null ? null : actions.get(label);
		if (action == null) {
			throw new IllegalArgumentException("Unknown action: " + label);
		}
		action.run();
		return Map.of("done", true);
	}

	public Object refresh(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		List<String> refreshed = new ArrayList<>();
		if (request.has("repo")) {
			RepositoryPlugin repo = repository(ws, request);
			if (repo instanceof Refreshable refreshable) {
				refreshable.refresh();
			}
			refreshed.add(repo.getName());
		} else {
			for (RepositoryPlugin repo : repositories(ws)) {
				if (repo instanceof Refreshable refreshable) {
					refreshable.refresh();
					refreshed.add(repo.getName());
				}
			}
			workspaceManager.refreshWorkspace(ws.getBase());
		}
		return Map.of("refreshed", refreshed);
	}

	public Object put(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		if (repo instanceof WorkspaceRepository || !repo.canWrite()) {
			throw new IllegalArgumentException("Repository is not writable: " + repo.getName());
		}
		if (!request.has("files") || !request.get("files")
			.isJsonArray()) {
			throw new IllegalArgumentException("Missing files");
		}
		List<Map<String, Object>> added = new ArrayList<>();
		for (JsonElement element : request.getAsJsonArray("files")) {
			File file = BndWorkspaceManager.getFileFromUri(element.getAsString());
			if (file == null || !file.isFile()) {
				throw new IllegalArgumentException("Not a file: " + element.getAsString());
			}
			String bsn;
			String version;
			try (Jar jar = new Jar(file)) {
				bsn = jar.getBsn();
				version = jar.getVersion();
			}
			if (bsn == null) {
				throw new IllegalArgumentException("Not a bundle: " + file);
			}
			RepositoryPlugin.PutResult result;
			try (InputStream in = Files.newInputStream(file.toPath())) {
				result = repo.put(in, new RepositoryPlugin.PutOptions());
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("bsn", bsn);
			entry.put("version", version);
			entry.put("artifact", result.artifact == null ? null : result.artifact.toString());
			added.add(entry);
		}
		if (repo instanceof Refreshable refreshable) {
			refreshable.refresh();
		}
		return Map.of("added", added);
	}

	public Object download(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		RepositoryPlugin repo = repository(ws, request);
		String bsn = string(request, "bsn");
		String version = string(request, "version");
		List<String> bsns = bsn != null ? List.of(bsn) : Optional.ofNullable(repo.list(null))
			.orElse(List.of());
		int downloaded = 0;
		List<String> errors = new ArrayList<>();
		for (String name : bsns) {
			Collection<Version> versions = version != null ? List.of(Version.parseVersion(version))
				: Optional.<Collection<Version>> ofNullable(repo.versions(name))
					.orElse(List.of());
			for (Version v : versions) {
				if (Thread.currentThread()
					.isInterrupted()) {
					return Map.of("downloaded", downloaded, "errors", errors);
				}
				try {
					if (repo instanceof RemoteRepositoryPlugin remote) {
						ResourceHandle handle = remote.getHandle(name, v.toString(), Strategy.EXACT, Map.of());
						if (handle != null && handle.getLocation() == ResourceHandle.Location.remote) {
							handle.request();
							downloaded++;
						}
					} else if (repo.get(name, v, Map.of()) != null) {
						downloaded++;
					}
				} catch (Exception e) {
					errors.add(name + ";version=" + v + ": " + e.getMessage());
				}
			}
		}
		return Map.of("downloaded", downloaded, "errors", errors);
	}

	public Object offline(JsonObject request) throws Exception {
		Workspace ws = workspace(request);
		if (request.has("offline") && !request.get("offline")
			.isJsonNull()) {
			ws.setOffline(request.get("offline")
				.getAsBoolean());
		}
		return Map.of("offline", ws.isOffline());
	}

	private Map<String, Runnable> actionMap(RepositoryPlugin repo, JsonObject request) throws Exception {
		if (!(repo instanceof Actionable actionable)) {
			return null;
		}
		return actionable.actions(target(request));
	}

	private static Object[] target(JsonObject request) {
		String bsn = string(request, "bsn");
		String version = string(request, "version");
		if (bsn == null) {
			return new Object[0];
		}
		if (version == null) {
			return new Object[] {
				bsn
			};
		}
		return new Object[] {
			bsn, Version.parseVersion(version)
		};
	}

	private Workspace workspace(JsonObject request) {
		String uri = required(request, "workspace");
		File file = BndWorkspaceManager.getFileFromUri(uri);
		Workspace ws = file == null ? null : workspaceManager.getWorkspaceForFile(file);
		if (ws == null) {
			throw new IllegalArgumentException("Not a bnd workspace: " + uri);
		}
		return ws;
	}

	static List<RepositoryPlugin> repositories(Workspace ws) {
		List<RepositoryPlugin> result = new ArrayList<>();
		result.add(ws.getWorkspaceRepository());
		for (RepositoryPlugin repo : ws.getRepositories()) {
			if (!(repo instanceof WorkspaceRepository)) {
				result.add(repo);
			}
		}
		return result;
	}

	private static RepositoryPlugin repository(Workspace ws, JsonObject request) {
		if (!request.has("repo")) {
			throw new IllegalArgumentException("Missing repo");
		}
		int index = request.get("repo")
			.getAsInt();
		List<RepositoryPlugin> all = repositories(ws);
		if (index < 0 || index >= all.size()) {
			throw new IllegalArgumentException("Unknown repository index: " + index);
		}
		RepositoryPlugin repo = all.get(index);
		String name = string(request, "repoName");
		if (name != null && !name.equals(repo.getName())) {
			throw new IllegalStateException("Repositories changed; refresh the view.");
		}
		return repo;
	}

	private static Map<String, Object> bundle(RepositoryPlugin repo, String bsn) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("bsn", bsn);
		entry.put("title", title(repo, bsn));
		entry.put("tooltip", tooltip(repo, bsn));
		return entry;
	}

	private static Map<String, Object> feature(Feature feature) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("id", feature.getId());
		entry.put("version", feature.getVersion());
		entry.put("label", feature.label);
		entry.put("provider", feature.providerName);
		return entry;
	}

	private static Map<String, Object> item(String id, String version) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("id", id);
		entry.put("version", version);
		return entry;
	}

	private static String title(RepositoryPlugin repo, Object... target) {
		if (repo instanceof Actionable actionable) {
			String title = safe(() -> actionable.title(target));
			if (title != null) {
				return title;
			}
		}
		return null;
	}

	private static String tooltip(RepositoryPlugin repo, Object... target) {
		return repo instanceof Actionable actionable ? safe(() -> actionable.tooltip(target)) : null;
	}

	private static String string(JsonObject request, String key) {
		return request.has(key) && !request.get(key)
			.isJsonNull() ? request.get(key)
				.getAsString() : null;
	}

	private static String required(JsonObject request, String key) {
		String value = string(request, key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing " + key);
		}
		return value;
	}

	interface Supplier<T> {
		T get() throws Exception;
	}

	private static <T> T safe(Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			return null;
		}
	}
}
