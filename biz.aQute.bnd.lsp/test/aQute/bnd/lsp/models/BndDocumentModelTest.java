package aQute.bnd.lsp.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class BndDocumentModelTest {

	@Test
	public void testParseSimpleDocument() {
		String text = """
			# Sample bnd file
			Bundle-SymbolicName: com.example.sample
			Bundle-Version: 1.0.0
			-buildpath: \\
				osgi.core;version=6.0,\\
				osgi.annotation
			""";

		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		List<BndHeaderEntry> entries = model.getHeaderEntries();
		assertThat(entries).hasSize(3);

		BndHeaderEntry bsn = entries.get(0);
		assertThat(bsn.getKey()).isEqualTo("Bundle-SymbolicName");
		assertThat(bsn.getValue()).isEqualTo("com.example.sample");
		assertThat(bsn.isInstruction()).isFalse();

		BndHeaderEntry bp = entries.get(2);
		assertThat(bp.getKey()).isEqualTo("-buildpath");
		assertThat(bp.isInstruction()).isTrue();
		assertThat(bp.getClauses()).hasSize(2);
		assertThat(bp.getClauses().get(0).getName()).isEqualTo("osgi.core");
		assertThat(bp.getClauses().get(0).getAttrs().get("version")).isEqualTo("6.0");
	}

	@Test
	public void testParseMacros() {
		String text = "Bundle-Version: ${version;===;${v}}\n";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		List<BndHeaderEntry> entries = model.getHeaderEntries();
		assertThat(entries).hasSize(1);

		List<BndMacroRef> macros = entries.get(0).getMacros();
		assertThat(macros).isNotEmpty();
		assertThat(macros.get(0).getName()).isEqualTo("version");
	}

	@Test
	public void testPositionOffsetConversions() {
		String text = "Line 0\nLine 1\nLine 2\n";
		BndDocumentModel model = new BndDocumentModel("file:///test/bnd.bnd", text);

		assertThat(model.getLineCount()).isEqualTo(4);
	}
}
