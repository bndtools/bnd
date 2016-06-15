package aQute.lib.exceptions;

public class Exceptions {
	static RuntimeException singleton = new RuntimeException();

	public static RuntimeException duck(Throwable t) {
		Exceptions.<RuntimeException> asUncheckedException0(t);
		return singleton;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> E asUncheckedException0(Throwable throwable) throws E {
		return (E) throwable;
	}

	public static Runnable wrap(final RunnableWithException run) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					run.run();
				} catch (Exception e) {
					duck(e);
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
					duck(e);
					return null; // will never happen
				}
			}
		};
	}

}
