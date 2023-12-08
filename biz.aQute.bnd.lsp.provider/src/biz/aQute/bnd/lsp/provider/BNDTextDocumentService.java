package biz.aQute.bnd.lsp.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import aQute.bnd.help.Syntax;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IDocument;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class BNDTextDocumentService implements TextDocumentService {

	private final Map<String, IDocument> docs = new ConcurrentHashMap<>();
	private Executor executor;

	BNDTextDocumentService(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		System.out.println("BNDTextDocumentService.didOpen()");
		IDocument model = new Document(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		IDocument model = new Document(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		System.out.println("BNDTextDocumentService.didClose()");
		this.docs.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO anything to do here?
		System.out.println("BNDTextDocumentService.didSave()");
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		System.out.println("BNDTextDocumentService.semanticTokensFull()");
		return withDocument(params.getTextDocument(), document -> {

			int lines = document.getNumberOfLines();
			List<IRegion> list = new ArrayList<>(lines);
			for (int i = 0; i < lines; i++) {
				list.add(document.getLineInformation(i));
			}

			PropertiesLineReader reader = new PropertiesLineReader(document);
			List<Token> tokens = new ArrayList<>();
			LineType type;
			while ((type = reader.next()) != LineType.eof) {
				// TODO https://github.com/eclipse/lsp4e/issues/861 would be
				// good to encode
				// type/modifier not by plain int...
				if (type == LineType.entry) {
					String key = reader.key();
					IRegion region = reader.region();
					tokens.add(new Token(getLine(region, list), 0, key.length(), 0, 0));
				} else if (type == LineType.comment) {
					// TODO https://github.com/bndtools/bnd/issues/5843
					IRegion region = reader.region();
					tokens.add(new Token(getLine(region, list), 0, region.getLength(), 1, 0));
				}
			}
			SemanticTokens semanticTokens = new SemanticTokens(new ArrayList<>());
			List<Integer> data = semanticTokens.getData();
			int lastLine = 0;
			int lastStartChar = 0;
			// See
			// https://github.com/Microsoft/language-server-protocol/blob/gh-pages/_specifications/specification-3-16.md#textDocument_semanticTokens
			for (Token token : tokens) {
				// TODO https://github.com/eclipse/lsp4e/issues/861 can Token record + encoding
				// probably be part of lsp4j?
				System.out.println(token);
				int lineDelta = token.line() - lastLine;
				data.add(lineDelta);
				if (lastLine == token.line()) {
					data.add(token.startChar() - lastStartChar);
				} else {
					data.add(token.startChar());
				}
				data.add(token.length());
				data.add(token.tokenType());
				data.add(token.tokenModifiers());
				lastLine = token.line();
				lastStartChar = token.startChar();
			}
			return semanticTokens;
		});
	}

	private int getLine(IRegion region, List<IRegion> list) {
		int s = region.getOffset();
		for (int i = 0; i < list.size(); i++) {
			IRegion r = list.get(i);
			int offsetStart = r.getOffset();
			int offsetEnd = offsetStart + r.getLength();
			if (s >= offsetStart && s <= offsetEnd) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		System.out.println("BNDTextDocumentService.completion()");
		return withDocument(params.getTextDocument(), document -> {
			// TODO prefix
			return Either.forRight(new CompletionList(false, Syntax.HELP.values().stream().map(syntax -> {
				CompletionItem item = new CompletionItem();
				item.setLabel(syntax.getHeader());
				item.setInsertText(syntax.getHeader() + ": ");
				return item;
			}).toList()));
		});
	}

	private <T> CompletableFuture<T> withDocument(TextDocumentIdentifier identifier, DocumentCallable<T> callable) {
		IDocument document = docs.get(identifier.getUri());
		if (document == null) {
			return CompletableFuture.failedFuture(new IllegalStateException());
		}
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeAsync(() -> {
			try {
				return callable.call(document);
			} catch (Exception e) {
				future.completeExceptionally(e);
				return null;
			}

		}, executor);
		return future;
	}

	private interface DocumentCallable<V> {
		V call(IDocument document) throws Exception;
	}

	private static final record Token(int line, int startChar, int length, int tokenType, int tokenModifiers) {
	}

}
