package aQute.remote.agent.provider;

import java.io.File;
import java.util.concurrent.Callable;

import aQute.lib.collections.ExtList;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.shacache.ShaCache;
import aQute.remote.api.Envoy;
import aQute.remote.api.Supervisor;

public class Main extends ReporterAdapter implements Callable<Linkable<Envoy,Supervisor>> {

	private static ShaCache shacache;
	private CommandLine commandLine;
	private File storage;

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

		Dispatcher<Envoy, Supervisor> d = new Dispatcher<Envoy, Supervisor>(
				Supervisor.class,this, network, port);
		d.open();
		d.join();
	}

	@Override
	public Linkable<Envoy, Supervisor> call() throws Exception {
		return new EnvoyImpl(this,shacache,storage);
	}
	
	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run(args);
	}
	
}
