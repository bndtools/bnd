package aQute.bnd.build;

import java.util.*;

public interface RunSession {

	String getName();

	String getLabel();

	int getJdb();

	void stderr(Appendable app) throws Exception;

	void stdout(Appendable app) throws Exception;

	void stdin(String input) throws Exception;

	int launch() throws Exception;

	void cancel() throws Exception;

	Map<String,Object> getProperties();

	int getExitCode();

	String getHost();

	int getAgent();

	void waitTillStarted(long ms) throws InterruptedException;

	long getTimeout();
}
