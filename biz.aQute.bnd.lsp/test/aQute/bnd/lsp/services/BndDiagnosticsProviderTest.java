package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;

import aQute.bnd.lsp.Constants;
import aQute.bnd.lsp.models.BndDocumentModel;

public class BndDiagnosticsProviderTest {
	@TempDir
	Path temporary;

	private List<Diagnostic> conflicts(String name, String text) {
		File file = temporary.resolve(name)
			.toFile();
		BndDocumentModel model = new BndDocumentModel(file.toURI()
			.toString(), text);
		return new BndDiagnosticsProvider(new BndWorkspaceManager()).computePropertyConflicts(model, file, null);
	}

	@Test
	public void duplicateDefinitionsAreReportedWithKeyRanges() {
		String text = "-propertyconflicts: warning\n-runvm: -Da\n-runvm: -Db\nBundle-Name: x\n  Bundle-Name: y\n";
		List<Diagnostic> diagnostics = conflicts("bnd.bnd", text);

		assertThat(diagnostics).hasSize(2);
		Diagnostic runvm = diagnostics.get(0);
		assertThat(runvm.getCode()
			.getLeft()).isEqualTo(Constants.DIAGNOSTIC_CODE_PROPERTY_CONFLICT);
		assertThat(runvm.getSeverity()).isEqualTo(DiagnosticSeverity.Warning);
		assertThat(runvm.getRange()).isEqualTo(new Range(new Position(2, 0), new Position(2, 6)));
		assertThat(runvm.getMessage()
			.getLeft()).contains("`-runvm` is defined more than once");
		JsonObject data = (JsonObject) runvm.getData();
		assertThat(data.get("key")
			.getAsString()).isEqualTo("-runvm");
		assertThat(data.get("mergeable")
			.getAsBoolean()).isTrue();
		assertThat(data.get("kind")
			.getAsString()).isEqualTo("DUPLICATE");

		Diagnostic name = diagnostics.get(1);
		assertThat(name.getRange()).isEqualTo(new Range(new Position(4, 2), new Position(4, 13)));
		assertThat(((JsonObject) name.getData()).get("mergeable")
			.getAsBoolean()).isFalse();
	}

	@Test
	public void errorPolicyProducesErrors() {
		List<Diagnostic> diagnostics = conflicts("bnd.bnd", "-propertyconflicts: error\na: 1\na: 2\n");
		assertThat(diagnostics).hasSize(1);
		assertThat(diagnostics.get(0)
			.getSeverity()).isEqualTo(DiagnosticSeverity.Error);
	}

	@Test
	public void defaultPolicyIsSilent() {
		assertThat(conflicts("bnd.bnd", "a: 1\na: 2\n")).isEmpty();
	}

	@Test
	public void includeShadowingIsReportedOnTheRootDefinition() throws Exception {
		Files.writeString(temporary.resolve("included.bnd"), "-runvm: -Dincluded\n");
		String text = "-propertyconflicts: warning\n-include: included.bnd\n-runvm: -Droot\n";
		List<Diagnostic> diagnostics = conflicts("launch.bndrun", text);

		assertThat(diagnostics).hasSize(1);
		Diagnostic shadowed = diagnostics.get(0);
		assertThat(shadowed.getMessage()
			.getLeft()).contains("is shadowed by -include");
		assertThat(shadowed.getRange()).isEqualTo(new Range(new Position(2, 0), new Position(2, 6)));
		assertThat(((JsonObject) shadowed.getData()).get("kind")
			.getAsString()).isEqualTo("INCLUDE");
	}
}
