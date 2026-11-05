package bndtools.core.test.ui.swtbot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.exceptions.Exceptions;
import bndtools.core.test.utils.WorkbenchTest;

/**
 * Asserts that typing a search term in every repository browser expands the
 * repository root and shows exactly the case-insensitively matching bundles,
 * without the test expanding any node itself.
 */
@WorkbenchTest("ui/swtbot/filter")
class RepositoryFilterSwtbotTest extends AbstractFeatureSwtbotTest {

	static final String			REPO		= "Filter Fixture";
	static final List<String>	ALL			= List.of("com.acme.RCPTools", "org.example.logging",
		"org.example.rcp.core", "org.example.rcp.ui", "org.example.util");
	static final List<String>	RCP_MATCHES	= List.of("com.acme.RCPTools", "org.example.rcp.core",
		"org.example.rcp.ui");

	private SWTBotShell			wizardShell;

	@AfterEach
	void closeWizard() {
		if (wizardShell != null && wizardShell.isOpen()) {
			wizardShell.close();
		}
		wizardShell = null;
	}

	@Test
	void projectBuildPathWizardFilters() {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();
		editor.bot()
			.toolbarButtonWithTooltip("Add Bundle")
			.click();
		assertWizardFilters("Project Build Path");
	}

	@Test
	void addBundleRequirementWizardFilters() {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();
		editor.activatePage("Run");
		editor.bot()
			.toolbarButtonWithTooltip("Add Bundle Requirement")
			.click();
		assertWizardFilters("Add Bundle Requirement");
	}

	@Test
	void runPageBrowseReposFilters() {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();
		editor.activatePage("Run");
		SWTBot editorBot = editor.bot();
		Tree tree = editorBot.tree().widget;
		SWTBotText search = editorBot.textWithMessage("type filter text");

		type(search, "rcp");
		waitForRows(tree, RCP_MATCHES, "Browse Repos filtered by 'rcp'");

		type(search, "RCP");
		waitForRows(tree, RCP_MATCHES, "Browse Repos filtered by 'RCP'");
	}

	@Test
	void repositoriesViewFilters() {
		Display.getDefault()
			.syncExec(() -> {
				try {
					PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView("bndtools.repositoriesView");
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		SWTBotView view = bot.viewById("bndtools.repositoriesView");
		try {
			SWTBot viewBot = view.bot();
			Tree tree = viewBot.tree().widget;
			SWTBotText search = viewBot.textWithMessage("Enter search string");

			type(search, "rcp");
			waitForRows(tree, RCP_MATCHES, "Repositories view filtered by 'rcp'");

			type(search, "RCP");
			waitForRows(tree, RCP_MATCHES, "Repositories view filtered by 'RCP'");

			// A second applied filter must not replace the pre-filter expansion state
			type(search, "util");
			waitForRows(tree, List.of("org.example.util"), "Repositories view filtered by 'util'");

			search.setText("");
			waitForCollapsedRoot(tree, "Repositories view filter cleared");
		} finally {
			view.close();
		}
	}

	private void assertWizardFilters(String title) {
		wizardShell = bot.activeShell();
		SWTBot wizardBot = wizardShell.bot();
		wizardBot.label(title);
		Tree tree = wizardBot.tree().widget;
		SWTBotText search = wizardBot.textWithMessage("filter bundle name");

		waitForRows(tree, ALL, title + " initial contents");

		type(search, "rcp");
		waitForRows(tree, RCP_MATCHES, title + " filtered by 'rcp'");

		type(search, "RCP");
		waitForRows(tree, RCP_MATCHES, title + " filtered by 'RCP'");

		type(search, "util");
		waitForRows(tree, List.of("org.example.util"), title + " filtered by 'util'");

		search.setText("");
		waitForRows(tree, ALL, title + " filter cleared");
	}

	private static void type(SWTBotText search, String text) {
		// setText per keystroke: typeText needs an AWT keyboard, unavailable in the test launch
		for (int index = 1; index <= text.length(); index++) {
			search.setText(text.substring(0, index));
		}
	}

	/**
	 * Waits until the fixture repository root is expanded by the view itself
	 * and lists exactly the expected bundles, with no loading or failure row.
	 */
	private void waitForRows(Tree tree, List<String> expected, String what) {
		AtomicReference<String> state = new AtomicReference<>("not read");
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				return UIThreadRunnable.syncExec(() -> {
					TreeItem root = fixtureRoot(tree);
					if (root == null) {
						state.set(REPO + " root not shown");
						return false;
					}
					List<String> identities = new ArrayList<>();
					List<String> labels = new ArrayList<>();
					for (TreeItem child : root.getItems()) {
						labels.add(child.getText());
						String identity = repositoryIdentity(child.getData());
						if (identity != null) {
							identities.add(identity);
						}
					}
					state.set("expanded=" + root.getExpanded() + ", rows=" + labels);
					return root.getExpanded() && labels.stream()
						.noneMatch(label -> label.startsWith("Loading ") || label.startsWith("Failed to load "))
						&& identities.size() == expected.size() && identities.containsAll(expected);
				});
			}

			@Override
			public String getFailureMessage() {
				return what + ": expected expanded " + REPO + " with " + expected + " but " + state.get();
			}
		}, 30_000, 100);
	}

	/**
	 * Clearing must restore the pre-filter (collapsed) state rather than
	 * expanding every repository with its full contents.
	 */
	private void waitForCollapsedRoot(Tree tree, String what) {
		AtomicReference<String> state = new AtomicReference<>("not read");
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				return UIThreadRunnable.syncExec(() -> {
					TreeItem root = fixtureRoot(tree);
					state.set(root == null ? REPO + " root not shown" : "expanded=" + root.getExpanded());
					return root != null && !root.getExpanded();
				});
			}

			@Override
			public String getFailureMessage() {
				return what + ": expected collapsed " + REPO + " but " + state.get();
			}
		}, 5_000, 100);
	}

	private static TreeItem fixtureRoot(Tree tree) {
		for (TreeItem item : tree.getItems()) {
			if (item.getText()
				.startsWith(REPO)) {
				return item;
			}
		}
		return null;
	}
}
