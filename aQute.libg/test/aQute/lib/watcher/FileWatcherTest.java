package aQute.lib.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.StandardWatchEventKinds;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.lib.io.IO;

public class FileWatcherTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;

	private ExecutorService		executor;

	@Before
	public void before() throws Exception {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
		executor = Executors.newFixedThreadPool(10);
	}

	@After
	public void after() throws Exception {
		executor.shutdownNow();
	}

	@Test
	public void testWatchingChange1() throws Exception {
		File test1 = new File(testDir, "foo/bar.txt");
		IO.mkdirs(test1.getParentFile());
		IO.store("test1", test1);
		File test2 = new File(testDir, "bar/foo.txt");
		IO.mkdirs(test2.getParentFile());
		IO.store("test2", test2);

		Set<File> changed = ConcurrentHashMap.newKeySet();
		CountDownLatch latch = new CountDownLatch(1);
		FileWatcher fw = new FileWatcher.Builder().files(test1, test2)
			.executor(executor)
			.changed((file, kind) -> {
				if (StandardWatchEventKinds.ENTRY_MODIFY.name()
					.equals(kind) && changed.add(file)) {
					latch.countDown();
				}
			})
			.build();

		assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse();
		IO.store("test2changed", test2);

		assertThat(latch.await(30, TimeUnit.SECONDS)).as("Failed to detect changes")
			.isTrue();
		assertThat(fw.await(2, TimeUnit.SECONDS)).isFalse();
		fw.close();
		assertThat(fw.await(2, TimeUnit.SECONDS)).as("")
			.isTrue();

		assertThat(changed).containsExactlyInAnyOrder(test2);
	}

	@Test
	public void testWatchingChange2() throws Exception {
		File test1 = new File(testDir, "foo/bar.txt");
		IO.mkdirs(test1.getParentFile());
		IO.store("test1", test1);
		File test2 = new File(testDir, "bar/foo.txt");
		IO.mkdirs(test2.getParentFile());
		IO.store("test2", test2);

		Set<File> changed = ConcurrentHashMap.newKeySet();
		CountDownLatch latch = new CountDownLatch(2);
		FileWatcher fw = new FileWatcher.Builder().files(test1, test2)
			.executor(executor)
			.changed((file, kind) -> {
				if (StandardWatchEventKinds.ENTRY_MODIFY.name()
					.equals(kind) && changed.add(file)) {
					latch.countDown();
				}
			})
			.build();

		assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse();
		IO.store("test1changed", test1);
		IO.store("test2changed", test2);

		assertThat(latch.await(30, TimeUnit.SECONDS)).as("Failed to detect changes")
			.isTrue();
		assertThat(fw.await(2, TimeUnit.SECONDS)).isFalse();
		fw.close();
		assertThat(fw.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(changed).containsExactlyInAnyOrder(test1, test2);
	}

	@Test
	public void testWatchingDeleted() throws Exception {
		File test1 = new File(testDir, "foo/bar.txt");
		IO.mkdirs(test1.getParentFile());
		IO.store("test1", test1);
		File test2 = new File(testDir, "bar/foo.txt");
		IO.mkdirs(test2.getParentFile());
		IO.store("test2", test2);

		Set<File> changed = ConcurrentHashMap.newKeySet();
		CountDownLatch latch = new CountDownLatch(1);
		FileWatcher fw = new FileWatcher.Builder().files(test1, test2)
			.executor(executor)
			.changed((file, kind) -> {
				if (StandardWatchEventKinds.ENTRY_DELETE.name()
					.equals(kind) && changed.add(file)) {
					latch.countDown();
				}
			})
			.build();

		assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse();
		IO.delete(test2);

		assertThat(latch.await(30, TimeUnit.SECONDS)).as("Failed to detect changes")
			.isTrue();
		assertThat(fw.await(2, TimeUnit.SECONDS)).isFalse();
		fw.close();
		assertThat(fw.await(2, TimeUnit.SECONDS)).as("")
			.isTrue();

		assertThat(changed).containsExactlyInAnyOrder(test2);
	}

	@Test
	public void testWatchingCreated() throws Exception {
		File test1 = new File(testDir, "foo/bar.txt");
		IO.mkdirs(test1.getParentFile());
		IO.store("test1", test1);
		File test2 = new File(testDir, "bar/foo.txt");
		IO.mkdirs(test2.getParentFile());

		Set<File> changed = ConcurrentHashMap.newKeySet();
		CountDownLatch latch = new CountDownLatch(1);
		FileWatcher fw = new FileWatcher.Builder().files(test1, test2)
			.executor(executor)
			.changed((file, kind) -> {
				if (StandardWatchEventKinds.ENTRY_CREATE.name()
					.equals(kind) && changed.add(file)) {
					latch.countDown();
				}
			})
			.build();

		assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse();
		IO.store("test2", test2);

		assertThat(latch.await(30, TimeUnit.SECONDS)).as("Failed to detect changes")
			.isTrue();
		assertThat(fw.await(2, TimeUnit.SECONDS)).isFalse();
		fw.close();
		assertThat(fw.await(2, TimeUnit.SECONDS)).as("")
			.isTrue();

		assertThat(changed).containsExactlyInAnyOrder(test2);
	}

}
