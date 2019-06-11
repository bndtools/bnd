package aQute.launchpad;

import java.io.Closeable;

public interface Probe {

	void foo();

	Closeable enable(Class<?> componentClass);

	boolean isOk();
}
