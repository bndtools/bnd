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

	private static List<CompletionItem> complete(String text) {
		BndCompletionProvider provider = new BndCompletionProvider(new BndWorkspaceManager());
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);
		CompletionParams params = new CompletionParams(new TextDocumentIdentifier("file:///test/bnd.bnd"),
			new Position(0, text.length()));
		return provider.provideCompletions(model, params)
			.getRight()
			.getItems();
	}

	@Test
	public void macroCompletionListsAllBndlibMacrosWithHelp() {
		List<CompletionItem> items = complete("Bundle-Version: ${");
		assertThat(items).extracting(CompletionItem::getLabel)
			.contains("${findfile}", "${foreach}", "${classes}", "${sha1}", "${versionmask}", "${if}");
		assertThat(items).filteredOn(i -> i.getLabel()
			.equals("${if}"))
			.singleElement()
			.satisfies(i -> assertThat(i.getDocumentation()).isNotNull());
	}

	@Test
	public void runeeCompletionUsesBndlibExecutionEnvironments() {
		List<CompletionItem> items = complete("-runee: ");
		assertThat(items).extracting(CompletionItem::getLabel)
			.contains("JavaSE-1.8", "JavaSE-17")
			.doesNotContain("JavaSE-50", "JavaSE-1.7", "<UNKNOWN>");
	}

	@Test
	public void clauseCompletionOffersAttributesDirectivesAndValues() {
		assertThat(complete("Import-Package: org.foo;")).extracting(CompletionItem::getLabel)
			.contains("version=", "resolution:=");
		assertThat(complete("Import-Package: org.foo;resolution:=")).extracting(CompletionItem::getLabel)
			.contains("optional", "mandatory");
		assertThat(complete("Import-Package: org.foo;version=1, org.bar")).extracting(CompletionItem::getLabel)
			.doesNotContain("resolution:=");
		assertThat(BndCompletionProvider.lastClausePart("a;b=1, c;re")).isEqualTo("re");
		assertThat(BndCompletionProvider.lastClausePart("a;b=1, c")).isNull();
	}
}
