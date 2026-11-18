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
import java.util.Map;
import java.util.Optional;
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
	/*
	 * On POSIX, closing any descriptor of a file releases the locks that the
	 * process holds on it. Copies of this class in other class loaders share
	 * the lock table of the JDK but not the maps below, so a descriptor is
	 * never closed while this JVM may hold the lock through it or another
	 * one: a probe that finds the lock held in this JVM is parked instead. All
	 * copies serialize on an interned string, which is one object per JVM.
	 */
	private static final Object					MONITOR		= "aQute.libg.filelock.ProcessLock.MONITOR";
	private static final Map<String, String>		held		= new HashMap<>();
	private static final Map<String, FileChannel>	parked		= new HashMap<>();

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
		synchronized (MONITOR) {
			if (held.containsKey(key))
				return Optional.empty();

			Files.createDirectories(absolute.getParentFile()
				.toPath());
			FileChannel channel = parked.remove(key);
			if (channel == null)
				channel = FileChannel.open(absolute.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ,
					StandardOpenOption.WRITE);
			FileLock lock = acquire(key, channel);
			if (lock == null)
				return Optional.empty();
			try {
				String description = describe(channel);
				held.put(key, description);
				return Optional.of(new ProcessLock(absolute, key, channel, lock));
			} catch (IOException | RuntimeException e) {
				channel.close();
				throw e;
			}
		}
	}

	/**
	 * Tell whether a thread in this JVM or another process holds the lock.
	 * Unlike {@link #tryLock(File)} this does not create or write the file.
	 *
	 * @param file the lock file
	 * @return {@code true} when the lock is held
	 * @throws IOException when an existing lock file cannot be opened
	 */
	public static boolean isLocked(File file) throws IOException {
		String key = file.getAbsoluteFile()
			.getCanonicalPath();
		synchronized (MONITOR) {
			if (held.containsKey(key))
				return true;
			if (!file.isFile())
				return false;

			FileChannel channel = parked.remove(key);
			if (channel == null)
				channel = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
			FileLock lock = acquire(key, channel);
			if (lock == null)
				return true;
			channel.close();
			return false;
		}
	}

	/*
	 * Returns null when the lock is held elsewhere. The channel is then closed
	 * when another process holds the lock, which means that this JVM does not,
	 * and parked when this JVM holds it.
	 */
	private static FileLock acquire(String key, FileChannel channel) throws IOException {
		FileLock lock;
		try {
			lock = channel.tryLock(LOCKED_BYTE, 1L, false);
		} catch (OverlappingFileLockException e) {
			parked.put(key, channel);
			return null;
		} catch (IOException | RuntimeException e) {
			channel.close();
			throw e;
		}
		if (lock == null)
			channel.close();
		return lock;
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
	 * that the process holds on that file. The file is therefore not read
	 * when this JVM holds the lock.
	 *
	 * @param file the lock file
	 * @return the description, never {@code null}
	 */
	public static String holder(File file) {
		try {
			String key = file.getAbsoluteFile()
				.getCanonicalPath();
			synchronized (MONITOR) {
				String own = held.get(key);
				if (own != null)
					return own;

				FileChannel channel = parked.remove(key);
				if (channel == null)
					channel = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
				try {
					channel.tryLock(LOCKED_BYTE, 1L, false);
				} catch (OverlappingFileLockException e) {
					parked.put(key, channel);
					return "pid " + ProcessHandle.current()
						.pid();
				} catch (IOException | RuntimeException e) {
					channel.close();
					throw e;
				}
				try (FileChannel c = channel) {
					ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(c.size(), 1024L));
					c.read(buffer, 0L);
					String description = new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8)
						.trim();
					return description.isEmpty() ? "unknown" : description;
				}
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
		synchronized (MONITOR) {
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
