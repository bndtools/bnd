package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
	public void testIncludeHoverLinksIncludedFiles(@TempDir Path dir) throws Exception {
		Files.writeString(Files.createDirectories(dir.resolve("cnf"))
			.resolve("build.bnd"), "");
		Path included = Files.createDirectories(dir.resolve("launch"))
			.resolve("base.bndrun");
		Files.writeString(included, "-runfw: x\n");
		Path file = dir.resolve("app.bndrun");
		String text = "home: launch\n-include: ~${home}/base.bndrun, -missing.bnd\n";
		Files.writeString(file, text);
		String uri = file.toUri()
			.toString();
		BndHoverProvider provider = new BndHoverProvider(new BndWorkspaceManager());
		BndDocumentModel model = new BndDocumentModel(uri, text);

		String link = "[`" + included.toFile()
			.getAbsolutePath() + "`](" + included.toUri() + ")";
		String onMacro = provider.provideHover(model, new HoverParams(new TextDocumentIdentifier(uri), new Position(1, 13)))
			.getContents()
			.getRight()
			.getValue();
		assertThat(onMacro).contains("Macro `${home}`", "Included files", link)
			.contains("`" + dir.resolve("missing.bnd")
				.toFile()
				.getAbsolutePath() + "` (optional, does not exist)");

		Hover onPath = provider.provideHover(model, new HoverParams(new TextDocumentIdentifier(uri), new Position(1, 22)));
		assertThat(onPath.getContents()
			.getRight()
			.getValue()).contains(link);
		assertThat(provider.provideHover(model, new HoverParams(new TextDocumentIdentifier(uri), new Position(1, 3)))
			.getContents()
			.getRight()
			.getValue()).contains("`-include`", link);
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
