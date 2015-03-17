package aQute.remote.main;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import aQute.lib.collections.ExtList;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.shacache.ShaCache;
import aQute.remote.util.Dispatcher;
import aQute.remote.util.Linkable;

public class Main extends ReporterAdapter implements Callable<Linkable<Envoy,EnvoySupervisor>> {

	private static ShaCache shacache;
	private static Main main;
	private CommandLine commandLine;
	private File storage;
	private Dispatcher<Envoy,EnvoySupervisor> dispatcher;

	public Main() throws Exception {
		super(System.out);
		commandLine = new CommandLine(this);
	}

	private void run(String[] args) throws Exception {
		String execute = commandLine.execute(this, "run", new ExtList<String>(
				args));
		if (execute != null)
			getOut().format("%s\n", execute);
	}

	interface RunOptions extends Options {
		boolean exceptions();

		boolean trace();

		String cache();
		
		String storage();

		int port(int deflt);

		String network();
	}

	public void _run(RunOptions options) throws Exception {
		
		setTrace(options.trace());
		setExceptions(options.exceptions());

		int port = options.port(Envoy.DEFAULT_PORT);
		File cache = IO.getFile(options.cache() == null ? "~/.bnd/remote/cache" : options.cache());
		storage = IO.getFile(options.storage() == null ? "storage" : options.cache());
		String network = options.network() == null ? "localhost" : options.network();

		cache.mkdirs();

		if (!cache.isDirectory())
			throw new IllegalArgumentException("Cannot create cache dir "
					+ cache);

		shacache = new ShaCache(cache);
		
		storage.mkdirs();

		if (!storage.isDirectory())
			throw new IllegalArgumentException("Cannot create storage dir "
					+ storage);

		dispatcher = new Dispatcher<Envoy, EnvoySupervisor>(
				EnvoySupervisor.class,this, network, port);
		dispatcher.open();
		dispatcher.join();
	}

	@Override
	public Linkable<Envoy, EnvoySupervisor> call() throws Exception {
		return new EnvoyImpl(this,shacache,storage);
	}
	
	private void close() throws IOException {
		dispatcher.close();
	}
	
	public static void main(String[] args) throws Exception {
		main = new Main();
		main.run(args);
	}

	public static void stop() throws IOException {
		main.close();
	}

}
