package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.service.RepositoryPlugin;
import aQute.p2.provider.Feature;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.core.test.utils.WorkbenchTest;

/**
 * Measures the "Add Bundle" dialog of the Build Path section against a
 * repository with {@value #BUNDLE_COUNT} bundles. Opt-in only: run via
 * test.swtbot.perf.*.bndrun which sets {@value #ENABLE_PROPERTY}=true.
 */
@Tag("performance")
@EnabledIfSystemProperty(named = RepoBundleSelectionPerfSwtbotTest.ENABLE_PROPERTY, matches = "true")
@WorkbenchTest("ui/swtbot/perf")
class RepoBundleSelectionPerfSwtbotTest extends AbstractFeatureSwtbotTest {

	static final String			ENABLE_PROPERTY	= "bndtools.test.performance";
	static final int			BUNDLE_COUNT	= 5000;
	static final String			PERF_REPO		= "Perf Fixture";
	static final String			FILTER			= "bundle.0123";
	static final int			FILTER_MATCHES	= 10;
	private static int			bundleCount		= BUNDLE_COUNT;
	private static int			featureCount;
	private static int			filterMatches	= FILTER_MATCHES;
	private static int			filterFeatureMatches;
	private static String		filter			= FILTER;

	// Windows baseline before tuning: open 9.4 s, type filter 19.2 s, clear filter 8.6 s
	private static final long	OPEN_LIMIT_MS	= Long.getLong(ENABLE_PROPERTY + ".open.ms", 5000);
	private static final long	LOAD_LIMIT_MS	= Long.getLong(ENABLE_PROPERTY + ".load.ms", 20000);
	private static final long	FILTER_LIMIT_MS	= Long.getLong(ENABLE_PROPERTY + ".filter.ms", 5000);
	private static final long	RUN_FILTER_LIMIT_MS	= Long.getLong(ENABLE_PROPERTY + ".run.filter.ms", 8000);

	private SWTBotShell			wizardShell;

	@BeforeAll
	static void generateIndex() throws Exception {
		File cnf = Central.getWorkspace()
			.getBuildDir();
		String externalIndex = System.getProperty(ENABLE_PROPERTY + ".index", System.getenv("BNDTOOLS_PERF_INDEX"));
		File target = new File(cnf, "perf-index.xml");
		if (externalIndex == null || externalIndex.isBlank()) {
			IO.store(index(), target);
		} else {
			File source = new File(externalIndex);
			assertThat(source).as("external performance index").isFile();
			new XMLResourceGenerator().resources(XMLResourceParser.getResources(source))
				.save(target);
			filter = System.getProperty(ENABLE_PROPERTY + ".filter", "ha");
		}
		Central.refreshPlugins();
		RepositoryPlugin repository = Central.getWorkspace()
			.getPlugins(RepositoryPlugin.class)
			.stream()
			.filter(plugin -> PERF_REPO.equals(plugin.getName()))
			.findFirst()
			.orElseThrow();
		List<String> bundles = repository.list(null);
		List<Resource> resources = XMLResourceParser.getResources(target);
		List<String> featureIds = resources.stream()
			.filter(RepoBundleSelectionPerfSwtbotTest::isFeature)
			.map(RepoBundleSelectionPerfSwtbotTest::identity)
			.toList();
		List<String> visibleBundles = new ArrayList<>(bundles.size());
		for (String name : bundles) {
			if (!featureIds.contains(name) || !repository.versions(name).isEmpty()) {
				visibleBundles.add(name);
			}
		}
		bundleCount = visibleBundles.size();
		featureCount = (int) resources.stream()
			.filter(RepoBundleSelectionPerfSwtbotTest::isFeature)
			.count();
		filterMatches = (int) visibleBundles.stream()
			.filter(name -> name.toLowerCase(Locale.ROOT)
				.contains(filter.toLowerCase(Locale.ROOT)))
			.count();
		filterFeatureMatches = (int) resources.stream()
			.filter(RepoBundleSelectionPerfSwtbotTest::isFeature)
			.filter(resource -> resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
				.stream()
				.anyMatch(capability -> String.valueOf(capability.getAttributes()
					.get(IdentityNamespace.IDENTITY_NAMESPACE))
					.toLowerCase(Locale.ROOT)
					.contains(filter.toLowerCase(Locale.ROOT))))
			.count();
		assertThat(filterMatches).as("matching bundles in performance fixture")
			.isPositive()
			.isLessThan(bundleCount);
		System.out.printf("RepoBundleSelection dataset: index=%s, bundles=%d, features=%d, filter=%s, matches=%d%n",
			externalIndex == null || externalIndex.isBlank() ? "synthetic" : externalIndex,
			bundleCount, featureCount, filter, filterMatches + filterFeatureMatches);
	}

