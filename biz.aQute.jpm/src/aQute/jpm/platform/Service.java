package aQute.jpm.platform;

public interface Service {
	boolean isRunning();

	String status();

	void stop();

	void start();
}
