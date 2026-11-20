package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.result.Result;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.libg.filelock.ProcessLock;

/**
 * Another process that has the workspace open expands the same JAR into the
 * cache. It is represented here by its lock.
 */
public class ExpandedCacheLockTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void expandWaitsForAnotherProcess() throws Exception {
		IO.copy(IO.getFile("testresources/ws"), tmp);
		File jar = new File(tmp, "content.jar");
		try (Jar content = new Jar("content")) {
			content.putResource("hello.txt", new aQute.bnd.osgi.EmbeddedResource("hello", 0L));
			content.write(jar);
		}
		String urn = "urn:resource:content-1.0.0";
		File lockFile = new File(tmp, "cnf/cache/locks/expanded-" + Integer.toHexString(urn.hashCode()) + ".lock");

		try (Workspace ws = new Workspace(tmp)) {
			CompletableFuture<Result<File>> expanded;
			try (ProcessLock other = ProcessLock.tryLock(lockFile)
				.orElseThrow()) {
				expanded = CompletableFuture.supplyAsync(() -> {
					try {
						return ws.getExpandedInCache(urn, jar);
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				});
				Thread.sleep(500);
				assertThat(expanded).isNotDone();
			}

			Result<File> result = expanded.get(60, TimeUnit.SECONDS);
			assertThat(result.isOk()).isTrue();
			assertThat(new File(result.unwrap(), "hello.txt")).hasContent("hello");
			assertThat(ws.getWarnings()).isEmpty();

			// the lock file is kept and can be taken again
			assertThat(lockFile).isFile();
			ProcessLock.tryLock(lockFile)
				.orElseThrow()
				.close();
		}
	}
}
