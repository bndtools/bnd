package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * Base class for SWTBot driven UI tests of the Eclipse feature support on
 * -buildpath. Requires a <em>rendered</em> workbench, i.e. the bndrun must
 * use the {@code bndtools.core.test.launch.rendered} bundle instead of the
 * headless {@code bndtools.core.test.launch} bundle.
 */
public abstract class AbstractFeatureSwtbotTest {

	protected static final String	FEATURE_CLAUSE	= "test.feature;version='1.0.0';type=org.eclipse.update.feature";
	protected static final String	FIXTURE_REPO	= "Feature Fixture";
	protected static final String	TEST_PROJECT	= "test.buildpath";

	protected static SWTWorkbenchBot	bot;

	@BeforeAll
	static void setUpBot() {
		SWTBotPreferences.TIMEOUT = 30000;
		SWTBotPreferences.PLAYBACK_DELAY = 20;
		bot = new SWTWorkbenchBot();
		try {
			bot.viewByTitle("Welcome")
				.close();
		} catch (WidgetNotFoundException e) {
			// Welcome view not present, continue
		}
	}

	@AfterEach
	void closeEditors() {
		bot.saveAllEditors();
		bot.closeAllEditors();
	}

	/**
	 * Open the bnd.bnd file of the test project in the BndEditor and activate
	 * its "Build" page. The editor is opened programmatically to keep the
	 * test focused on the Build Path UI itself.
	 */
	protected SWTBotMultiPageEditor openBndEditorBuildPage() {
		IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(TEST_PROJECT);
		IFile bndFile = project.getFile("bnd.bnd");
		assertThat(bndFile.exists()).as("bnd.bnd of %s exists", TEST_PROJECT)
			.isTrue();

		AtomicReference<Exception> failure = new AtomicReference<>();
		Display.getDefault()
			.syncExec(() -> {
				try {
					IEditorPart part = IDE.openEditor(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage(), bndFile, "bndtools.bndEditor", true);
					assertThat(part).isNotNull();
				} catch (Exception e) {
					failure.set(e);
				}
			});
		if (failure.get() != null) {
			throw Exceptions.duck(failure.get());
		}

		SWTBotEditor editor = bot.editorByTitle(TEST_PROJECT);
		SWTBotMultiPageEditor multiPage = new SWTBotMultiPageEditor(editor.getReference(), bot);
		multiPage.activatePage("Build");
		return multiPage;
	}

	/**
	 * Locate the feature node below the fixture repository node. Repository
	 * children are loaded lazily and the tree may be refreshed asynchronously,
	 * so all nodes are re-resolved on every poll.
	 */
	protected SWTBotTreeItem findFeatureNode(SWTBotTree tree) {
		AtomicReference<SWTBotTreeItem> result = new AtomicReference<>();
		bot.waitUntilWidgetAppears(new DefaultCondition() {
			@Override
			public boolean test() {
				for (SWTBotTreeItem repoNode : tree.getAllItems()) {
					String repoText = repoNode.getText();
					if (repoText == null || !repoText.startsWith(FIXTURE_REPO)) {
						continue;
					}
					SWTBotTreeItem feature = featureChild(repoNode);
					if (feature != null) {
						result.set(feature);
						return true;
					}
					// The node may report expanded although JFace never
					// populated it (native dummy child). Cycle
					// collapse/expand to force the content provider to run.
					if (repoNode.isExpanded()) {
						repoNode.collapse();
					}
					repoNode.expand();
					feature = featureChild(repoNode);
					if (feature != null) {
						result.set(feature);
						return true;
					}
				}
				return false;
			}

			private SWTBotTreeItem featureChild(SWTBotTreeItem repoNode) {
				for (SWTBotTreeItem child : repoNode.getItems()) {
					String text = child.getText();
					if (text != null && text.startsWith("test.feature")) {
						return child;
					}
				}
				return null;
			}

			@Override
			public String getFailureMessage() {
				StringBuilder sb = new StringBuilder(
					"Feature node test.feature did not appear below repository " + FIXTURE_REPO + ". Tree state:");
				for (SWTBotTreeItem repoNode : tree.getAllItems()) {
					sb.append("\n- ")
						.append(repoNode.getText());
					for (SWTBotTreeItem child : repoNode.getItems()) {
						sb.append("\n    - ")
							.append(child.getText());
					}
				}
				return sb.toString();
			}
		});
		return result.get();
	}

	protected String readTestProjectBndBnd() {
		try {
			IFile bndFile = ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProject(TEST_PROJECT)
				.getFile("bnd.bnd");
			bndFile.refreshLocal(0, null);
			return IO.collect(bndFile.getLocation()
				.toFile());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
