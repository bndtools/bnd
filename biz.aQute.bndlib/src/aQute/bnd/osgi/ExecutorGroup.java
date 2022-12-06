package aQute.bnd.osgi;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.unmodifiable.Lists;

public class ExecutorGroup {

	static final class ExecutorThreadFactory implements ThreadFactory {
		private final ThreadFactory	delegate;
		private final String		prefix;

		ExecutorThreadFactory(ThreadFactory delegate, String prefix) {
			this.delegate = delegate;
			this.prefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = delegate.newThread(r);
			t.setName(prefix.concat(t.getName()));
			t.setDaemon(true);
			return t;
		}
	}

	static final class RejectedExecution implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
			if (executor.isShutdown()) {
				return;
			}
			try {
				runnable.run();
			} catch (Throwable t) {
				// We are stealing another's thread because we have hit max
				// pool size, so we cannot let the runnable's exception
				// propagate back up this thread.
				try {
					Thread thread = Thread.currentThread();
					thread.getUncaughtExceptionHandler()
						.uncaughtException(thread, t);
				} catch (Throwable for_real) {
					// we will ignore this
				}
			}
		}
	}

	private final ThreadPoolExecutor			executor;
	private final ScheduledThreadPoolExecutor	scheduledExecutor;
	// Use dedicated ScheduledThreadPoolExecutor for the promise factory
	private final ScheduledThreadPoolExecutor	promiseScheduledExecutor;
	private final PromiseFactory				promiseFactory;

	public ExecutorGroup() {
		this(2, Integer.getInteger("bnd.executor.maximumPoolSize", 256)
			.intValue());
	}

	public ExecutorGroup(int corePoolSize, int maximumPoolSize) {
		ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
		RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecution();
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L, TimeUnit.SECONDS,
			new SynchronousQueue<>(), new ExecutorThreadFactory(defaultThreadFactory, "Bnd-Executor,"),
			rejectedExecutionHandler);
		scheduledExecutor = new ScheduledThreadPoolExecutor(corePoolSize,
			new ExecutorThreadFactory(defaultThreadFactory, "Bnd-ScheduledExecutor,"), rejectedExecutionHandler);
		// Use dedicated ScheduledThreadPoolExecutor for the promise factory
		promiseScheduledExecutor = new ScheduledThreadPoolExecutor(corePoolSize,
			new ExecutorThreadFactory(defaultThreadFactory, "Bnd-PromiseScheduledExecutor,"), rejectedExecutionHandler);
		promiseFactory = new PromiseFactory(executor, promiseScheduledExecutor);

		List<ThreadPoolExecutor> executors = Lists.of(scheduledExecutor, promiseScheduledExecutor, executor);

		// Handle shutting down executors via shutdown hook
		AtomicBoolean shutdownHookInstalled = new AtomicBoolean();
		Function<ThreadPoolExecutor, ThreadFactory> shutdownHookInstaller = threadPoolExecutor -> {
			ThreadFactory threadFactory = threadPoolExecutor.getThreadFactory();
			return (Runnable r) -> {
				threadPoolExecutor.setThreadFactory(threadFactory);
				if (shutdownHookInstalled.compareAndSet(false, true)) {
					Thread shutdownThread = new Thread(() -> {
						executors.forEach(executor -> {
							executor.shutdown();
							try {
								executor.awaitTermination(20, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								Thread.currentThread()
									.interrupt();
							}
						});
					}, "Bnd-ExecutorShutdownHook");
					try {
						Runtime.getRuntime()
							.addShutdownHook(shutdownThread);
					} catch (IllegalStateException e) {
						// VM is already shutting down...
						executors.forEach(ThreadPoolExecutor::shutdown);
					}
				}
				return threadFactory.newThread(r);
			};
		};
		executors.forEach(executor -> {
			executor.setThreadFactory(shutdownHookInstaller.apply(executor));
			if (executor instanceof ScheduledThreadPoolExecutor scheduledExecutor) {
				scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
				scheduledExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
			}
		});
	}

	public Executor getExecutor() {
		return executor;
	}

	public ScheduledExecutorService getScheduledExecutor() {
		return scheduledExecutor;
	}

	public PromiseFactory getPromiseFactory() {
		return promiseFactory;
	}
}
