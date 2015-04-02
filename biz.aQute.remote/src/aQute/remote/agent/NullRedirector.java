package aQute.remote.agent;

import java.io.*;

public class NullRedirector implements Redirector {

	@Override
	public void close() throws IOException {}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public void stdin(String s) {}

	@Override
	public PrintStream getOut() throws Exception {
		return System.out;
	}

}
