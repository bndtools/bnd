package aQute.remote.agent;

import java.io.*;

/**
 * API def for a redirector.
 */
public interface Redirector extends Closeable {

	/**
	 * The port (or pseudo port) this one is connected to
	 * 
	 * @return the port
	 */
	int getPort();

	/**
	 * Provide input
	 * 
	 * @param s
	 *            the input
	 */
	void stdin(String s) throws Exception;

	/**
	 * Get the output stream
	 */
	PrintStream getOut() throws Exception;
}
