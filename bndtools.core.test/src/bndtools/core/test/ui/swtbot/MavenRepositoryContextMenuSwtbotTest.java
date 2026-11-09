package bndtools.core.test.ui.swtbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRootMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.exceptions.Exceptions;
import bndtools.core.test.utils.WorkbenchTest;

/**
 * Asserts that the context menus of the Maven repositories in the Repositories
 * view offer their actions. The artifacts come from Maven Central, so the
 * assertions only depend on what a repository has to offer for any artifact:
 * entries that need a newer release or a sources jar are checked leniently.
 */
@WorkbenchTest("ui/swtbot/maven")
class MavenRepositoryContextMenuSwtbotTest extends AbstractFeatureSwtbotTest {

	static final String	LENIENT_REPO	= "Maven Fixture";
	static final String	STRICT_REPO		= "Maven Strict Fixture";
	static final String	BSN				= "org.osgi.util.function";
	static final String	VERSION			= "1.1.0";
	static final String	CLIPBOARD		= "Copy to clipboard";

	private SWTBotTree	tree;

	@BeforeAll
	static void requireMavenCentral() {
		boolean reachable;
		try {
			HttpURLConnection connection = (HttpURLConnection) URI.create("https://repo.maven.apache.org/maven2/")
				.toURL()
				.openConnection();
			connection.setRequestMethod("HEAD");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			reachable = connection.getResponseCode() < 400;
		} catch (Exception e) {
			reachable = false;
		}
		Assumptions.assumeTrue(reachable, "Maven Central is not reachable");
	}

	@BeforeEach
	void openRepositoriesView() {
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
		view.show();
		tree = view.bot()
			.tree();
	}

	// repository level

	@Test
	void repositoryOffersOpenDefinition() {
		assertThat(rootLabels(repository(LENIENT_REPO))).contains("Open definition");
	}

	@Test
	void repositoryOffersCopyInfo() {
		assertThat(subLabels(repository(LENIENT_REPO), CLIPBOARD)).contains("Copy info");
	}

	@Test
	void repositoryOffersCopyBundlesWithSelfImports() {
		assertThat(subLabels(repository(LENIENT_REPO), CLIPBOARD))
			.contains("Bundles with substitution packages (self-imports)");
	}

	@Test
	void repositoryOffersUpdateRevisions() {
		assertThat(subLabels(repository(LENIENT_REPO), "Update Revisions"))
			.contains("To higher MICRO revision", "To higher MINOR revision", "To higher MAJOR revision");
	}

	@Test
	void repositoryOffersDryRunUpdateRevisions() {
		assertThat(subLabels(repository(LENIENT_REPO), "Update Revisions"))
			.contains("Dry run to clipboard - Update to higher MICRO revision",
				"Dry run to clipboard - Update to higher MINOR revision",
				"Dry run to clipboard - Update to higher MAJOR revision");
	}

	@Test
	void repositoryOffersConvert() {
		assertThat(subLabels(repository(LENIENT_REPO), "Convert")).contains("Text <--> pom.xml");
	}

	@Test
	void repositoryOffersCreateTrustedChecksumsFile() {
		assertThat(rootLabels(repository(LENIENT_REPO))).contains("Create Trusted Checksums file");
	}

	// bundle level

	@Test
	void bundleOffersCopyInfo() {
		assertThat(subLabels(bundle(LENIENT_REPO), CLIPBOARD)).contains("Copy info");
	}

	@Test
	void bundleOffersDeleteAllFromIndex() {
		assertThat(rootLabels(bundle(LENIENT_REPO))).contains("Delete All from Index");
	}

	// revision level

	@Test
	void revisionOffersRefreshCache() {
		assertRevisionEntry(LENIENT_REPO, "Refresh Cache (re-download)");
	}

	@Test
	void revisionOffersClearFromCache() {
		assertRevisionEntry(LENIENT_REPO, "Clear from Cache");
	}

	@Test
	void revisionOffersDeleteFromIndex() {
		assertRevisionEntry(LENIENT_REPO, "Delete from Index");
	}

	@Test
	void revisionOffersAddCompileDependencies() {
		assertRevisionEntry(LENIENT_REPO, "Add Compile Dependencies");
	}

	@Test
	void revisionOffersAddRuntimeDependencies() {
		assertRevisionEntry(LENIENT_REPO, "Add Runtime Dependencies");
	}

