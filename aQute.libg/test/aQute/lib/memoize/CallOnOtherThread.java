package aQute.lib.memoize;

import java.util.concurrent.ForkJoinPool;
import java.util.function.BooleanSupplier;

import aQute.lib.exceptions.RunnableWithException;

public class CallOnOtherThread {
	final long					timeout;
	final RunnableWithException	call;

	volatile boolean			started;
	volatile boolean			ended;
	volatile Throwable			exception;

	public CallOnOtherThread(RunnableWithException call, long timeout) {
		this.timeout = 0;
		this.call = call;
	}

	public void call() {
		ForkJoinPool.commonPool()
			.execute(() -> {
				started = true;
				try {
					call.run();
				} catch (Throwable e) {
					exception = e;
				} finally {
					ended = true;
				}
			});
	}

	public boolean hasStarted() {
		return check(() -> started);
	}

	public boolean hasEnded() {
		return check(() -> ended);
	}

	public Throwable getThrowable() {
		hasEnded();
		return exception;
	}

	private boolean check(BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + timeout;
		while (!condition.getAsBoolean()) {
			if (deadline < System.currentTimeMillis())
				return false;

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
}
