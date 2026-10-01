package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import aQute.bnd.lsp.models.BndDocumentModel;

public class BndCompletionProviderTest {

	@Test
	public void testHeaderKeyCompletion() {
		BndWorkspaceManager wsManager = new BndWorkspaceManager();
		BndCompletionProvider provider = new BndCompletionProvider(wsManager);

		String text = "-build";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		CompletionParams params = new CompletionParams(new TextDocumentIdentifier("file:///test/bnd.bnd"),
			new Position(0, 6));
		Either<List<CompletionItem>, CompletionList> result = provider.provideCompletions(model, params);

		assertThat(result.isRight()).isTrue();
		List<CompletionItem> items = result.getRight().getItems();
		assertThat(items).isNotEmpty();
		assertThat(items.stream().anyMatch(i -> i.getLabel().equals("-buildpath"))).isTrue();
	}

	@Test
	public void testMacroCompletion() {
		BndWorkspaceManager wsManager = new BndWorkspaceManager();
		BndCompletionProvider provider = new BndCompletionProvider(wsManager);

		String text = "Bundle-Version: ${";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		CompletionParams params = new CompletionParams(new TextDocumentIdentifier("file:///test/bnd.bnd"),
			new Position(0, 18));
		Either<List<CompletionItem>, CompletionList> result = provider.provideCompletions(model, params);

		assertThat(result.isRight()).isTrue();
		List<CompletionItem> items = result.getRight().getItems();
		assertThat(items).isNotEmpty();
		assertThat(items.stream().anyMatch(i -> i.getLabel().equals("${tstamp}"))).isTrue();
	}

	@Test
	public void testJavaVersionCompletion() {
		BndWorkspaceManager wsManager = new BndWorkspaceManager();
		BndCompletionProvider provider = new BndCompletionProvider(wsManager);

		String text = "javac.source: ";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		CompletionParams params = new CompletionParams(new TextDocumentIdentifier("file:///test/bnd.bnd"),
			new Position(0, 14));
		Either<List<CompletionItem>, CompletionList> result = provider.provideCompletions(model, params);

		assertThat(result.isRight()).isTrue();
		List<CompletionItem> items = result.getRight().getItems();
		assertThat(items).isNotEmpty();
		assertThat(items.stream().anyMatch(i -> i.getLabel().equals("17"))).isTrue();
	}
}
