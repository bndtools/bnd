package aQute.libg.filelock;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Locks of directories that a process owns while it runs, for example the
 * storage of an OSGi framework. The lock of a directory is the
 * {@link ProcessLock} {@code <directory>.lock} next to it.
 * <p>
 * Deleting a directory also deletes the locks of the directories inside it,
 * and a directory inside an owned directory belongs to its owner. A party
 * that deletes a directory therefore checks the locks above and below it,
 * and a party that takes a lock checks the locks above it. Both do so while
 * holding the {@link #guard(long, TimeUnit) guard}, so that a lock cannot be
 * taken between the check and the deletion.
 */
public final class StorageLocks {
	private StorageLocks() {}

	/**
	 * The lock file of a directory.
	 *
	 * @param directory the directory, canonical
	 * @return the file {@code <directory>.lock} next to it
	 */
	public static File lockFile(File directory) {
		return new File(directory.getParentFile(), directory.getName() + ".lock");
	}

	/**
	 * Acquire the guard that serializes taking directory locks with checking
	 * and deleting directories. It is held only briefly. It lives in the
	 * {@code .bnd} directory of the user, outside of any directory that is
	 * deleted.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit the unit of the timeout
	 * @return the guard
	 * @throws IOException when the guard file cannot be created or opened
	 * @throws TimeoutException when the guard is still held after the
	 *             timeout
	 * @throws InterruptedException when the waiting thread is interrupted
	 */
	public static ProcessLock guard(long timeout, TimeUnit unit)
		throws IOException, InterruptedException, TimeoutException {
		String home = System.getProperty("user.home");
		if (home == null)
			throw new IOException("No user.home for the storage lock guard");
		return ProcessLock.lock(new File(home, ".bnd/locks/storage.lock"), timeout, unit);
	}

	/**
	 * Find a held lock that makes a directory belong to another party: the
	 * lock of the directory itself or of a directory that contains it, and,
	 * when the directory will be deleted, any lock inside it.
	 *
	 * @param directory the directory, canonical
	 * @param deleted {@code true} when the directory will be deleted
	 * @param ignore lock files of the caller, canonical
	 * @return the held lock file, or empty
	 * @throws IOException when an existing lock file cannot be checked, or a
	 *             directory below cannot be read, so that the check is
	 *             incomplete
	 */
	public static Optional<File> findHeld(File directory, boolean deleted, Collection<File> ignore)
		throws IOException {
		for (File d = directory; d != null && d.getParentFile() != null; d = d.getParentFile()) {
			File lock = lockFile(d);
			if (!ignore.contains(lock) && ProcessLock.isLocked(lock))
				return Optional.of(lock);
		}
		if (!deleted || !directory.isDirectory())
			return Optional.empty();

		List<File> locks = new ArrayList<>();
		Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
				if (attributes.isRegularFile() && file.getFileName()
					.toString()
					.endsWith(".lock"))
					locks.add(file.toFile());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
				// removed while walking, so it holds no lock
				if (e instanceof NoSuchFileException)
					return FileVisitResult.CONTINUE;
				throw e;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
				if (e != null && !(e instanceof NoSuchFileException))
					throw e;
				return FileVisitResult.CONTINUE;
			}
		});
		for (File lock : locks) {
			if (!ignore.contains(lock) && ProcessLock.isLocked(lock))
				return Optional.of(lock);
		}
		return Optional.empty();
	}
}
