package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import biz.aQute.resolve.RunResolution;

public class BndWorkspaceService implements WorkspaceService {
	private static final Logger			logger	= LoggerFactory.getLogger(BndWorkspaceService.class);

	private final BndWorkspaceManager	workspaceManager;
	private final BndLaunchService		launchService;
	private LanguageClient				client;
	private BndTextDocumentService documents;
	private boolean effectivePropertiesTrusted;

	public void configureEffectiveProperties(BndTextDocumentService documents, boolean trusted) {
		this.documents = documents;
		this.effectivePropertiesTrusted = trusted;
	}

	public BndWorkspaceService(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
		this.launchService = new BndLaunchService(workspaceManager);
	}

	public void shutdown() {
		launchService.shutdown();
	}

	public void setClient(LanguageClient client) {
		this.client = client;
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String command = params.getCommand();
			List<Object> args = params.getArguments();

			try {
				switch (command) {
					case Constants.COMMAND_EFFECTIVE_PROPERTIES :
						return executeEffectiveProperties(args);
					case Constants.COMMAND_RESOLVE_BNDRUN :
						return executeResolveBndrun(args);
					case Constants.COMMAND_BUILD_PROJECT :
						return executeBuildProject(args);
					case Constants.COMMAND_BUILD_WORKSPACE :
						return executeBuildWorkspace(args);
					case Constants.COMMAND_MACRO_EXPAND :
						return executeMacroExpand(args);
					case Constants.COMMAND_REPO_LIST :
						return executeRepoList(args);
					case Constants.COMMAND_JAR_PRINT :
						return executeJarPrint(args);
					case Constants.COMMAND_LAUNCH_PREPARE :
						return executeLaunchPrepare(args);
					case Constants.COMMAND_LAUNCH_DISPOSE :
						return executeLaunchDispose(args);
					default :
						logger.warn("Unknown command: {}", command);
						return null;
				}
			} catch (Exception e) {
				logger.error("Error executing command {}", command, e);
				Map<String, Object> err = new HashMap<>();
				err.put("error", e.getMessage());
				return err;
			}
		});
	}

	private Object executeEffectiveProperties(List<Object> args) throws Exception {
		if (!effectivePropertiesTrusted || documents == null) {
			throw new IllegalStateException("Effective properties require a trusted workspace.");
		}
		if (args == null || args.size() != 1) {
			throw new IllegalArgumentException("Expected one effective-properties request.");
		}
		JsonObject request = new Gson().toJsonTree(args.get(0)).getAsJsonObject();
		String uri = request.get("uri").getAsString();
		File file = new File(URI.create(uri));
		if (!file.isFile() || !(file.getName().endsWith(".bnd") || file.getName().endsWith(".bndrun"))) {
			throw new IllegalArgumentException("Expected an existing .bnd or .bndrun file.");
		}
		var snapshot = documents.getSnapshot(uri);
		Integer version = request.has("documentVersion") && !request.get("documentVersion").isJsonNull()
			? request.get("documentVersion").getAsInt() : null;
		if (version != null && (snapshot == null || snapshot.version() != version)) {
			return Map.of("error", "Document changed; refresh Effective properties.", "code", "staleDocument");
		}
		boolean expanded = !request.has("expanded") || request.get("expanded").getAsBoolean();
		boolean merged = !request.has("merged") || request.get("merged").getAsBoolean();
		return new BndEffectivePropertiesService().evaluate(file, snapshot == null ? null : snapshot.text(),
			snapshot == null ? null : snapshot.version(), expanded, merged);
	}

	private Object executeLaunchPrepare(List<Object> args) throws Exception {
		if (!effectivePropertiesTrusted) {
			throw new IllegalStateException("Launching requires a trusted workspace.");
		}
		if (args == null || args.size() != 1) {
			throw new IllegalArgumentException("Expected one launch request.");
		}
		JsonObject request = new Gson().toJsonTree(args.get(0))
			.getAsJsonObject();
		File file = getFileFromUri(request.get("uri")
			.getAsString());
		BndLaunchService.Kind kind = request.has("kind") && "test".equals(request.get("kind")
			.getAsString()) ? BndLaunchService.Kind.test : BndLaunchService.Kind.run;
		List<String> tests = new ArrayList<>();
		if (request.has("tests") && request.get("tests")
			.isJsonArray()) {
			request.getAsJsonArray("tests")
				.forEach(test -> tests.add(test.getAsString()));
		}
		boolean build = !request.has("build") || request.get("build")
			.getAsBoolean();
		try {
			return launchService.prepare(new BndLaunchService.Request(file, kind, tests, build));
		} catch (BndLaunchService.LaunchException e) {
			return Map.of("error", e.getMessage(), "errors", e.getErrors());
		}
	}

	private Object executeLaunchDispose(List<Object> args) {
		if (args == null || args.isEmpty() || args.get(0) == null) {
			throw new IllegalArgumentException("Missing launch id");
		}
		String id = args.get(0) instanceof String s ? s
			: new Gson().toJsonTree(args.get(0))
				.getAsString();
		return Map.of("disposed", launchService.dispose(id));
	}

	private Object executeResolveBndrun(List<Object> args) throws Exception {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("Missing .bndrun file URI argument");
		}

		String uriStr = args.get(0)
			.toString();
		File file = getFileFromUri(uriStr);
		if (file == null || !file.isFile()) {
			throw new IllegalArgumentException("Invalid file: " + uriStr);
		}

		Workspace ws = workspaceManager.getWorkspaceForFile(file);
		if (ws == null) {
			throw new IllegalStateException("Not in a bnd workspace: " + file);
		}

		try (biz.aQute.resolve.Bndrun bndrun = biz.aQute.resolve.Bndrun.createBndrun(ws, file)) {
			RunResolution res = bndrun.resolve();
			if (res.isOK()) {
				bndrun.update(res, false, true);
				workspaceManager.refreshWorkspace(file);

				Map<String, Object> result = new HashMap<>();
				result.put("success", true);
				result.put("resolvedBundles", res.getRunBundlesAsString());
				return result;
			} else {
				Map<String, Object> result = new HashMap<>();
				result.put("success", false);
				result.put("error", res.report(true));
				return result;
			}
		}
	}

	private Object executeBuildProject(List<Object> args) throws Exception {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("Missing project file URI");
		}

		File file = getFileFromUri(args.get(0)
			.toString());
		Project project = workspaceManager.getProjectForFile(file);
		if (project == null) {
			throw new IllegalStateException("Could not find project for: " + file);
		}

		File[] built = project.build();
		List<String> files = new ArrayList<>();
		if (built != null) {
			for (File f : built) {
				files.add(f.getAbsolutePath());
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("success", project.isOk());
		result.put("builtFiles", files);
		result.put("errors", project.getErrors());
		result.put("warnings", project.getWarnings());
		return result;
	}

	private Object executeBuildWorkspace(List<Object> args) throws Exception {
		File file = (args != null && !args.isEmpty()) ? getFileFromUri(args.get(0)
			.toString()) : null;
		Workspace ws = file != null ? workspaceManager.getWorkspaceForFile(file) : null;
		if (ws == null) {
			throw new IllegalStateException("Workspace not found");
		}

		List<String> builtProjects = new ArrayList<>();
		for (Project p : ws.getAllProjects()) {
			p.build();
			if (p.isOk()) {
				builtProjects.add(p.getName());
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("builtProjects", builtProjects);
		return result;
	}

	private Object executeMacroExpand(List<Object> args) {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("Missing macro expression");
		}

		String expr = args.get(0)
			.toString();
		File contextFile = args.size() > 1 ? getFileFromUri(args.get(1)
			.toString()) : null;
		String expanded = workspaceManager.expandMacro(expr, contextFile);

		Map<String, Object> result = new HashMap<>();
		result.put("expression", expr);
		result.put("result", expanded);
		return result;
	}

	private Object executeRepoList(List<Object> args) {
		File contextFile = (args != null && !args.isEmpty()) ? getFileFromUri(args.get(0)
			.toString()) : null;
		List<RepositoryPlugin> repos = workspaceManager.getRepositories(contextFile);

		List<Map<String, Object>> repoList = new ArrayList<>();
		for (RepositoryPlugin r : repos) {
			Map<String, Object> entry = new HashMap<>();
			entry.put("name", r.getName());
			entry.put("location", r.getLocation());
			try {
				entry.put("bundles", r.list(null));
			} catch (Exception e) {
				entry.put("bundles", Collections.emptyList());
			}
			repoList.add(entry);
		}

		Map<String, Object> result = new HashMap<>();
		result.put("repositories", repoList);
		return result;
	}

	private Object executeJarPrint(List<Object> args) throws Exception {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("Missing jar file path");
		}

		File jarFile = new File(args.get(0)
			.toString());
		if (!jarFile.isFile()) {
			throw new IllegalArgumentException("Jar file does not exist: " + jarFile);
		}

		try (Jar jar = new Jar(jarFile)) {
			Map<String, Object> result = new HashMap<>();
			if (jar.getManifest() != null) {
				Map<String, String> headers = new HashMap<>();
				jar.getManifest()
					.getMainAttributes()
					.forEach((k, v) -> headers.put(k.toString(), v.toString()));
				result.put("manifest", headers);
			}
			return result;
		}
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		for (FileEvent event : params.getChanges()) {
			File file = getFileFromUri(event.getUri());
			if (file != null) {
				workspaceManager.refreshWorkspace(file);
			}
		}
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		logger.debug("Workspace configuration changed: {}", params.getSettings());
	}

	private static File getFileFromUri(String uriStr) {
		return BndWorkspaceManager.getFileFromUri(uriStr);
	}
}
