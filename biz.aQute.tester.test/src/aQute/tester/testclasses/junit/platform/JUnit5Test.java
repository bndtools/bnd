package aQute.tester.testclasses.junit.platform;

import java.util.concurrent.atomic.AtomicReference;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5Test {
	public static AtomicReference<Thread> currentThread = new AtomicReference<>();

	@org.junit.jupiter.api.Test
	public void somethingElseAgain() {
		currentThread.set(Thread.currentThread());
	}
}