	@AfterEach
	void closeWizard() {
		if (wizardShell != null && wizardShell.isOpen()) {
			wizardShell.close();
		}
	}

	@Test
	void openAndFilter() throws Exception {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();

		long start = System.nanoTime();
		editor.bot()
			.toolbarButtonWithTooltip("Add Bundle")
			.click();
		long clickMs = elapsedMs(start);
		wizardShell = bot.activeShell();
		SWTBot wizardBot = wizardShell.bot();
		Tree tree = wizardBot.tree().widget;
		waitForPerfRepoChildren(tree, bundleCount + featureCount, children -> true, "all repository entries listed");
		long contentLoadMs = elapsedMs(start) - clickMs;

		SWTBotText search = wizardBot.textWithMessage("filter bundle name");
		start = System.nanoTime();
		// setText per keystroke: typeText needs an AWT keyboard, unavailable in the test launch
		for (int index = 1; index <= filter.length(); index++) {
			search.setText(filter.substring(0, index));
		}
		long typingMs = elapsedMs(start);
		waitForPerfRepoChildren(tree, filterMatches + filterFeatureMatches, children -> children.stream()
			.allMatch(text -> text.toLowerCase(Locale.ROOT)
				.contains(filter.toLowerCase(Locale.ROOT))), "filtered bundles listed");
		long filterMs = elapsedMs(start);

		start = System.nanoTime();
		search.setText("");
		waitForPerfRepoChildren(tree, bundleCount + featureCount, children -> true, "filter cleared");
		long clearMs = elapsedMs(start);

		wizardShell.close();
		wizardShell = null;
		start = System.nanoTime();
		editor.bot()
			.toolbarButtonWithTooltip("Add Bundle")
			.click();
		wizardShell = bot.activeShell();
		Tree reopenedTree = wizardShell.bot().tree().widget;
		TreeSnapshot reopened = perfRepoChildren(reopenedTree, bundleCount + featureCount);
		long reopenMs = elapsedMs(start);
		assertThat(reopened).as("reopened dialog reuses completed repository contents")
			.isNotNull();
		assertThat(reopened.count()).isEqualTo(bundleCount + featureCount);
		assertThat(reopened.labels()).noneMatch(label -> label.startsWith("Loading "));

		System.out.printf(
			"RepoBundleSelection perf: shell open=%d ms, content load=%d ms, type filter=%d ms (typing=%d), clear filter=%d ms, cached reopen=%d ms%n",
			clickMs, contentLoadMs, filterMs, typingMs, clearMs, reopenMs);

		assertThat(clickMs).as("open dialog shell")
			.isLessThanOrEqualTo(OPEN_LIMIT_MS);
		assertThat(contentLoadMs).as("load %s repository entries in background", bundleCount + featureCount)
			.isLessThanOrEqualTo(LOAD_LIMIT_MS);
		assertThat(filterMs).as("type filter '%s'", filter)
			.isLessThanOrEqualTo(FILTER_LIMIT_MS);
		assertThat(clearMs).as("clear filter")
			.isLessThanOrEqualTo(FILTER_LIMIT_MS);
	}

	@Test
	void addBundleRequirementOpensAndFiltersAsynchronously() throws Exception {
		long start = System.nanoTime();
		wizardShell = openBundleRequirementWizard();
		long shellOpenMs = elapsedMs(start);
		SWTBot wizardBot = wizardShell.bot();
		Tree tree = wizardBot.tree().widget;
		waitForPerfRepoChildren(tree, bundleCount + featureCount, children -> true,
			"Add Bundle Requirement repository entries loaded");
		long contentLoadMs = elapsedMs(start) - shellOpenMs;

		SWTBotText search = wizardBot.textWithMessage("filter bundle name");
		start = System.nanoTime();
		for (int index = 1; index <= filter.length(); index++) {
			search.setText(filter.substring(0, index));
		}
		long typingMs = elapsedMs(start);
		waitForPerfRepoChildren(tree, filterMatches + filterFeatureMatches, children -> children.stream()
			.allMatch(text -> text.toLowerCase(Locale.ROOT)
				.contains(filter.toLowerCase(Locale.ROOT))), "Add Bundle Requirement filtered entries");
		long filterMs = elapsedMs(start);

		System.out.printf(
			"AddBundleRequirement perf: shell open=%d ms, content load=%d ms, type filter=%d ms (typing=%d)%n",
			shellOpenMs, contentLoadMs, filterMs, typingMs);
		assertThat(shellOpenMs).as("open Add Bundle Requirement shell")
			.isLessThanOrEqualTo(OPEN_LIMIT_MS);
		assertThat(contentLoadMs).as("load Add Bundle Requirement repository entries")
			.isLessThanOrEqualTo(LOAD_LIMIT_MS);
		assertThat(filterMs).as("filter Add Bundle Requirement entries")
			.isLessThanOrEqualTo(FILTER_LIMIT_MS);
	}

