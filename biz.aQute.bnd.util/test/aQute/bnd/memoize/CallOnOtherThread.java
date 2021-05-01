package aQute.bnd.memoize;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import aQute.bnd.exceptions.RunnableWithException;

public class CallOnOtherThread {
	final Executor				executor;
	final long					timeout;
	final RunnableWithException	call;

	final CountDownLatch		started	= new CountDownLatch(1);
	final CountDownLatch		ended	= new CountDownLatch(1);
	volatile Throwable			exception;

	public CallOnOtherThread(RunnableWithException call, long timeout) {
		this(call, timeout, ForkJoinPool.commonPool());
	}

	public CallOnOtherThread(RunnableWithException call, long timeout, Executor executor) {
		this.timeout = timeout;
		this.call = call;
		this.executor = executor;
	}

	public void call() {
		executor.execute(() -> {
			started.countDown();
			try {
				call.run();
			} catch (Throwable e) {
				exception = e;
			} finally {
				ended.countDown();
			}
		});
	}

	public boolean hasStarted() {
		return check(started);
	}

	public boolean hasEnded() {
		return check(ended);
	}

	public Throwable getThrowable() {
		hasEnded();
		return exception;
	}

	private boolean check(CountDownLatch condition) {
		for (long end = System.nanoTime()
			+ TimeUnit.MILLISECONDS.toNanos(timeout), delay; (delay = end - System.nanoTime()) >= 0L;) {
			try {
				if (condition.await(delay, TimeUnit.NANOSECONDS)) {
					return true;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
