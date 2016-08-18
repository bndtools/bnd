package aQute.lib.exceptions;

public class Exceptions {
	private Exceptions() {}
	public static RuntimeException duck(Throwable t) {
		Exceptions.<RuntimeException> throwsUnchecked(t);
		throw new AssertionError("unreachable");
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}

	public static Runnable wrap(final RunnableWithException run) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					run.run();
				} catch (Exception e) {
					throw duck(e);
				}
			}

		};
	}

	public static <T, R> org.osgi.util.function.Function<T,R> wrap(final FunctionWithException<T,R> run) {
		return new org.osgi.util.function.Function<T,R>() {

			@Override
			public R apply(T value) {
				try {
					return run.apply(value);
				} catch (Exception e) {
					throw duck(e);
				}
			}
		};
	}

}
