package aQute.libg.filelock;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An exclusive lock on a file that is honored by the threads of this JVM and
 * by other processes that use this class on the same file. The operating
 * system releases the lock when the owning process ends, also after a crash,
 * so there are no stale locks.
 * <p>
 * The lock is advisory: it only excludes parties that ask for it. It is not
 * reentrant. The lock file is created when needed and is never deleted; it
 * holds a description of the current holder.
 */
public final class ProcessLock implements Closeable {
	/*
	 * A byte outside of any content. This leaves the holder description
	 * readable on platforms with mandatory byte range locks.
	 */
	private static final long			LOCKED_BYTE	= 1L << 30;
	private static final long			POLL_MILLIS	= 50L;
	private static final Map<String, String>	held		= new HashMap<>();

	private final File					file;
	private final String				key;
	private final FileChannel			channel;
	private final FileLock				lock;
	private boolean						closed;

	private ProcessLock(File file, String key, FileChannel channel, FileLock lock) {
		this.file = file;
		this.key = key;
		this.channel = channel;
		this.lock = lock;
	}

	/**
	 * Acquire the lock if it is free.
	 *
	 * @param file the lock file, its parent directory is created when absent
	 * @return the lock, or empty when a thread in this JVM or another process
	 *         holds it
	 * @throws IOException when the lock file cannot be created or opened
	 */
	public static Optional<ProcessLock> tryLock(File file) throws IOException {
		File absolute = file.getAbsoluteFile();
		String key = absolute.getCanonicalPath();
		synchronized (held) {
			if (held.containsKey(key))
				return Optional.empty();

			Files.createDirectories(absolute.getParentFile()
				.toPath());
			FileChannel channel = FileChannel.open(absolute.toPath(), StandardOpenOption.CREATE,
				StandardOpenOption.READ, StandardOpenOption.WRITE);
			try {
				FileLock lock;
				try {
					lock = channel.tryLock(LOCKED_BYTE, 1L, false);
				} catch (OverlappingFileLockException e) {
					// held through another class loader in this JVM
					lock = null;
				}
				if (lock == null) {
					channel.close();
					return Optional.empty();
				}
				held.put(key, describe(channel));
				return Optional.of(new ProcessLock(absolute, key, channel, lock));
			} catch (IOException | RuntimeException e) {
				channel.close();
				throw e;
			}
		}
	}

	/**
	 * Acquire the lock, waiting for the current holder to release it.
	 *
	 * @param file the lock file, its parent directory is created when absent
	 * @param timeout the maximum time to wait
	 * @param unit the unit of the timeout
	 * @return the lock
	 * @throws TimeoutException when the lock is still held after the timeout
	 * @throws InterruptedException when the waiting thread is interrupted
	 * @throws IOException when the lock file cannot be created or opened
	 */
	public static ProcessLock lock(File file, long timeout, TimeUnit unit)
		throws IOException, InterruptedException, TimeoutException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		while (true) {
			Optional<ProcessLock> lock = tryLock(file);
			if (lock.isPresent())
				return lock.get();

			if (System.nanoTime() - deadline >= 0)
				throw new TimeoutException("Could not lock " + file + ", held by " + holder(file));

			Thread.sleep(POLL_MILLIS);
		}
	}

	/**
	 * A description of the process that holds, or last held, the lock. Only
	 * meaningful while the lock is held.
	 * <p>
	 * On POSIX systems closing any descriptor of a file releases all locks
	 * that the process holds on that file. The file is therefore only read
	 * when this JVM does not hold the lock.
	 *
	 * @param file the lock file
	 * @return the description, never {@code null}
	 */
	public static String holder(File file) {
		try {
			String key = file.getAbsoluteFile()
				.getCanonicalPath();
			synchronized (held) {
				String own = held.get(key);
				if (own != null)
					return own;

				String description = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
				return description.isEmpty() ? "unknown" : description;
			}
		} catch (IOException e) {
			return "unknown";
		}
	}

	private static String describe(FileChannel channel) throws IOException {
		String description = "pid " + ProcessHandle.current()
			.pid();
		channel.truncate(0L);
		channel.write(ByteBuffer.wrap(description.getBytes(StandardCharsets.UTF_8)), 0L);
		channel.force(false);
		return description;
	}

	public File getFile() {
		return file;
	}

	/**
	 * Release the lock. Closing a released lock has no effect.
	 */
	@Override
	public void close() throws IOException {
		synchronized (held) {
			if (closed)
				return;
			closed = true;
			try {
				if (lock.isValid())
					lock.release();
			} finally {
				try {
					channel.close();
				} finally {
					held.remove(key);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "ProcessLock[" + file + "]";
	}
}
