package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class Exceptions {
	private Exceptions() {}

	public static <V> V unchecked(Callable<? extends V> callable) {
		try {
			return callable.call();
		} catch (Exception t) {
			throw duck(t);
		}
	}

	public static void unchecked(RunnableWithException runnable) {
		try {
			runnable.run();
		} catch (Exception t) {
			throw duck(t);
		}
	}

	public static RuntimeException duck(Throwable t) {
		Exceptions.throwsUnchecked(t);
		throw new AssertionError("unreachable");
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}

	public static String toString(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static Throwable unrollCause(Throwable t, Class<? extends Throwable> unrollType) {
		requireNonNull(t);
		for (Throwable cause; unrollType.isInstance(t) && ((cause = t.getCause()) != null);) {
			t = cause;
		}
		return t;
	}

	public static Throwable unrollCause(Throwable t) {
		return unrollCause(t, Throwable.class);
	}

	public static String causes(Throwable t) {
		StringJoiner sj = new StringJoiner(" -> ");
		while (t != null) {
			sj.add(t.getMessage());
			t = t.getCause();
		}
		return sj.toString();
	}

}
