package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.libg.filelock.ProcessLock;

/**
 * Index files are shared state: between repositories when misconfigured and
 * between processes that open the same workspace.
 */
public class P2RepositorySharingTest {
	@InjectTemporaryDirectory
	File tmp;

	private Workspace workspace() throws Exception {
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setProperty(Constants.CONNECTION_SETTINGS, "false");
		w.setBase(tmp);
		return w;
	}

	private static P2Repository repository(Workspace w, String name, File repository, File location, File cache)
		throws Exception {
		P2Repository p2r = new P2Repository();
		p2r.setRegistry(w);
		p2r.setReporter(w);
		Map<String, String> config = new HashMap<>();
		config.put("url", repository.toURI()
			.toString());
		config.put("name", name);
		config.put("location", location.getAbsolutePath());
		config.put("cache", cache.getAbsolutePath());
		p2r.setProperties(config);
		w.addBasicPlugin(p2r);
		return p2r;
	}

	private File copyOfRepository() throws Exception {
		File copy = new File(tmp, "macbadge-copy");
		IO.copy(IO.getFile("testdata/p2/macbadge"), copy);
		return copy;
	}

	@Test
	public void reportsRepositoriesWithDifferentUrlsThatShareAnIndexFile() throws Exception {
		File index = new File(tmp, "cnf/ext/shared_index.xml.gz");
		try (Workspace w = workspace();
			P2Repository first = repository(w, "first", IO.getFile("testdata/p2/macbadge"), index,
				new File(tmp, "cache/first"));
			P2Repository second = repository(w, "second", copyOfRepository(), index, new File(tmp, "cache/second"))) {

			assertThat(first.list(null)).isNotEmpty();
			assertThat(w.getErrors()).singleElement()
				.asString()
				.contains("first", "second", index.getPath(), "distinct location");

			assertThat(second.list(null)).isNotEmpty();
			assertThat(w.getErrors()).hasSize(2);
		}
	}

	@Test
	public void acceptsDistinctIndexFilesAndTheSameRepositoryTwice() throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File index = new File(tmp, "cnf/ext/first_index.xml.gz");
		try (Workspace w = workspace();
			P2Repository first = repository(w, "first", repository, index, new File(tmp, "cache/first"));
			P2Repository sameUrl = repository(w, "alias", repository, index, new File(tmp, "cache/first"));
			P2Repository other = repository(w, "other", copyOfRepository(),
				new File(tmp, "cnf/ext/other_index.xml.gz"), new File(tmp, "cache/other"))) {

			List<String> bundles = first.list(null);
			assertThat(bundles).isNotEmpty();
			assertThat(sameUrl.list(null)).isEqualTo(bundles);
			assertThat(other.list(null)).isEqualTo(bundles);
			assertThat(w.getErrors()).isEmpty();
		}
	}

	@Test
	public void waitsForTheIndexLockOfAnotherReader() throws Exception {
		File index = new File(tmp, "cnf/ext/locked_index.xml.gz");
		File cache = new File(tmp, "cache/locked");
		File lockFile = new File(cache, index.getName() + ".lock");
		try (Workspace w = workspace();
			P2Repository p2r = repository(w, "locked", IO.getFile("testdata/p2/macbadge"), index, cache)) {

			CompletableFuture<List<String>> listing;
			try (ProcessLock held = ProcessLock.tryLock(lockFile)
				.orElseThrow()) {
				listing = CompletableFuture.supplyAsync(() -> {
					try {
						return p2r.list(null);
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				});
				Thread.sleep(500);
				assertThat(listing).isNotDone();
				assertThat(index).doesNotExist();
			}

			assertThat(listing.get(60, TimeUnit.SECONDS)).isNotEmpty();
			assertThat(index).isFile();
			assertThat(w.getErrors()).isEmpty();

			// released after reading, and taken again for a refresh
			try (ProcessLock free = ProcessLock.tryLock(lockFile)
				.orElseThrow()) {
				assertThat(free.getFile()).isEqualTo(lockFile);
			}
			assertThat(p2r.refresh()).isTrue();
		}
	}

	@Test
	public void readsWithoutTheLockAfterTheTimeout() throws Exception {
		File index = new File(tmp, "cnf/ext/timeout_index.xml.gz");
		File cache = new File(tmp, "cache/timeout");
		long timeout = P2Indexer.lockTimeout;
		P2Indexer.lockTimeout = 200L;
		try (Workspace w = workspace();
			P2Repository p2r = repository(w, "timeout", IO.getFile("testdata/p2/macbadge"), index, cache);
			ProcessLock held = ProcessLock.tryLock(new File(cache, index.getName() + ".lock"))
				.orElseThrow()) {

			assertThat(p2r.list(null)).isNotEmpty();
			assertThat(index).isFile();
		} finally {
			P2Indexer.lockTimeout = timeout;
		}
	}
}