	@Test
	void revisionOffersUpdateToNewerRevision() {
		List<String> labels = rootLabels(revision(LENIENT_REPO));
		// depends on what Maven Central offers: an update or the reason it is unavailable
		assertThat(labels).anyMatch(label -> label.startsWith("Update to ") || label.startsWith("Update ["));
	}

	@Test
	void revisionOffersAddSources() {
		assertRevisionEntry(LENIENT_REPO, "Add Sources");
	}

	@Test
	void revisionOffersCopyBsnAndVersion() {
		assertThat(subLabels(revision(LENIENT_REPO), CLIPBOARD)).contains("Copy bsn+version");
		assertThat(copyToClipboard(LENIENT_REPO, "Copy bsn+version")).startsWith(BSN + ";version=" + VERSION);
	}

	@Test
	void revisionOffersCopyInfo() {
		assertThat(subLabels(revision(LENIENT_REPO), CLIPBOARD)).contains("Copy info");
		assertThat(copyToClipboard(LENIENT_REPO, "Copy info")).contains(BSN);
	}

	@Test
	void revisionOffersCopyGav() {
		assertThat(subLabels(revision(LENIENT_REPO), CLIPBOARD)).contains("Copy GAV to clipboard");
		assertThat(copyToClipboard(LENIENT_REPO, "Copy GAV to clipboard")).contains("org.osgi:" + BSN)
			.contains(VERSION);
	}

	@Test
	void revisionOffersCopyCompileDependencies() {
		assertThat(subLabels(revision(LENIENT_REPO), CLIPBOARD)).contains("Copy Compile Dependecies to clipboard");
	}

	@Test
	void clearFromCacheRemovesCachedArtifact() {
		File artifact = cachedArtifact("maven-fixture");
		bot.waitUntil(fileExists(artifact, true), 120_000, 500);

		select(revision(LENIENT_REPO), menu -> menu.menu("Clear from Cache"))
			.click();

		bot.waitUntil(fileExists(artifact, false), 60_000, 250);
	}

	@Test
	void refreshCacheDownloadsArtifactAgain() {
		File artifact = cachedArtifact("maven-fixture");

		select(revision(LENIENT_REPO), menu -> menu.menu("Refresh Cache (re-download)"))
			.click();

		bot.waitUntil(fileExists(artifact, true), 120_000, 500);
	}

	// strict checksum policy: probing for sources must not drop the menu

	@Test
	void strictRepositoryRevisionKeepsItsActions() {
		// the second time the sources jar of the first probe is cached and has no trusted checksum
		for (int attempt = 1; attempt <= 2; attempt++) {
			List<String> labels = rootLabels(revision(STRICT_REPO));
			assertThat(labels).as("menu, attempt %d", attempt)
				.contains("Refresh Cache (re-download)", "Clear from Cache", "Delete from Index",
					"Add Compile Dependencies", "Add Runtime Dependencies", "Add Sources", CLIPBOARD);
		}
	}

	@Test
	void strictRepositoryRevisionKeepsItsClipboardEntries() {
		assertThat(subLabels(revision(STRICT_REPO), CLIPBOARD)).contains("Copy bsn+version", "Copy info",
			"Copy GAV to clipboard");
	}

	private void assertRevisionEntry(String repository, String label) {
		SWTBotMenu entry = select(revision(repository), menu -> menu.menu(label));
		assertThat(entry.isEnabled()).as("'%s' is enabled", label)
			.isTrue();
	}

	// clipboard

	private String copyToClipboard(String repository, String label) {
		String sentinel = "sentinel-" + System.nanoTime();
		writeClipboard(sentinel);

		select(revision(repository), menu -> menu.menu(CLIPBOARD)
			.menu(label)).click();

		AtomicReference<String> content = new AtomicReference<>();
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				content.set(readClipboard());
				return content.get() != null && !content.get()
					.equals(sentinel);
			}

