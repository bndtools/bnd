package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.repository.p2.provider.P2Repository;
import bndtools.central.Central;
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
	void featureVisibilityAndResolution_workForP2AndOSGi() throws Exception {
		SWTBotView repositoriesView = openRepositoriesView();
		SWTBotToolbarToggleButton toggle = repositoriesView.toolbarToggleButton("Show or hide Eclipse features");
		boolean originallyChecked = toggle.isChecked();
		if (!originallyChecked)
			toggle.click();
		findFeatureNode(repositoriesView.bot().tree());
		P2Repository p2 = (P2Repository) Central.getWorkspace().getRepository(FIXTURE_REPO);
		TreeViewer viewer = (TreeViewer) repositoriesView.getViewReference().getView(false).getSite().getSelectionProvider();
		AtomicReference<Object> originalInput = new AtomicReference<>();
		Display.getDefault().syncExec(() -> originalInput.set(viewer.getInput()));
		OSGiRepository osgi = new OSGiRepository();
		try (osgi) {
			osgi.setRegistry(Central.getWorkspace());
			osgi.setProperties(java.util.Map.of("name", "OSGi Feature Fixture", "locations",
				new File(p2.getRoot(), "index.xml.gz").toURI().toString()));
			Central.getWorkspace().addBasicPlugin(osgi);
			List<?> repositories = Central.getWorkspace().getRepositories();
			Display.getDefault().syncExec(() -> viewer.setInput(repositories));
			SWTBotView resolution = openView("bndtools.impExpView");
			for (String repository : List.of(FIXTURE_REPO, "OSGi Feature Fixture")) {
				SWTBotTreeItem feature = findFeatureNode(repositoriesView.bot().tree(), repository);
				feature.expand();
				feature.getNode("1.0.0").expand();
				assertThat(feature.getNode("1.0.0").getItems()).isNotEmpty();
				feature.select();
				bot.waitUntil(new DefaultCondition() {
					@Override
					public boolean test() {
						AtomicReference<Boolean> found = new AtomicReference<>(false);
						Display.getDefault().syncExec(() -> {
							for (TableItem item : resolution.bot().table().widget.getItems()) {
								if (item.getData() instanceof Capability capability && "test.feature".equals(
									capability.getAttributes().get("osgi.identity")))
									found.set(true);
							}
						});
						return found.get();
					}

					@Override
					public String getFailureMessage() {
						return "Feature capabilities missing for " + repository;
					}
				});
			}
			toggle.click();
			bot.waitUntil(new DefaultCondition() {
				@Override
				public boolean test() {
					int fixtures = 0;
					for (SWTBotTreeItem repository : repositoriesView.bot().tree().getAllItems()) {
						if (!repository.getText().startsWith(FIXTURE_REPO)
							&& !repository.getText().startsWith("OSGi Feature Fixture"))
							continue;
						fixtures++;
						repository.expand();
						boolean bundleFound = false;
						for (SWTBotTreeItem child : repository.getItems()) {
							if (child.getText().startsWith("test.feature"))
								return false;
							bundleFound |= child.getText().startsWith("feature.member.");
						}
						if (!bundleFound)
							return false;
					}
					return fixtures == 2;
				}

				@Override
				public String getFailureMessage() {
					return "Features must be hidden in both repositories while bundles remain visible";
				}
			});
			toggle.click();
			findFeatureNode(repositoriesView.bot().tree(), FIXTURE_REPO);
			findFeatureNode(repositoriesView.bot().tree(), "OSGi Feature Fixture");
		} finally {
			Central.getWorkspace().removeBasicPlugin(osgi);
			Display.getDefault().syncExec(() -> viewer.setInput(originalInput.get()));
			if (toggle.isChecked() != originallyChecked)
				toggle.click();
		}
	}

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
		return openView(REPOSITORIES_VIEW_ID);
	}

	private SWTBotView openView(String viewId) {
		AtomicReference<Exception> failure = new AtomicReference<>();
		Display.getDefault()
			.syncExec(() -> {
				try {
					PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(viewId);
				} catch (Exception e) {
					failure.set(e);
				}
			});
		if (failure.get() != null) {
			throw Exceptions.duck(failure.get());
		}
		SWTBotView view = bot.viewById(viewId);
		view.show();
		return view;
	}
}
