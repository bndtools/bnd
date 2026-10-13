package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;

import aQute.bnd.exceptions.Exceptions;
import bndtools.core.test.utils.WorkbenchTest;

/**
 * Drags an Eclipse feature from the Repositories view onto the Build Path
 * table of the BndEditor and asserts that the canonical feature clause is
 * written to bnd.bnd. This exercises the same drop handler that is shared
 * with the Run Bundles section.
 */
@WorkbenchTest("ui/swtbot/features")
class FeatureBuildPathDndSwtbotTest extends AbstractFeatureSwtbotTest {

	private static final String REPOSITORIES_VIEW_ID = "bndtools.repositoriesView";

	@Test
	void dragFeatureToBuildPath_writesCanonicalClause() throws Exception {
		SWTBotView repositoriesView = openRepositoriesView();
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();

		// Locate the feature in the Repositories view
		SWTBotTreeItem featureNode = findFeatureNode(repositoriesView.bot()
			.tree());
		featureNode.select();

		// The Build Path table is the only table on the Build page
		SWTBotTable buildPathTable = editor.bot()
			.table(0);

		featureNode.dragAndDrop(buildPathTable);

		editor.save();

		String bndBnd = readTestProjectBndBnd();
		assertThat(bndBnd).as("bnd.bnd contains the canonical feature clause")
			.contains("test.feature")
			.containsPattern("type='?org\\.eclipse\\.update\\.feature'?")
			.doesNotContain("feature:")
			.doesNotContain("feature=true");
	}

	private SWTBotView openRepositoriesView() {
		AtomicReference<Exception> failure = new AtomicReference<>();
		Display.getDefault()
			.syncExec(() -> {
				try {
					PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(REPOSITORIES_VIEW_ID);
				} catch (Exception e) {
					failure.set(e);
				}
			});
		if (failure.get() != null) {
			throw Exceptions.duck(failure.get());
		}
		SWTBotView view = bot.viewById(REPOSITORIES_VIEW_ID);
		view.show();
		return view;
	}
}