			@Override
			public String getFailureMessage() {
				return "'" + label + "' did not change the clipboard";
			}
		}, 60_000, 250);
		return content.get();
	}

	private static void writeClipboard(String text) {
		Display.getDefault()
			.syncExec(() -> {
				Clipboard clipboard = new Clipboard(Display.getDefault());
				try {
					clipboard.setContents(new Object[] {
						text
					}, new org.eclipse.swt.dnd.Transfer[] {
						TextTransfer.getInstance()
					});
				} finally {
					clipboard.dispose();
				}
			});
	}

	private static String readClipboard() {
		AtomicReference<String> text = new AtomicReference<>();
		Display.getDefault()
			.syncExec(() -> {
				Clipboard clipboard = new Clipboard(Display.getDefault());
				try {
					text.set((String) clipboard.getContents(TextTransfer.getInstance()));
				} finally {
					clipboard.dispose();
				}
			});
		return text.get();
	}

	private static DefaultCondition fileExists(File file, boolean exists) {
		return new DefaultCondition() {
			@Override
			public boolean test() {
				return file.isFile() == exists;
			}

			@Override
			public String getFailureMessage() {
				return file + (exists ? " was not downloaded" : " was not removed");
			}
		};
	}

	private static File cachedArtifact(String cacheDirectory) {
		File cnf = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("cnf")
			.getLocation()
			.toFile();
		return new File(cnf, "cache/" + cacheDirectory + "/org/osgi/" + BSN + "/" + VERSION + "/" + BSN + "-" + VERSION
			+ ".jar");
	}

	// menus

	private List<String> rootLabels(Supplier<SWTBotTreeItem> node) {
		return select(node, menu -> menu.menuItems()
			.stream()
			.map(MavenRepositoryContextMenuSwtbotTest::label)
			.filter(text -> !text.isBlank())
			.toList());
	}

	private List<String> subLabels(Supplier<SWTBotTreeItem> node, String submenu) {
		return select(node, menu -> menu.menu(submenu)
			.menuItems()
			.stream()
			.map(MavenRepositoryContextMenuSwtbotTest::label)
			.filter(text -> !text.isBlank())
			.toList());
	}

	private static String label(String text) {
		int tab = text.indexOf('\t');
		return (tab < 0 ? text : text.substring(0, tab)).replace("&&", "&");
	}

	/**
	 * Opens the context menu of a node and applies the function. The tree is
	 * refreshed when a repository finishes loading, which disposes the nodes
	 * found earlier, so the node is resolved again when that happens.
	 */
	private <T> T select(Supplier<SWTBotTreeItem> node, Function<SWTBotRootMenu, T> use) {
		RuntimeException failure = null;
		for (int attempt = 0; attempt < 5; attempt++) {
			try {
				return use.apply(node.get()
					.select()
					.contextMenu());
			} catch (WidgetNotFoundException | SWTException e) {
				failure = e;
				bot.sleep(500);
			}
		}
		throw failure;
	}

	// tree nodes

	private SWTBotTreeItem waitForRepository(String repository) {
		AtomicReference<SWTBotTreeItem> result = new AtomicReference<>();
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				result.set(find(tree.getAllItems(), repository));
				return result.get() != null;
			}

			@Override
			public String getFailureMessage() {
				return "Repository " + repository + " is not shown in the Repositories view";
			}
		}, 60_000, 250);
		return result.get();
	}

	private Supplier<SWTBotTreeItem> repository(String repository) {
		return () -> waitForRepository(repository);
	}

	private Supplier<SWTBotTreeItem> bundle(String repository) {
		return () -> child(repository(repository), BSN);
	}

	private Supplier<SWTBotTreeItem> revision(String repository) {
		return () -> child(bundle(repository), VERSION);
	}

	// the parent is resolved on every poll because refreshing the tree disposes its items
	private SWTBotTreeItem child(Supplier<SWTBotTreeItem> parentNode, String prefix) {
		AtomicReference<SWTBotTreeItem> result = new AtomicReference<>();
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				try {
					SWTBotTreeItem parent = parentNode.get();
					parent.expand();
					SWTBotTreeItem found = find(parent.getItems(), prefix);
					if (found == null && parent.isExpanded()) {
						// JFace may not have populated a node the widget reports as expanded
						parent.collapse();
						parent.expand();
						found = find(parent.getItems(), prefix);
					}
					result.set(found);
					return found != null;
				} catch (WidgetNotFoundException | SWTException e) {
					return false;
				}
			}

			@Override
			public String getFailureMessage() {
				return "No node starting with " + prefix + " in the Repositories view";
			}
		}, 120_000, 500);
		return result.get();
	}

	private static SWTBotTreeItem find(SWTBotTreeItem[] items, String prefix) {
		for (SWTBotTreeItem item : items) {
			String text = item.getText();
			if (text != null && text.startsWith(prefix)) {
				return item;
			}
		}
		return null;
	}
}
