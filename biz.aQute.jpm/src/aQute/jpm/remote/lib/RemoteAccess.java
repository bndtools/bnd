package aQute.jpm.remote.lib;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.remote.*;

public class RemoteAccess extends Processor {
	public static int	DEFAULT_JPM_REMOTE_PORT	= 17281;

	List<String>		remainder;
	RemoteOptions		opts;
	File				root					= IO.getFile(IO.work, "remote");

	private PrintStream	out;

	interface CmdOptions extends Options {
		String host();

		String host(String deflt);

		String cwd(String path);

		String area();
	}

	public RemoteAccess(RemoteOptions opts, PrintStream out) {
		this.opts = opts;
		this.out = out;
	}

	public void _slave(SlaveOptions o) throws Exception {
		File cwd = new File(o.cwd(root.getAbsolutePath()));
		ServerSocket server = new ServerSocket(opts.port(DEFAULT_JPM_REMOTE_PORT));
		while (true) {
			Socket socket = server.accept();
			trace("New connection " + socket);
			SlaveImpl slave = new SlaveImpl(this, cwd, socket);
			slave.open();
		}
	}

	interface PingOptions extends CmdOptions {

	}

	public void _ping(PingOptions o) throws Exception {
		Master master = getMaster(o);

		System.out.println("Remote version is" + master.getSlave().getWelcome(Sink.version).version);

		master.close();
	}

	interface AreaOptions extends CmdOptions {}

	public void _list(AreaOptions o) throws Exception {
		Master master = getMaster(o);
		list(master);
		master.close();
	}

	public void _remove(AreaOptions o) throws Exception {
		Master master = getMaster(o);

		for (String name : o._()) {
			master.getSlave().removeArea(name);
		}

		list(master);
		master.close();
	}

	interface LaunchOptions extends CmdOptions {
		String[] env();

		boolean join();
	}

	public void _launch(LaunchOptions opts) throws Exception {
		MasterImpl master = getMaster(opts);
		List<String> args = opts._();
		String name = args.remove(0);
		HashMap<String,String> env = new HashMap<String,String>();
		if (opts.env() != null) {
			for (String var : opts.env()) {
				String[] parts = var.trim().split("\\s*,\\s*");
				if (parts.length == 1) {
					env.put(parts[0], "" + true);
				} else
					env.put(parts[0], parts[1]);
			}
		}
		master.launch(env, args, System.in, System.out, System.err);

		if (opts.join())
			master.join();

		master.close();
	}

	private void list(Master master) throws Exception {
		for (Area area : master.getSlave().getAreas()) {
			print(area);
		}
	}

	private void print(Area area) {
		out.printf("%-8s %-4s %s\n", area.id, area.running ? "run" : "", area.exitCode);
	}

	private MasterImpl getMaster(CmdOptions o) throws IOException {
		String host = o.host("localhost");
		int port = opts.port(DEFAULT_JPM_REMOTE_PORT);
		Socket socket = new Socket(host, port);
		File cwd = new File(o.cwd(IO.work.getAbsolutePath()));
		MasterImpl master = new MasterImpl(this, socket, cwd, o.area());
		master.open();
		return master;
	}
}
