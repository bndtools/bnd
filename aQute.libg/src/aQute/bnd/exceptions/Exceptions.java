package aQute.bnd.exceptions;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Exceptions {
	final static Pattern DISPLAY_P = Pattern.compile("(.*)(Exception|Error)");

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
			String message = t.getMessage();
			sj.add(message == null ? t.getClass()
				.getSimpleName() : message);
			t = t.getCause();
		}
		return sj.toString();
	}

	/**
	 * Return a display name of an exception type. This is basically removing
	 * the package and the Exception or Error suffix.
	 *
	 * @param e the exception
	 * @return a display name for its type
	 */
	public static String getDisplayTypeName(Throwable e) {
		String name = e.getClass()
			.getSimpleName();

		Matcher m = DISPLAY_P.matcher(name);
		if (m.matches()) {
			return m.group(1);
		} else
			return name;
	}

}
