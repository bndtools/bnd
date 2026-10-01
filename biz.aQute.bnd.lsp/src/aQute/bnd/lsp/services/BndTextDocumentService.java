package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderClause;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.lsp.models.BndToken;
import aQute.bnd.lsp.models.BndTokenType;

public class BndTextDocumentService implements TextDocumentService {
	private static final Logger						logger			= LoggerFactory
		.getLogger(BndTextDocumentService.class);

	private final Map<String, BndDocumentModel>		documents		= new ConcurrentHashMap<>();
	private final BndWorkspaceManager				workspaceManager;
	private final BndHoverProvider					hoverProvider;
	private final BndCompletionProvider				completionProvider;
	private final BndSemanticTokensProvider			semanticTokensProvider;
	private final BndDiagnosticsProvider			diagnosticsProvider;
	private final ScheduledExecutorService			scheduler;
	private final Map<String, ScheduledFuture<?>>	pendingDebounce	= new ConcurrentHashMap<>();

	private LanguageClient							client;
	private final Map<String, DocumentSnapshot> snapshots = new ConcurrentHashMap<>();

	public record DocumentSnapshot(String text, int version) {}

	public DocumentSnapshot getSnapshot(String uri) {
		return snapshots.get(uri);
	}

	public BndTextDocumentService(BndWorkspaceManager workspaceManager, ScheduledExecutorService scheduler) {
		this.workspaceManager = workspaceManager;
		this.scheduler = scheduler;
		this.hoverProvider = new BndHoverProvider(workspaceManager);
		this.completionProvider = new BndCompletionProvider(workspaceManager);
		this.semanticTokensProvider = new BndSemanticTokensProvider();
		this.diagnosticsProvider = new BndDiagnosticsProvider(workspaceManager);
	}

	public void setClient(LanguageClient client) {
		this.client = client;
	}

