package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import bndtools.core.test.utils.WorkbenchTest;

/**
 * Adds an Eclipse feature to -buildpath through the "Add Bundle" wizard of
 * the Build Path section and asserts that the canonical feature clause
 * {@code id;version='V';type=org.eclipse.update.feature} is written to
 * bnd.bnd.
 */
@WorkbenchTest("ui/swtbot/features")
class FeatureBuildPathWizardSwtbotTest extends AbstractFeatureSwtbotTest {

	@Test
	void addFeatureViaWizard_writesCanonicalClause() throws Exception {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();

		// Open the bundle selection wizard of the Build Path section
		editor.bot()
			.toolbarButtonWithTooltip("Add Bundle")
			.click();

		// The wizard dialog is the active shell after the click
		SWTBotShell shell = bot.activeShell();
		SWTBot wizardBot = shell.bot();

		// Select the feature below the fixture repository
		SWTBotTreeItem featureNode = findFeatureNode(wizardBot.tree());
		featureNode.select();

		wizardBot.button("Add -->")
			.click();
		wizardBot.button("Finish")
			.click();

		editor.save();

		String bndBnd = readTestProjectBndBnd();
		assertThat(bndBnd).as("bnd.bnd contains the canonical feature clause")
			.contains("test.feature")
			.containsPattern("type='?org\\.eclipse\\.update\\.feature'?")
			.containsPattern("version='?1\\.0\\.0'?");
	}
}
