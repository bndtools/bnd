package aQute.remote.api;

/**
 * A Supervisor handles the initiating side of a session with a remote agent.
 * The methods defined in this interface are intended to be called by the remote
 * agent, not the initiator. I.e. this is not the interface the initiator will
 * use to control the session.
 */
public interface Supervisor {

	/**
	 * An event sent from the agent.
	 *
	 * @param e the event
	 */
	void event(Event e) throws Exception;

	/**
	 * Callback method for OSGi Events delivered by {@code EventAdmin}
	 * <p>
	 * Note that, this is only possible if the remote runtime has Event Admin
	 * bundle installed.
	 */
	default void onOSGiEvent(XEventDTO event) {}

	/**
	 * Callback method for logs delivered by {@code LogService}
	 * <p>
	 * Note that, this is only possible if the remote runtime has OSGi log
	 * bundle installed.
	 */
	default void logged(XLogEntryDTO event) {}

	/**
	 * Redirected standard output
	 *
	 * @param out the text that was redirected
	 * @return ignored (to make sync)
	 */
	boolean stdout(String out) throws Exception;

	/**
	 * Redirected standard error.
	 *
	 * @param out the text that was redirected
	 * @return ignored (to make sync)
	 */
	boolean stderr(String out) throws Exception;

	/**
	 * Return the contents of the file that has the given SHA-1. The initiator
	 * of the connection should in general register the files it refers to in
	 * the communication to the agent. The agent then calls this method to
	 * retrieve the contents if it does not have it in its local cache.
	 *
	 * @param sha the SHA-1
	 * @return the contents of that file or null if no such file exists.
	 */
	byte[] getFile(String sha) throws Exception;
}