	@Test
	void runPageRepositoryBrowserLoadsAndFiltersAsynchronously() throws Exception {
		SWTBotMultiPageEditor editor = openBndEditorBuildPage();
		long start = System.nanoTime();
		editor.activatePage("Run");
		long pageOpenMs = elapsedMs(start);
		SWTBot editorBot = editor.bot();
		SWTBotText search = editorBot.textWithMessage("type filter text");
		SWTBotTree runTree = editorBot.tree();
		Tree tree = runTree.widget;
		SWTBotTreeItem repositoryItem = Arrays.stream(runTree.getAllItems())
			.filter(item -> item.getText()
				.startsWith(PERF_REPO))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Perf repository root not present in Run page"));
		repositoryItem.expand();
		List<String> rows = waitForRunRepositoryRows(tree,
			visibleBundlesCount(), "Browse Repos entries loaded");
		long contentLoadMs = elapsedMs(start) - pageOpenMs;

		assertThat(rows.stream()
			.filter(text -> text.toLowerCase(Locale.ROOT)
				.contains(filter.toLowerCase(Locale.ROOT)))
			.count()).as("Browse Repos has matches for '%s'", filter)
			.isPositive();

		start = System.nanoTime();
		for (int index = 1; index <= filter.length(); index++) {
			search.setText(filter.substring(0, index));
		}
		assertThat(search.getText()).isEqualTo(filter);
		long typingMs = elapsedMs(start);
		List<String> filteredRows = waitForFilteredRunRepositoryRows(tree, rows.size());
		long filterMs = elapsedMs(start);

		System.out.printf(
			"RunRequirements Browse Repos perf: page open=%d ms, content load=%d ms, filter feedback=%d ms, filtered rows=%d ms, matches=%d%n",
			pageOpenMs, contentLoadMs, typingMs, filterMs, filteredRows.size());
		assertThat(pageOpenMs).as("open Run page")
			.isLessThanOrEqualTo(OPEN_LIMIT_MS);
		assertThat(contentLoadMs).as("load Browse Repos entries")
			.isLessThanOrEqualTo(LOAD_LIMIT_MS);
		assertThat(typingMs).as("show typed filter immediately")
			.isLessThanOrEqualTo(500);
		assertThat(filterMs).as("load filtered Browse Repos entries in background")
			.isLessThanOrEqualTo(RUN_FILTER_LIMIT_MS);
	}

