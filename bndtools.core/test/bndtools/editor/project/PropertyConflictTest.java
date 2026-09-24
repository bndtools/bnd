package bndtools.editor.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.PropertyConflict;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.service.reporter.Report.Location;

class PropertyConflictTest {
	@Test
	void duplicateRepairUsesParsedKeysAndPreservesLayout() throws Exception {
		String content = "# -runvm: comment\r\n-runvm: first\\\r\n continued\r\n"
			+ "  -runv\\u006d = second\r\n-runvm: third\r\n";
		String repaired = IncludeConflictDetector.renameKeys(content, "-runvm", "local",
			Set.of("-runvm.local", "-runvm.local-2"), true);
		assertThat(repaired).isEqualTo("# -runvm: comment\r\n-runvm: first\\\r\n continued\r\n"
			+ "  -runvm.local-3 = second\r\n-runvm.local-4: third\r\n");
	}

	@Test
	void includeRepairRenamesOnlyFirstDeclaration() throws Exception {
		assertThat(IncludeConflictDetector.renameKeys(" -runvm: first\n-runvm: second\n", "-runvm", "local",
			Set.of(), false)).isEqualTo(" -runvm.local: first\n-runvm: second\n");
	}

	@Test
	void scalarPropertiesCannotBeRenamedToMergedKeys() {
		assertThatThrownBy(() -> IncludeConflictDetector.renameKeys("name: first\nname: second\n", "name", "local",
			Set.of(), true)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void markerAttributesUseCoreDetails() throws Exception {
		File file = new File("test.bnd").getAbsoluteFile();
		try (Processor processor = PropertyConflict.analyze(null, file,
			"-runvm: first\n-runvm: last\n-propertyconflicts: warning\n")) {
			String message = processor.getWarnings().get(0);
			Location location = processor.getLocation(message);
			Map<String, Object> attributes = IncludeConflictDetector.attributes((PropertyConflict) location.details,
				location, processor);
			assertThat(attributes).containsEntry(IMarker.LINE_NUMBER, 2)
				.containsEntry(IMarker.MESSAGE, message)
				.containsEntry(IncludeConflictDetector.ATTR_MERGEABLE, true)
				.containsEntry(IncludeConflictDetector.ATTR_IN_FILE, true)
				.containsEntry(IncludeConflictDetector.ATTR_SOURCES, file.getAbsolutePath())
				.containsEntry(IncludeConflictDetector.ATTR_ROOT, file.getAbsolutePath());
			assertThat(attributes).doesNotContainKey(IMarker.SEVERITY);
		}
	}

	@Test
	void scalarMarkersHaveNoMergeResolution() {
		IMarker marker = mock(IMarker.class);
		when(marker.getAttribute(IncludeConflictDetector.ATTR_KEY, null)).thenReturn("Bundle-SymbolicName");
		IncludeConflictMarkerResolutionGenerator generator = new IncludeConflictMarkerResolutionGenerator();
		assertThat(generator.hasResolutions(marker)).isFalse();
		assertThat(generator.getResolutions(marker)).isEmpty();
	}

	@Test
	void suffixedMergedHeaderCanBeRepaired() throws Exception {
		String content = "-runblacklist.win32: mac\n";
		String renamed = IncludeConflictDetector.renameKeys(content, "-runblacklist.win32", "bndrun",
			Set.of("-runblacklist.win32.bndrun"), false);
		assertThat(renamed).isEqualTo("-runblacklist.win32.bndrun-2: mac\n");
	}

	@Test
	void suffixedMergedHeaderMarkerHasMergeResolution(@InjectTemporaryDirectory
	File tmp) throws Exception {
		File file = new File(tmp, "app.ui_win32.win32.x86-64.bndrun");
		aQute.lib.io.IO.store("-runblacklist: mac\n", file);
		IMarker marker = mock(IMarker.class);
		when(marker.getAttribute(IncludeConflictDetector.ATTR_KEY, null)).thenReturn("-runblacklist");
		when(marker.getAttribute(IncludeConflictDetector.ATTR_MERGEABLE, false)).thenReturn(true);
		when(marker.getAttribute(IncludeConflictDetector.ATTR_SOURCES, "")).thenReturn(file.getAbsolutePath());
		when(marker.getAttribute(IncludeConflictDetector.ATTR_IN_FILE, false)).thenReturn(false);
		when(marker.getAttribute(IncludeConflictDetector.ATTR_ROOT, file.getAbsolutePath())).thenReturn(file.getAbsolutePath());
		IncludeConflictMarkerResolutionGenerator generator = new IncludeConflictMarkerResolutionGenerator();
		assertThat(generator.hasResolutions(marker)).isTrue();
		org.eclipse.ui.IMarkerResolution[] resolutions = generator.getResolutions(marker);
		assertThat(resolutions).hasSize(1);
		assertThat(resolutions[0].getLabel()).isEqualTo("Rename '-runblacklist' to the unique merged property key '-runblacklist.app.ui_win32.win32.x86-64' inside app.ui_win32.win32.x86-64.bndrun");

		PropertyConflictDetailsHandler handler = new PropertyConflictDetailsHandler();
		List<org.eclipse.jface.text.contentassist.ICompletionProposal> proposals = handler.getProposals(marker);
		assertThat(proposals).hasSize(1);
		assertThat(proposals.get(0).getDisplayString()).isEqualTo("Rename '-runblacklist' to the unique merged property key '-runblacklist.app.ui_win32.win32.x86-64' inside app.ui_win32.win32.x86-64.bndrun");
	}
}
