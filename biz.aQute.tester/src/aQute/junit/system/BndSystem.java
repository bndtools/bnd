package aQute.junit.system;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

/**
 * Centralizes calling exit
 */
public class BndSystem {

	public static AtomicReference<IntConsumer> exit = new AtomicReference<>(System::exit);

	public static void exit(int code) {
		exit.get()
			.accept(code);
	}
}
