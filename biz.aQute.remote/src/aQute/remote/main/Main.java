package aQute.remote.main;

import java.io.File;
import java.io.IOException;

import aQute.bnd.util.home.Home;
import aQute.lib.collections.ExtList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.remote.api.Agent;

/**
 * This is a main program. This starts an Envoy, a restricted Agent that will
 * first create a framework and then uses the same communications link for the
 * resulting agent.
 */
public class Main extends ReporterAdapter {

	static Main				main;
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
		String execute = commandLine.execute(this, "run", new ExtList<>(args));
		if (execute != null)
			getOut().format("%s\n", execute);
	}

	/**
	 * Options
	 */
	@Description("Start an envoy server. This envoy server can be contacted by a remote system which can then set a runpath and install an agent.")
	@Arguments(arg = {})
	interface RunOptions extends Options {
		@Description("Show exception stack traces")
		boolean exceptions();

		@Description("Show trace information")
		boolean trace();

		@Description("Set the agent's binary SHA cache directory")
		String cache(String deflt);

		@Description("Set the directory for the framework storage")
		String storage(String deflt);

		@Description("Set the port to listen to, default is " + Agent.DEFAULT_PORT)
		int port(int deflt);

		@Description("Set the network interface to register the socket listener on. If `-a` is set then the default is 0.0.0.0, else localhost")
		String network(String deflt);

		@Description("Register on all network interfaces")
		boolean all();
	}

	/**
	 * The real one
	 */
	@Description("Start an envoy server. This envoy server can be contacted by a remote system which can then set a runpath and install an agent.")
	public void _run(RunOptions options) throws Exception {

		setTrace(options.trace());
		setExceptions(options.exceptions());

		int port = options.port(Agent.DEFAULT_PORT);
		String network = options.network(options.all() ? "0.0.0.0" : "localhost");

		File cache = IO.getFile(options.cache(Home.getUserHomeBnd() + "/remote/cache"));
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

	static EnvoyDispatcher getDispatcher() {
		return main.dispatcher;
	}
}