	private List<String> waitForRunRepositoryRows(Tree tree, int expectedCount, String what) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>(List.of());
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				List<String> rows = runRepositoryRows(tree);
				snapshot.set(rows);
				return rows.size() == expectedCount
					&& rows.stream().noneMatch(row -> row.startsWith("Loading " + PERF_REPO));
			}

			@Override
			public String getFailureMessage() {
				return what + "; expected=" + expectedCount + ", found=" + snapshot.get().size();
			}
		}, 60_000, 250);
		return snapshot.get();
	}

	private List<String> waitForFilteredRunRepositoryRows(Tree tree, int unfilteredCount) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>(List.of());
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				List<String> rows = runRepositoryRows(tree);
				snapshot.set(rows);
				return !rows.isEmpty() && rows.size() < unfilteredCount
					&& rows.stream().noneMatch(row -> row.startsWith("Loading " + PERF_REPO));
			}

			@Override
			public String getFailureMessage() {
				return "Browse Repos filter did not narrow results; unfiltered=" + unfilteredCount + ", found="
					+ snapshot.get().size();
			}
		}, 60_000, 250);
		return snapshot.get();
	}

	private static List<String> runRepositoryRows(Tree tree) {
		return UIThreadRunnable.syncExec(() -> {
			List<String> rows = new ArrayList<>();
			for (TreeItem item : tree.getItems()) {
				if (item.getText()
					.startsWith(PERF_REPO)) {
					for (TreeItem child : item.getItems()) {
						rows.add(child.getText());
					}
					return rows;
				}
			}
			return rows;
		});
	}

	private static int visibleBundlesCount() {
		return bundleCount + featureCount;
	}

	private SWTBotShell openBundleRequirementWizard() throws Exception {
		Class<?> bndtoolsCore = Platform.getBundle("bndtools.core")
			.loadClass("bndtools.wizards.repo.RepoBundleSelectionWizard");
		Class<?> dependencyPhase = Platform.getBundle("bndtools.core")
			.loadClass("bndtools.model.repo.DependencyPhase");
		Object requirementPhase = Arrays.stream(dependencyPhase.getEnumConstants())
			.filter(value -> "Req".equals(value.toString()))
			.findFirst()
			.orElseThrow();
		IWizard wizard = (IWizard) bndtoolsCore.getConstructor(List.class, dependencyPhase)
			.newInstance(new ArrayList<>(), requirementPhase);
		bndtoolsCore.getMethod("setSelectionPageTitle", String.class)
			.invoke(wizard, "Add Bundle Requirement");
		bndtoolsCore.getMethod("setSelectionPageDescription", String.class)
			.invoke(wizard, "Select bundles to add as requirements.");

		Display.getDefault()
			.syncExec(() -> {
				Shell parent = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getShell();
				WizardDialog wizardDialog = new WizardDialog(parent, wizard);
				wizardDialog.create();
				wizardDialog.getShell()
					.open();
			});
		return bot.activeShell();
	}

	private static long elapsedMs(long startNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
	}

	private void waitForPerfRepoChildren(Tree tree, int count, Predicate<List<String>> settled, String what) {
		bot.waitUntil(new DefaultCondition() {
			TreeSnapshot last;

			@Override
			public boolean test() {
				last = perfRepoChildren(tree, count);
				return last != null && last.count() == count && settled.test(last.labels());
			}

			@Override
			public String getFailureMessage() {
				return what + " not reached; children of " + PERF_REPO + ": "
					+ last;
			}
		}, 60_000, 50);
	}

	private record TreeSnapshot(int count, List<String> labels) {}

	private static TreeSnapshot perfRepoChildren(Tree tree, int count) {
		return UIThreadRunnable.syncExec(() -> {
			for (TreeItem repo : tree.getItems()) {
				if (repo.getText()
					.startsWith(PERF_REPO)) {
					int n = repo.getItemCount();
					List<String> children = new ArrayList<>();
					if (n != count) {
						return new TreeSnapshot(n, children);
					}
					for (int index = 0; index < Math.min(n, 10); index++) {
						children.add(repo.getItem(index).getText());
					}
					return new TreeSnapshot(n, children);
				}
			}
			return null;
		});
	}

	private static String index() {
		StringBuilder sb = new StringBuilder(BUNDLE_COUNT * 600);
		sb.append("<?xml version='1.0' encoding='UTF-8'?>\n")
			.append("<repository name='perf' increment='0' xmlns='http://www.osgi.org/xmlns/repository/v1.0.0'>\n");
		for (int i = 0; i < BUNDLE_COUNT; i++) {
			String bsn = String.format("perf.bundle.%05d", i);
			sb.append("<resource>\n")
				.append("<capability namespace='osgi.identity'>")
				.append("<attribute name='osgi.identity' value='")
				.append(bsn)
				.append("'/>")
				.append("<attribute name='type' value='osgi.bundle'/>")
				.append("<attribute name='version' type='Version' value='1.0.0'/>")
				.append("</capability>\n")
				.append("<capability namespace='osgi.content'>")
				.append("<attribute name='osgi.content' value='")
				.append(String.format("%064x", i))
				.append("'/>")
				.append("<attribute name='url' value='")
				.append(bsn)
				.append("-1.0.0.jar'/>")
				.append("<attribute name='size' type='Long' value='1'/>")
				.append("<attribute name='mime' value='application/vnd.osgi.bundle'/>")
				.append("</capability>\n")
				.append("</resource>\n");
		}
		return sb.append("</repository>\n")
			.toString();
	}

	private static boolean isFeature(Resource resource) {
		return resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
			.stream()
			.anyMatch(capability -> Feature.TYPE.equals(capability.getAttributes()
				.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)));
	}

	private static String identity(Resource resource) {
		return resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
			.stream()
			.map(capability -> (String) capability.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE))
			.findFirst()
			.orElse("");
	}
}
