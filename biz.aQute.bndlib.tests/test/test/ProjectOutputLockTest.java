package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.libg.filelock.ProcessLock;

/**
 * Another process that has the workspace open, for example an IDE, builds and
 * cleans the same target directory. It is represented here by its lock.
 */
public class ProjectOutputLockTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void buildAndCleanWaitForAnotherProcess() throws Exception {
		IO.copy(IO.getFile("testresources/ws"), tmp);
		File lockFile = new File(tmp, "cnf/cache/locks/p6.lock");
		try (Workspace ws = new Workspace(tmp)) {
			Project project = ws.getProject("p6");

			CompletableFuture<File[]> build;
			try (ProcessLock other = ProcessLock.tryLock(lockFile)
				.orElseThrow()) {
				build = CompletableFuture.supplyAsync(() -> {
					try {
						return project.build();
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				});
				Thread.sleep(500);
				assertThat(build).isNotDone();
				assertThat(new File(project.getTarget(), "p6.jar")).doesNotExist();
			}
			File[] files = build.get(60, TimeUnit.SECONDS);
			assertThat(project.check()).isTrue();
			assertThat(files).hasSize(1)
				.allMatch(File::isFile);

			CompletableFuture<Void> clean;
			try (ProcessLock other = ProcessLock.tryLock(lockFile)
				.orElseThrow()) {
				clean = CompletableFuture.runAsync(() -> {
					try {
						project.clean();
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				});
				Thread.sleep(500);
				assertThat(clean).isNotDone();
				assertThat(files[0]).isFile();
			}
			clean.get(60, TimeUnit.SECONDS);
			assertThat(files[0]).doesNotExist();
			assertThat(project.check()).isTrue();

			// the lock file is not part of the cleaned directories
			assertThat(lockFile).isFile();
			ProcessLock.tryLock(lockFile)
				.orElseThrow()
				.close();
		}
	}

	/**
	 * A listener runs inside the build and can build again, for example
	 * through {@link Project#getBuildFiles()}. That must not wait for the lock
	 * its own thread holds.
	 */
	@Test
	public void nestedBuildDoesNotWaitForItsOwnLock() throws Exception {
		IO.copy(IO.getFile("testresources/ws"), tmp);
		try (Workspace ws = new Workspace(tmp)) {
			Project project = ws.getProject("p6");
			AtomicInteger builds = new AtomicInteger();
			ws.addBasicPlugin(new BndListener() {
				@Override
				public void built(Project built, Collection<File> files) {
					if (builds.incrementAndGet() == 1) {
						try {
							built.buildLocal(false);
						} catch (Exception e) {
							throw new IllegalStateException(e);
						}
					}
				}
			});

			File[] files = CompletableFuture.supplyAsync(() -> {
				try {
					return project.build();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			})
				.get(60, TimeUnit.SECONDS);

			assertThat(builds).hasValue(2);
			assertThat(files).hasSize(1);
			assertThat(project.getWarnings()).isEmpty();
			assertThat(project.check()).isTrue();
		}
	}
}
