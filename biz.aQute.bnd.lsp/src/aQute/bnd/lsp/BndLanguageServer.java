package aQute.bnd.lsp;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.services.BndAutoBuildService;
import aQute.bnd.lsp.services.BndSemanticTokensProvider;
import aQute.bnd.lsp.services.BndTextDocumentService;
import aQute.bnd.lsp.services.BndWorkspaceManager;
import aQute.bnd.lsp.services.BndWorkspaceService;

public class BndLanguageServer implements LanguageServer, LanguageClientAware {
	private static final Logger				logger		= LoggerFactory.getLogger(BndLanguageServer.class);
	private static final Pattern			DRIVER		= Pattern.compile("[A-Za-z][A-Za-z0-9_.-]*");

	private final ScheduledExecutorService	scheduler	= Executors.newScheduledThreadPool(2, r -> {
															Thread t = new Thread(r, "bnd-lsp-worker");
															t.setDaemon(true);
															return t;
														});

	private final BndWorkspaceManager		workspaceManager;
	private final BndTextDocumentService	textDocumentService;
	private final BndWorkspaceService		workspaceService;
	private final BndAutoBuildService		autoBuildService;

	private LanguageClient					client;
	private int								exitCode	= 0;

	public BndLanguageServer() {
		this.workspaceManager = new BndWorkspaceManager();
		this.autoBuildService = new BndAutoBuildService(workspaceManager, scheduler);
		this.textDocumentService = new BndTextDocumentService(workspaceManager, scheduler);
		this.workspaceService = new BndWorkspaceService(workspaceManager);
		this.textDocumentService.setAutoBuildService(autoBuildService);
		this.workspaceService.setAutoBuildService(autoBuildService);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		logger.info("Initializing bnd Language Server");
		JsonElement options = new Gson().toJsonTree(params.getInitializationOptions());
		boolean trusted = options.isJsonObject() && options.getAsJsonObject().has("workspaceTrusted")
			&& options.getAsJsonObject().get("workspaceTrusted").getAsBoolean();
		workspaceService.configureEffectiveProperties(textDocumentService, trusted);

		// Automatic builds are on unless the client switches them off
		JsonElement autoBuild = options.isJsonObject() ? options.getAsJsonObject().get("autoBuild") : null;
		autoBuildService.configure(autoBuild == null || !autoBuild.isJsonPrimitive() || autoBuild.getAsBoolean(),
			trusted);

		// The client names itself so workspaces can route tool specific
		// directories with the ${driver} macro
		JsonElement driver = options.isJsonObject() ? options.getAsJsonObject().get("driver") : null;
		if (driver != null && driver.isJsonPrimitive() && DRIVER.matcher(driver.getAsString()).matches()) {
			Workspace.setDriver(driver.getAsString());
		}

		ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

		// Completion
		CompletionOptions completionOptions = new CompletionOptions();
		completionOptions.setTriggerCharacters(Arrays.asList("-", ":", "=", "$", "{", ";", ","));
		completionOptions.setResolveProvider(false);
		capabilities.setCompletionProvider(completionOptions);

		// Hover
		capabilities.setHoverProvider(true);

		// Semantic Tokens
		SemanticTokensWithRegistrationOptions semanticOptions = new SemanticTokensWithRegistrationOptions();
		semanticOptions.setLegend(BndSemanticTokensProvider.getLegend());
		semanticOptions.setFull(true);
		capabilities.setSemanticTokensProvider(semanticOptions);

		// Document Symbols, Folding, Definition
		capabilities.setDocumentSymbolProvider(true);
		capabilities.setFoldingRangeProvider(true);
		capabilities.setDefinitionProvider(true);
		capabilities.setDocumentFormattingProvider(false);

		// Code Actions
		capabilities.setCodeActionProvider(new CodeActionOptions(Arrays.asList("quickfix", "source")));

		// Execute Command
		ExecuteCommandOptions cmdOptions = new ExecuteCommandOptions(Arrays.asList(Constants.COMMAND_BUILD_PROJECT,
			Constants.COMMAND_BUILD_WORKSPACE, Constants.COMMAND_BUILD_CLEAN, Constants.COMMAND_BUILD_AUTO,
			Constants.COMMAND_RESOLVE_BNDRUN,
			Constants.COMMAND_BASELINE, Constants.COMMAND_MACRO_EXPAND, Constants.COMMAND_REPO_LIST,
			Constants.COMMAND_JAR_PRINT, Constants.COMMAND_JAR_PRINT_TEXT, Constants.COMMAND_EFFECTIVE_PROPERTIES,
			Constants.COMMAND_RESOLUTION_ANALYZE, Constants.COMMAND_LAUNCH_PREPARE,
			Constants.COMMAND_LAUNCH_DISPOSE, Constants.COMMAND_EXPORT, Constants.COMMAND_REPOSITORIES_LIST,
			Constants.COMMAND_REPOSITORIES_BUNDLES, Constants.COMMAND_REPOSITORIES_VERSIONS,
			Constants.COMMAND_REPOSITORIES_FEATURE, Constants.COMMAND_REPOSITORIES_GET,
			Constants.COMMAND_REPOSITORIES_SEARCH, Constants.COMMAND_REPOSITORIES_ACTIONS,
			Constants.COMMAND_REPOSITORIES_RUN_ACTION, Constants.COMMAND_REPOSITORIES_REFRESH,
			Constants.COMMAND_REPOSITORIES_PUT, Constants.COMMAND_REPOSITORIES_DOWNLOAD,
			Constants.COMMAND_WORKSPACE_OFFLINE, Constants.COMMAND_WIZARD_LIST, Constants.COMMAND_WIZARD_PLAN,
			Constants.COMMAND_WIZARD_APPLY, Constants.COMMAND_WIZARD_DISPOSE, Constants.COMMAND_WIZARD_CREATE));
		capabilities.setExecuteCommandProvider(cmdOptions);
		capabilities.setExperimental(Map.of(Constants.CAPABILITY_AUTO_BUILD, true));

		return CompletableFuture.completedFuture(new InitializeResult(capabilities, serverInfo()));
	}

