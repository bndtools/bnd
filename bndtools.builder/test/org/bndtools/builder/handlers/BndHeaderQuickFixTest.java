package org.bndtools.builder.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BndHeaderQuickFixTest {

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void markerResolutionUsesLiveBufferAndOnlySavesCleanFiles(boolean dirty) throws Exception {
		IMarker marker = mock(IMarker.class);
		IFile file = mock(IFile.class);
		ITextFileBufferManager manager = mock(ITextFileBufferManager.class);
		ITextFileBuffer buffer = mock(ITextFileBuffer.class);
		Path path = new Path("/project/bnd.bnd");
		Document document = new Document("# unsaved\nBundle-Version: 1.0.0\n");
		when(marker.getResource()).thenReturn(file);
		when(file.getFullPath()).thenReturn(path);
		when(manager.getTextFileBuffer(path, LocationKind.IFILE)).thenReturn(buffer);
		when(buffer.getDocument()).thenReturn(document);
		when(buffer.isDirty()).thenReturn(dirty);
		new BndHeaderQuickFix("Bundle-Version", "2.0.0", "Replace").run(marker, manager);
		assertThat(document.get()).isEqualTo("# unsaved\nBundle-Version: 2.0.0\n");
		verify(manager).connect(path, LocationKind.IFILE, null);
		verify(manager).disconnect(path, LocationKind.IFILE, null);
		if (dirty)
			verify(buffer, never()).commit(null, false);
		else
			verify(buffer).commit(null, false);
	}

	@Test
	void replacesLiveHeaderWithoutChangingUnsavedText() throws Exception {
		Document document = new Document("# unsaved\r\nBundle-Activator: old.\\\r\n Activator\r\nOther: keep\r\n");
		BndHeaderQuickFix fix = new BndHeaderQuickFix("Bundle-Activator", "new.Activator", "Replace");
		document.replace(0, 0, "# shifted\r\n");
		fix.apply(document);
		assertThat(document.get()).isEqualTo("# shifted\r\n# unsaved\r\nBundle-Activator: new.Activator\r\nOther: keep\r\n");
		assertThat(fix.getSelection(document).x).isEqualTo(document.get().indexOf("\r\nOther"));
	}

	@Test
	void replacesEffectiveDuplicateHeader() {
		Document document = new Document("Bundle-Version: 1.0.0\nBundle-Version: 2.0.0\nOther: keep\n");
		new BndHeaderQuickFix("Bundle-Version", "3.0.0", "Replace").apply(document);
		assertThat(document.get()).isEqualTo("Bundle-Version: 1.0.0\nBundle-Version: 3.0.0\nOther: keep\n");
	}

	@Test
	void removesWholeContinuedEntry() {
		Document document = new Document("Other: keep\r\nBundle-Activator: old.\\\r\n Activator\r\nLast: keep");
		new BndHeaderQuickFix("Bundle-Activator", null, "Remove").apply(document);
		assertThat(document.get()).isEqualTo("Other: keep\r\nLast: keep");
	}

	@Test
	void removesAllDuplicateHeaders() {
		Document document = new Document("Bundle-Activator: old.Activator\nOther: keep\nBundle-Activator: new.Activator\n");
		new BndHeaderQuickFix("Bundle-Activator", null, "Remove").apply(document);
		assertThat(document.get()).isEqualTo("Other: keep\n");
	}

	@Test
	void missingHeaderDoesNotChangeDocument() {
		Document document = new Document("Other: keep\n");
		new BndHeaderQuickFix("Bundle-Version", "3.0.0", "Replace").apply(document);
		assertThat(document.get()).isEqualTo("Other: keep\n");
	}
}