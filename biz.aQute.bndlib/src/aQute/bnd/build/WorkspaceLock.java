package aQute.bnd.build;

import static aQute.bnd.stream.DropWhile.dropWhile;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.strings.Strings;

/**
 * ReentrantReadWriteLock lock for serializing access to the Workspace.
 */
final class WorkspaceLock extends ReentrantReadWriteLock {
	private static final long	serialVersionUID	= 1L;
	private static final Logger	logger				= LoggerFactory.getLogger(WorkspaceLock.class);
	private final AtomicInteger	progress			= new AtomicInteger();

	WorkspaceLock(boolean fair) {
		super(fair);
	}

	private String type(Lock lock) {
		if (lock == readLock()) {
			return "read";
		}
		if (lock == writeLock()) {
			return "write";
		}
		return "unknown_lock";
	}

	private void trace(String name, Lock lock) {
		if (logger.isDebugEnabled()) {
			String trace = dropWhile(Arrays.stream(new Exception().getStackTrace()), ste -> {
				String className = ste.getClassName();
				if (Objects.equals(className, "aQute.bnd.build.WorkspaceLock")) {
					return true;
				}
				if (Objects.equals(className, "aQute.bnd.build.Workspace")) {
					String methodName = ste.getMethodName();
					if (Objects.equals(methodName, "readLocked") || Objects.equals(methodName, "writeLocked")) {
						return true;
					}
				}
				return false;
			})
				.limit(5L)
				.map(Object::toString)
				.collect(Strings.joining("\n\tat ", "\n\tat ", "", ""));
			logger.debug("{} {} @{}[Write locks = {}, Read locks = {}]; owner {}; waiting {}{}", name, type(lock),
				Integer.toHexString(hashCode()), getWriteHoldCount(), getReadLockCount(), getOwner(),
				getQueuedThreads(), trace);
		}
	}

	private Throwable getOwnerCause() {
		final Thread owner = getOwner();
		if (owner == null) {
			return null;
		}
		final Throwable cause = new Throwable(
			owner + " owns the WorkspaceLock\n\nFull thread dump:\n" + dumpAllThreads() + "Owner stacktrace:");
		cause.setStackTrace(owner.getStackTrace());
		return cause;
	}

	private String dumpAllThreads() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		return Arrays
			.stream(threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
				threadMXBean.isSynchronizerUsageSupported()))
			.map(Object::toString)
			.collect(Collectors.joining());
	}

	private CancellationException canceled(Lock lock) {
		CancellationException e = new CancellationException(
			String.format("Canceled waiting to %s acquire %s; owner %s; waiting %s", type(lock), this, getOwner(),
				getQueuedThreads()));
		e.initCause(getOwnerCause());
		return e;
	}

	private TimeoutException timeout(Lock lock) {
		TimeoutException e = new TimeoutException(
			String.format("Timeout waiting to %s acquire %s; owner %s; waiting %s", type(lock), this, getOwner(),
				getQueuedThreads()));
		e.initCause(getOwnerCause());
		return e;
	}

	<T> T locked(final Lock lock, final long timeoutInMs, final Callable<T> callable, final BooleanSupplier canceled)
		throws Exception {
		boolean interrupted = Thread.interrupted();
		trace("Enter", lock);
		try {
			int startingProgress = progress.get();
			long remaining = timeoutInMs;
			do {
				if (canceled.getAsBoolean()) {
					throw canceled(lock);
				}
				long wait = Math.min(remaining, 1_000L); // max is 1s
				long start = System.nanoTime();
				boolean locked;
				try {
					locked = lock.tryLock(wait, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					interrupted = true;
					throw e;
				}
				if (locked) {
					try {
						return callable.call();
					} finally {
						progress.incrementAndGet();
						lock.unlock();
					}
				}
				int currentProgress = progress.get();
				if (startingProgress == currentProgress) {
					long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
					remaining -= elapsed;
					trace("Busy with no progress", lock);
				} else {
					startingProgress = currentProgress;
					trace("Busy but progressing", lock);
				}
			} while (remaining > 0L);
			throw timeout(lock);
		} finally {
			trace("Exit", lock);
			if (interrupted) {
				Thread.currentThread()
					.interrupt();
			}
		}
	}
}