	/**
	 * Names the server with the Bundle-Version and Git-SHA of its JAR, so
	 * clients can show which build runs.
	 */
	static ServerInfo serverInfo() {
		String version = null;
		try {
			URL location = BndLanguageServer.class.getProtectionDomain()
				.getCodeSource()
				.getLocation();
			File file = new File(location.toURI());
			if (file.isFile()) {
				try (JarFile jar = new JarFile(file)) {
					version = version(jar.getManifest());
				}
			}
		} catch (Exception e) {
			logger.debug("Cannot read the manifest of the bnd Language Server", e);
		}
		return new ServerInfo("bnd Language Server", version);
	}

	static String version(Manifest manifest) {
		if (manifest == null) {
			return null;
		}
		Attributes main = manifest.getMainAttributes();
		String version = main.getValue(aQute.bnd.osgi.Constants.BUNDLE_VERSION);
		String sha = main.getValue("Git-SHA");
		if (version == null) {
			return null;
		}
		return sha == null || sha.isBlank() ? version : version + " (" + sha + ")";
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		logger.info("Shutting down bnd Language Server");
		workspaceService.shutdown();
		autoBuildService.close();
		workspaceManager.shutdown();
		scheduler.shutdown();
		return CompletableFuture.completedFuture(new Object());
	}

	@Override
	public void exit() {
		logger.info("Exiting bnd Language Server with code {}", exitCode);
		System.exit(exitCode);
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return textDocumentService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
		this.textDocumentService.setClient(client);
		this.workspaceService.setClient(client);
		if (client instanceof BndLanguageClient bndClient) {
			autoBuildService.setListener(bndClient::buildStatus);
		}
	}

	public BndWorkspaceManager getWorkspaceManager() {
		return workspaceManager;
	}
}
