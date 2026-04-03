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
		// Configure scheduled executor to prevent unbounded thread growth
		scheduledExecutor.setMaximumPoolSize(Math.max(corePoolSize, 4));
		scheduledExecutor.setKeepAliveTime(60L, TimeUnit.SECONDS);
		scheduledExecutor.allowCoreThreadTimeOut(false);
		
		// Use dedicated ScheduledThreadPoolExecutor for the promise factory
		promiseScheduledExecutor = new ScheduledThreadPoolExecutor(corePoolSize,
			new ExecutorThreadFactory(defaultThreadFactory, "Bnd-PromiseScheduledExecutor,"), rejectedExecutionHandler);
		// Configure promise scheduled executor to prevent unbounded thread growth
		promiseScheduledExecutor.setMaximumPoolSize(Math.max(corePoolSize, 4));
		promiseScheduledExecutor.setKeepAliveTime(60L, TimeUnit.SECONDS);
		promiseScheduledExecutor.allowCoreThreadTimeOut(false);
		
		promiseFactory = new PromiseFactory(executor, promiseScheduledExecutor);

		List<ThreadPoolExecutor> executors = Lists.of(scheduledExecutor, promiseScheduledExecutor, executor);

		// Configure shutdown policies for scheduled executors
		executors.forEach(exec -> {
			if (exec instanceof ScheduledThreadPoolExecutor scheduledExec) {
				scheduledExec.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
				scheduledExec.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
				scheduledExec.setRemoveOnCancelPolicy(true);
			}
		});

		// Install shutdown hook eagerly to avoid thread factory complications
		Thread shutdownThread = new Thread(() -> {
			executors.forEach(exec -> {
				exec.shutdown();
				try {
					exec.awaitTermination(20, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread()
						.interrupt();
				}
			});
		}, "Bnd-ExecutorShutdownHook");
		shutdownThread.setDaemon(false);
		try {
			Runtime.getRuntime()
				.addShutdownHook(shutdownThread);
		} catch (IllegalStateException e) {
			// VM is already shutting down...
			executors.forEach(ThreadPoolExecutor::shutdown);
		}
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
