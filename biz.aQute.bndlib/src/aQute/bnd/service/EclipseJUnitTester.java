package aQute.bnd.service;

public interface EclipseJUnitTester {
	void setPort(int port);

	void setHost(String host);

	default void setControlPort(int port) {
		throw new IllegalStateException("Control port not supported by " + getClass());
	}
}
