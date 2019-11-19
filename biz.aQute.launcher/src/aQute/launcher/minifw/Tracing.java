package aQute.launcher.minifw;

@FunctionalInterface
public interface Tracing {
	Tracing noop = (String msg, Object... objects) -> {};

	void trace(String msg, Object... objects);
}
