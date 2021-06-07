package aQute.tester.testclasses;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit4Test {
	public static AtomicReference<Thread>	currentThread	= new AtomicReference<>();
	public static AtomicBoolean				theOtherFlag	= new AtomicBoolean(false);

	@Test
	public void somethingElse() {
		currentThread.set(Thread.currentThread());
	}

	@Test
	public void theOther() {
		theOtherFlag.set(true);
	}
}
