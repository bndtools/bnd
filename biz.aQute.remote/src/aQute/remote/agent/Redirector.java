package aQute.remote.agent;

import java.io.Closeable;
import java.io.PrintStream;

public interface Redirector extends Closeable {
	int COMMAND_SESSION = -1;
	int NONE = 0;
	int CONSOLE = 1;

	int getPort();

	void stdin(String s) throws Exception;

	PrintStream getOut() throws Exception;
}
