package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aQute.bnd.lsp.Constants;
import aQute.bnd.lsp.models.BndDocumentModel;

public class BndQuickFixProviderTest {
	@TempDir
	Path temporary;

	private final BndWorkspaceManager	workspaceManager	= new BndWorkspaceManager();
	private final BndQuickFixProvider	provider			= new BndQuickFixProvider(workspaceManager);
	private final BndDiagnosticsProvider	diagnostics			= new BndDiagnosticsProvider(workspaceManager);

	private static TextEdit onlyEdit(CodeAction action, String uri) {
		List<TextEdit> edits = action.getEdit()
			.getChanges()
			.get(uri);
		assertThat(edits).hasSize(1);
		return edits.get(0);
	}

	@Test
	public void propertyConflictOffersMergeAndRemoval() {
		File file = temporary.resolve("bnd.bnd")
			.toFile();
		String uri = file.toURI()
			.toString();
		String text = "-propertyconflicts: warning\n-runvm: -Da\n-runvm: -Db,\\\n  -Dc\nBundle-Name: x\n";
		BndDocumentModel model = new BndDocumentModel(uri, text);
		List<Diagnostic> found = diagnostics.computePropertyConflicts(model, file, null);
		assertThat(found).hasSize(1);

		List<CodeAction> actions = provider.provideQuickFixes(model, file, found);
		assertThat(actions).extracting(CodeAction::getTitle)
			.containsExactly("Rename to `-runvm.bnd` to merge values", "Remove this definition of `-runvm`");
		assertThat(actions).allSatisfy(action -> {
			assertThat(action.getKind()).isEqualTo(CodeActionKind.QuickFix);
			assertThat(action.getDiagnostics()).containsExactly(found.get(0));
		});

		TextEdit rename = onlyEdit(actions.get(0), uri);
		assertThat(rename.getRange()).isEqualTo(new Range(new Position(2, 0), new Position(2, 6)));
		assertThat(rename.getNewText()).isEqualTo("-runvm.bnd");

		TextEdit remove = onlyEdit(actions.get(1), uri);
		assertThat(remove.getRange()).isEqualTo(new Range(new Position(2, 0), new Position(4, 0)));
		assertThat(remove.getNewText()).isEmpty();
	}

	@Test
	public void mergedKeyAvoidsExistingKeys() {
		File file = temporary.resolve("bnd.bnd")
			.toFile();
		String text = "-propertyconflicts: warning\n-runvm.bnd: -Dx\n-runvm: -Da\n-runvm: -Db\n";
		BndDocumentModel model = new BndDocumentModel(file.toURI()
			.toString(), text);
		List<Diagnostic> found = diagnostics.computePropertyConflicts(model, file, null);
		List<CodeAction> actions = provider.provideQuickFixes(model, file, found);
		assertThat(actions.get(0)
			.getTitle()).isEqualTo("Rename to `-runvm.bnd-2` to merge values");
	}

	@Test
	public void plainPropertyOnlyOffersRemoval() {
		File file = temporary.resolve("bnd.bnd")
			.toFile();
		String text = "-propertyconflicts: warning\nBundle-Name: x\nBundle-Name: y";
		BndDocumentModel model = new BndDocumentModel(file.toURI()
			.toString(), text);
		List<Diagnostic> found = diagnostics.computePropertyConflicts(model, file, null);
		List<CodeAction> actions = provider.provideQuickFixes(model, file, found);
		assertThat(actions).extracting(CodeAction::getTitle)
			.containsExactly("Remove this definition of `Bundle-Name`");
		TextEdit remove = onlyEdit(actions.get(0), file.toURI()
			.toString());
		// last line without line break: delete to end of line
		assertThat(remove.getRange()).isEqualTo(new Range(new Position(2, 0), new Position(2, 14)));
	}

	@Test
	public void dataSurvivesProtocolRoundTrip() {
		File file = temporary.resolve("bnd.bnd")
			.toFile();
		String text = "Bundle-Activator: com.example.Missing\nBundle-Version: 1.0.0.${tstamp}\n";
		BndDocumentModel model = new BndDocumentModel(file.toURI()
			.toString(), text);

		Diagnostic activator = new Diagnostic(new Range(new Position(0, 0), new Position(0, 16)), "no activator");
		activator.setCode(Either.forLeft(Constants.DIAGNOSTIC_CODE_BUNDLE_ACTIVATOR));
		activator.setData(JsonParser.parseString("{\"activatorClassName\":\"com.example.Missing\"}"));

		Diagnostic version = new Diagnostic(new Range(new Position(1, 0), new Position(1, 14)), "too low");
		version.setCode(Either.forLeft(Constants.DIAGNOSTIC_CODE_BUNDLE_VERSION));
		JsonObject data = new JsonObject();
		data.addProperty("suggestedVersion", "1.1.0");
		version.setData(data);

		List<CodeAction> actions = provider.provideQuickFixes(model, file, List.of(activator, version));
		assertThat(actions).extracting(CodeAction::getTitle)
			.containsExactly("Remove the Bundle-Activator header", "Change Bundle-Version to 1.1.0.${tstamp}");

		String uri = file.toURI()
			.toString();
		assertThat(onlyEdit(actions.get(0), uri).getRange())
			.isEqualTo(new Range(new Position(0, 0), new Position(1, 0)));
		TextEdit bump = onlyEdit(actions.get(1), uri);
		assertThat(bump.getRange()).isEqualTo(new Range(new Position(1, 16), new Position(1, 31)));
		assertThat(bump.getNewText()).isEqualTo("1.1.0.${tstamp}");
	}

	@Test
	public void unknownCodesProduceNoActions() {
		File file = temporary.resolve("bnd.bnd")
			.toFile();
		BndDocumentModel model = new BndDocumentModel(file.toURI()
			.toString(), "Bundle-Name: x\n");
		Diagnostic plain = new Diagnostic(new Range(new Position(0, 0), new Position(0, 1)), "message");
		assertThat(provider.provideQuickFixes(model, file, List.of(plain))).isEmpty();
	}
}
