package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import bndtools.core.test.utils.WorkbenchTest;

@Tag("performance")
@EnabledIfSystemProperty(named = "bndtools.test.performance", matches = "true")
@WorkbenchTest("ui/swtbot/features")
class WorkspaceTemplateFilterPerfSwtbotTest extends AbstractFeatureSwtbotTest {

	private static final long FILTER_LIMIT_MS = Long.getLong("bndtools.test.performance.template.filter.ms", 5000);

	private SWTBotShell wizardShell;

	@Test
	void filtersWorkspaceTemplates() throws Exception {
		Display display = Display.getDefault();
		display.syncExec(() -> {
			try {
				Shell workbenchShell = org.eclipse.ui.PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getShell();
				IWizard wizard = newWorkspaceWizard();
				WizardDialog dialog = new WizardDialog(workbenchShell, wizard);
				dialog.create();
				dialog.getShell()
					.open();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		wizardShell = bot.activeShell();
		SWTBot wizardBot = wizardShell.bot();
		SWTBotTable table = wizardBot.table();
		List<List<String>> allRows = waitForRows(table.widget, rows -> !rows.isEmpty(), "templates loaded");
		TemplateQuery query = chooseSelectiveQuery(allRows);

		SWTBotText filter = wizardBot.textWithMessage("glob pattern, e.g. *eclipse*");
		long start = System.nanoTime();
		filter.setText(query.text());
		List<List<String>> filteredRows = waitForRows(table.widget,
			rows -> rows.size() == query.matches() && rows.stream().allMatch(row -> matches(row, query.text())),
			"filtered templates settled");
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		System.out.printf("WorkspaceTemplateFilter perf: templates=%d, query=%s, matches=%d, filter=%d ms%n",
			allRows.size(), query.text(), filteredRows.size(), elapsedMs);
		assertThat(elapsedMs).as("filter %s workspace templates", allRows.size())
			.isLessThanOrEqualTo(FILTER_LIMIT_MS);
	}

	@AfterEach
	void closeWizard() {
		if (wizardShell != null && wizardShell.isOpen()) {
			wizardShell.close();
		}
	}

	private static IWizard newWorkspaceWizard() throws Exception {
		return Platform.getBundle("bndtools.core")
			.loadClass("bndtools.wizards.newworkspace.NewWorkspaceWizard")
			.asSubclass(IWizard.class)
			.getConstructor()
			.newInstance();
	}

	private List<List<String>> waitForRows(Table table, Predicate<List<List<String>>> condition, String what) {
		AtomicReference<List<List<String>>> snapshot = new AtomicReference<>(List.of());
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				List<List<String>> rows = tableRows(table);
				snapshot.set(rows);
				return condition.test(rows);
			}

			@Override
			public String getFailureMessage() {
				return what + "; visible rows=" + snapshot.get().size();
			}
		}, 60_000, 50);
		return snapshot.get();
	}

	private static List<List<String>> tableRows(Table table) {
		return UIThreadRunnable.syncExec(() -> {
			List<List<String>> rows = new ArrayList<>(table.getItemCount());
			for (TableItem item : table.getItems()) {
				List<String> columns = new ArrayList<>(table.getColumnCount());
				for (int column = 0; column < table.getColumnCount(); column++) {
					columns.add(item.getText(column));
				}
				rows.add(columns);
			}
			return rows;
		});
	}

	private static TemplateQuery chooseSelectiveQuery(List<List<String>> rows) {
		for (List<String> row : rows) {
			for (int column = 1; column < Math.min(row.size(), 4); column++) {
				String value = row.get(column);
				for (int length = 3; length <= value.length(); length++) {
					String candidate = value.substring(0, length);
					long matches = rows.stream().filter(item -> matches(item, candidate)).count();
					if (matches > 0 && matches < rows.size()) {
						return new TemplateQuery(candidate, (int) matches);
					}
				}
			}
		}
		throw new AssertionError("Template dataset has no selective substring; count=" + rows.size());
	}

	private static boolean matches(List<String> row, String query) {
		String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
		for (int column = 1; column < Math.min(row.size(), 4); column++) {
			if (row.get(column).toLowerCase(java.util.Locale.ROOT).contains(normalizedQuery)) {
				return true;
			}
		}
		return false;
	}

	private record TemplateQuery(String text, int matches) {}
}
