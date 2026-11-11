package aQute.libg.filelock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

public class ProcessLockTest {
	@InjectTemporaryDirectory
	File tmp;

	/**
	 * Holds the lock in another process until its standard input is closed.
	 */
	public static class Holder {
		public static void main(String[] args) throws Exception {
			try (ProcessLock lock = ProcessLock.tryLock(new File(args[0]))
				.orElseThrow(() -> new IllegalStateException("lock is held"))) {
				System.out.println("locked");
				System.out.flush();
				while (System.in.read() >= 0) {}
			}
		}
	}

	@Test
	public void excludesSecondHolderUntilReleased() throws Exception {
		File file = new File(tmp, "nested/dir/test.lock");

		try (ProcessLock lock = ProcessLock.tryLock(file)
			.orElseThrow()) {
			assertThat(file).isFile();
			assertThat(lock.getFile()).isEqualTo(file.getAbsoluteFile());
			assertThat(ProcessLock.tryLock(file)).isEmpty();
			assertThat(ProcessLock.tryLock(new File(tmp, "nested/dir/../dir/test.lock"))).isEmpty();
			assertThat(ProcessLock.holder(file)).isEqualTo("pid " + ProcessHandle.current()
				.pid());
			lock.close();
			lock.close();
		}

		Optional<ProcessLock> again = ProcessLock.tryLock(file);
		assertThat(again).isPresent();
		again.get()
			.close();
	}

	@Test
	public void differentFilesDoNotExclude() throws Exception {
		try (ProcessLock a = ProcessLock.tryLock(new File(tmp, "a.lock"))
			.orElseThrow();
			ProcessLock b = ProcessLock.tryLock(new File(tmp, "b.lock"))
				.orElseThrow()) {
			assertThat(a.getFile()).isNotEqualTo(b.getFile());
		}
	}

	@Test
	public void waitsForReleaseAndTimesOut() throws Exception {
		File file = new File(tmp, "test.lock");
		ProcessLock first = ProcessLock.tryLock(file)
			.orElseThrow();
		try {
			assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> ProcessLock.lock(file, 200, TimeUnit.MILLISECONDS))
				.withMessageContaining("pid ");

			CompletableFuture<ProcessLock> second = CompletableFuture.supplyAsync(() -> {
				try {
					return ProcessLock.lock(file, 30, TimeUnit.SECONDS);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
			Thread.sleep(300);
			assertThat(second).isNotDone();

			first.close();
			second.get(30, TimeUnit.SECONDS)
				.close();
		} finally {
			first.close();
		}
	}

	@Test
	public void excludesOtherProcessAndIsReleasedWhenItDies() throws Exception {
		File file = new File(tmp, "test.lock");
		String classpath = location(ProcessLock.class) + File.pathSeparator + location(Holder.class);
		Process process = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java")
			.toString(), "-cp", classpath, Holder.class.getName(), file.getAbsolutePath())
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();
		try {
			BufferedReader out = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			assertThat(out.readLine()).isEqualTo("locked");

			assertThat(ProcessLock.tryLock(file)).isEmpty();
			assertThat(ProcessLock.holder(file)).isEqualTo("pid " + process.pid());
			assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> ProcessLock.lock(file, 200, TimeUnit.MILLISECONDS))
				.withMessageContaining("pid " + process.pid());

			// no orderly release: the operating system must free the lock
			process.destroyForcibly()
				.waitFor(30, TimeUnit.SECONDS);
			ProcessLock.lock(file, 30, TimeUnit.SECONDS)
				.close();
		} finally {
			process.destroyForcibly();
		}
	}

	private static String location(Class<?> type) throws Exception {
		return Paths.get(type.getProtectionDomain()
			.getCodeSource()
			.getLocation()
			.toURI())
			.toString();
	}
}
