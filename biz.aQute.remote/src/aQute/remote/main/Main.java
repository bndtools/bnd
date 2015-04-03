package aQute.remote.main;

import java.io.*;

import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;
import aQute.remote.api.*;

/**
 * This is a main program. This starts an Envoy, a restricted Agent that will
 * first create a framework and then uses the same communications link for the
 * resulting agent.
 */
public class Main extends ReporterAdapter {

	private static Main		main;
	private CommandLine		commandLine;
	private EnvoyDispatcher	dispatcher;

	/**
	 * Constructor
	 */
	public Main() throws Exception {
		super(System.out);
		commandLine = new CommandLine(this);
	}

	/**
	 * 
	 */
	private void run(String[] args) throws Exception {
		String execute = commandLine.execute(this, "run", new ExtList<String>(args));
		if (execute != null)
			getOut().format("%s\n", execute);
	}

	/**
	 * Options
	 */
	interface RunOptions extends Options {
		boolean exceptions();

		boolean trace();

		String cache(String deflt);

		String storage(String deflt);

		int port(int deflt);

		String network(String deflt);
	}

	/**
	 * The real one
	 */
	public void _run(RunOptions options) throws Exception {

		setTrace(options.trace());
		setExceptions(options.exceptions());

		int port = options.port(Agent.DEFAULT_PORT);
		String network = options.network("localhost");

		File cache = IO.getFile(options.cache("~/.bnd/remote/cache"));
		File storage = IO.getFile(options.storage("storage"));

		dispatcher = new EnvoyDispatcher(this, cache, storage, network, port);
		dispatcher.run();
	}

	public static void main(String[] args) throws Exception {
		main = new Main();
		main.run(args);
	}

	public static void stop() throws IOException {
		main.dispatcher.close();
	}

}
