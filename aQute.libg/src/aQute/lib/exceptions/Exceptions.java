package aQute.lib.exceptions;

public class Exceptions {

	public static RuntimeException duck(Throwable t) {
		return Exceptions.<RuntimeException> asUncheckedException0(t);
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
