package aQute.junit.system;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Centralizes calling exit. Handling might give the impression that is clumsy
 * but realize that this class is sometimes used with multiple classloaders that
 * hold this class. E.g. Launchpad. That is the reason system properties are
 * used.
 */
public class BndSystem {

	static final String							THROW_ERROR_ON_EXIT	= "aQute.throw.error.on.exit";

	@Deprecated
	public static AtomicReference<IntConsumer> exit = new AtomicReference<>(System::exit);

	public static void exit(int code) {
		if ( Boolean.getBoolean(THROW_ERROR_ON_EXIT)) {
			throw new Error(THROW_ERROR_ON_EXIT + code);
		}
		exit.get()
			.accept(code);
	}

	public static AutoCloseable throwErrorOnExit() {
		System.getProperties()
			.setProperty(THROW_ERROR_ON_EXIT, "true");
		return () -> {
			System.getProperties()
				.setProperty(THROW_ERROR_ON_EXIT, "false");
		};
	}

	public static Throwable convert(Throwable throwable, Function<Integer, Throwable> converter) {
		if (throwable instanceof Error) {
			String message = throwable.getMessage();
			if (message.startsWith(THROW_ERROR_ON_EXIT)) {
				try {
					return converter.apply(Integer.parseInt(message.substring(THROW_ERROR_ON_EXIT.length())));
				} catch (NumberFormatException nfe) {
				}
			}
		}
		return throwable;
	}
}