	public BndDocumentModel getDocument(String uri) {
		return documents.get(uri);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		String uri = params.getTextDocument()
			.getUri();
		String text = params.getTextDocument()
			.getText();
		BndDocumentModel model = new BndDocumentModel(uri, text);
		documents.put(uri, model);
		snapshots.put(uri, new DocumentSnapshot(text, params.getTextDocument().getVersion()));
		triggerDiagnostics(uri);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		String uri = params.getTextDocument()
			.getUri();
		BndDocumentModel model = documents.get(uri);
		if (model != null) {
			List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
			if (!changes.isEmpty()) {
				// Full sync for now
				String newText = changes.get(changes.size() - 1)
					.getText();
				model.update(newText);
				snapshots.put(uri, new DocumentSnapshot(newText, params.getTextDocument().getVersion()));
				triggerDiagnostics(uri);
			}
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		String uri = params.getTextDocument()
			.getUri();
		documents.remove(uri);
		snapshots.remove(uri);
		if (client != null) {
			client.publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
		}
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		String uri = params.getTextDocument()
			.getUri();
		triggerDiagnostics(uri);
	}

	private void triggerDiagnostics(String uri) {
		if (client == null)
			return;

		ScheduledFuture<?> existing = pendingDebounce.get(uri);
		if (existing != null && !existing.isDone()) {
			existing.cancel(false);
		}

		ScheduledFuture<?> future = scheduler.schedule(() -> {
			BndDocumentModel model = documents.get(uri);
			if (model != null) {
				List<Diagnostic> diagnostics = diagnosticsProvider.computeDiagnostics(model);
				client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
			}
		}, 300, TimeUnit.MILLISECONDS);

		pendingDebounce.put(uri, future);
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = position.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return Either.forRight(new CompletionList(false, Collections.emptyList()));
			}
			return completionProvider.provideCompletions(model, position);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return null;
			}
			return hoverProvider.provideHover(model, params);
		});
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return new SemanticTokens(Collections.emptyList());
			}
			return semanticTokensProvider.provideSemanticTokens(model);
		});
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
		DocumentSymbolParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return Collections.emptyList();
			}

			List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
			for (BndHeaderEntry entry : model.getHeaderEntries()) {
				Range headerRange = new Range(new Position(entry.getStartLine(), 0),
					new Position(entry.getEndLine(), entry.getValueLength()));
				Range selectionRange = new Range(new Position(entry.getStartLine(), 0),
					new Position(entry.getStartLine(), entry.getKeyLength()));

				DocumentSymbol headerSymbol = new DocumentSymbol(entry.getKey(), SymbolKind.Property, headerRange,
					selectionRange, entry.getValue());

				List<DocumentSymbol> clauseSymbols = new ArrayList<>();
				for (BndHeaderClause clause : entry.getClauses()) {
					Range clauseRange = new Range(new Position(clause.getStartLine(), 0),
						new Position(clause.getEndLine(), clause.getLength()));
					clauseSymbols.add(new DocumentSymbol(clause.getName(), SymbolKind.Module, clauseRange, clauseRange,
						clause.getAttrs()
							.toString()));
				}
				headerSymbol.setChildren(clauseSymbols);
				result.add(Either.forRight(headerSymbol));
			}

			return result;
		});
	}

	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return Collections.emptyList();
			}

			List<FoldingRange> ranges = new ArrayList<>();
			for (BndHeaderEntry entry : model.getHeaderEntries()) {
				if (entry.getEndLine() > entry.getStartLine()) {
					FoldingRange r = new FoldingRange(entry.getStartLine(), entry.getEndLine());
					ranges.add(r);
				}
			}
			return ranges;
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
		DefinitionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			String uri = params.getTextDocument()
				.getUri();
			BndDocumentModel model = documents.get(uri);
			if (model == null) {
				return Either.forLeft(Collections.emptyList());
			}

			Position pos = params.getPosition();
			BndHeaderEntry entry = model.getEntryAt(pos.getLine());
			if (entry == null) {
				return Either.forLeft(Collections.emptyList());
			}

			File docFile = getFileFromUri(uri);
			if (docFile == null) {
				return Either.forLeft(Collections.emptyList());
			}

			// Check for -include
			if (entry.getKey()
				.equals("-include") || entry.getKey()
					.equals("include")) {
				for (BndHeaderClause clause : entry.getClauses()) {
					File target = new File(docFile.getParentFile(), clause.getName());
					if (target.isFile()) {
						Location loc = new Location(target.toURI()
							.toString(), new Range(new Position(0, 0), new Position(0, 0)));
						return Either.forLeft(Collections.singletonList(loc));
					}
				}
			}

			// Check for project in -buildpath
			if (entry.getKey()
				.equals("-buildpath")) {
				Workspace ws = workspaceManager.getWorkspaceForFile(docFile);
				if (ws != null) {
					for (BndHeaderClause clause : entry.getClauses()) {
						try {
							Project p = ws.getProject(clause.getName());
							if (p != null) {
								File bndFile = p.getFile("bnd.bnd");
								if (bndFile.isFile()) {
									Location loc = new Location(bndFile.toURI()
										.toString(), new Range(new Position(0, 0), new Position(0, 0)));
									return Either.forLeft(Collections.singletonList(loc));
								}
							}
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}

			return Either.forLeft(Collections.emptyList());
		});
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			List<Either<Command, CodeAction>> actions = new ArrayList<>();
			String uri = params.getTextDocument()
				.getUri();

			if (uri.endsWith(".bndrun")) {
				CodeAction resolveAction = new CodeAction("Resolve Runbundles");
				resolveAction.setCommand(new Command("Resolve", aQute.bnd.lsp.Constants.COMMAND_RESOLVE_BNDRUN,
					Collections.singletonList(uri)));
				actions.add(Either.forRight(resolveAction));
			}

			return actions;
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private static File getFileFromUri(String uriStr) {
		try {
			URI uri = URI.create(uriStr);
			if ("file".equalsIgnoreCase(uri.getScheme())) {
				return new File(uri.getPath());
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
}
