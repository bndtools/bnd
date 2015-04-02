package aQute.remote.main;

import java.io.*;

import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

public class Main extends ReporterAdapter {

	private static Main		main;
	private CommandLine		commandLine;
	private EnvoyDispatcher	dispatcher;

	public Main() throws Exception {
		super(System.out);
		commandLine = new CommandLine(this);
	}

	private void run(String[] args) throws Exception {
		String execute = commandLine.execute(this, "run", new ExtList<String>(args));
		if (execute != null)
			getOut().format("%s\n", execute);
	}

	interface RunOptions extends Options {
		boolean exceptions();

		boolean trace();

		String cache(String deflt);

		String storage(String deflt);

		int port(int deflt);

		String network(String deflt);
	}

	public void _run(RunOptions options) throws Exception {

		setTrace(options.trace());
		setExceptions(options.exceptions());

		int port = options.port(Envoy.DEFAULT_PORT);
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
