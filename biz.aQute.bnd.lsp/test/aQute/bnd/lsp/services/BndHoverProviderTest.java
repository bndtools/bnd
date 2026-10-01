package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import aQute.bnd.lsp.models.BndDocumentModel;

public class BndHoverProviderTest {

	@Test
	public void testHoverOnInstruction() {
		BndWorkspaceManager wsManager = new BndWorkspaceManager();
		BndHoverProvider provider = new BndHoverProvider(wsManager);

		String text = "-buildpath: osgi.core\n";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		HoverParams params = new HoverParams(new TextDocumentIdentifier("file:///test/bnd.bnd"), new Position(0, 3));
		Hover hover = provider.provideHover(model, params);

		assertThat(hover).isNotNull();
		assertThat(hover.getContents().isRight()).isTrue();
		MarkupContent content = hover.getContents().getRight();
		assertThat(content.getValue()).contains("`-buildpath`");
	}

	@Test
	public void testHoverOnMacro() {
		BndWorkspaceManager wsManager = new BndWorkspaceManager();
		BndHoverProvider provider = new BndHoverProvider(wsManager);

		String text = "Bundle-Version: ${tstamp}\n";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		HoverParams params = new HoverParams(new TextDocumentIdentifier("file:///test/bnd.bnd"), new Position(0, 18));
		Hover hover = provider.provideHover(model, params);

		assertThat(hover).isNotNull();
		MarkupContent content = hover.getContents().getRight();
		assertThat(content.getValue()).contains("${tstamp}");
	}
}
