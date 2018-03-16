package aQute.libg.remote.sink;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.command.Command;
import aQute.libg.remote.Area;
import aQute.libg.remote.Delta;
import aQute.libg.remote.Event;
import aQute.libg.remote.Sink;
import aQute.libg.remote.Source;
import aQute.libg.remote.Welcome;

public class RemoteSink implements Sink {
	final static JSONCodec		codec	= new JSONCodec();
	final File					root;
	Source[]					sources;
	final Map<String, AreaImpl>	areas	= new ConcurrentHashMap<>();

	final File					areasDir;
	final SinkFS				sinkfs;
	private File				shacache;

	public RemoteSink(File root, Source... s) throws Exception {
		this.root = root;
		areasDir = new File(root, "areas");
		IO.mkdirs(areasDir);

		for (File areaDir : areasDir.listFiles()) {
			areas.put(areaDir.getName(), read(areaDir));
		}

		this.sources = s;

		shacache = new File(root, "shacache");
		IO.mkdirs(shacache);
		sinkfs = new SinkFS(s, shacache);
	}

	@Override
	public AreaImpl getArea(String areaId) throws Exception {
		AreaImpl area = areas.get(areaId);
		if (area != null)
			return area;

		File af = new File(areasDir, areaId);
		IO.mkdirs(af);
		return read(af);
	}

	@Override
	public boolean removeArea(String areaId) throws Exception {
		AreaImpl area = areas.remove(areaId);
		if (area != null) {
			IO.delete(area.root);
			return true;
		}
		return false;
	}

	@Override
	public boolean launch(String areaId, Map<String, String> env, List<String> args) throws Exception {
		final AreaImpl area = getArea(areaId);
		if (area == null)
			throw new IllegalArgumentException("No such area");

		if (area.running)
			throw new IllegalStateException("Already running");

		area.command = new Command();
		area.command.addAll(args);
		area.command.setCwd(area.cwd);
		if (env != null) {
			for (Map.Entry<String, String> e : env.entrySet()) {
				area.command.var(e.getKey(), e.getValue());
			}
		}

		area.line = area.command.toString();

		PipedInputStream pin = new PipedInputStream();
		@SuppressWarnings("resource")
		PipedOutputStream pout = new PipedOutputStream();
		pout.connect(pin);
		area.toStdin = pout;
		area.stdin = pin;

		area.stdout = new Appender(sources, area.id, false);
		area.stderr = new Appender(sources, area.id, true);

		area.thread = new Thread(areaId + "::" + args) {
			@Override
			public void run() {
				try {
					event(Event.launching, area);
					area.running = true;
					area.command.setCwd(area.cwd);
					area.command.setUseThreadForInput(true);
					area.exitCode = area.command.execute(area.stdin, area.stdout, area.stderr);
				} catch (Throwable e) {
					area.exitCode = -1;
					area.exception = e.toString();
				} finally {
					area.running = false;
					area.toStdin = null;
					area.stderr = null;
					area.stdout = null;
					area.stdin = null;
					area.command = null;
					event(Event.exited, area);
				}
			}

		};
		area.thread.start();
		return true;
	}

	@Override
	public void cancel(String areaId) throws Exception {
		final AreaImpl area = getArea(areaId);
		if (area == null)
			throw new IllegalArgumentException("No such area");

		if (!area.running)
			throw new IllegalStateException("Not running");

		area.canceled = true;
		area.command.cancel();
	}

	@Override
	public void input(String areaId, String text) throws Exception {
		AreaImpl area = getArea(areaId);

		OutputStream input = area.toStdin;
		if (input != null) {
			input.write(text.getBytes());
		} else
			throw new IllegalStateException("Area " + areaId + " is not running");
	}

	@Override
	public int exit(String areaId) throws Exception {
		AreaImpl area = getArea(areaId);

		Command c = area.command;
		if (!area.running || c == null)
			throw new IllegalStateException("Area " + areaId + " is not running");

		c.cancel();

		area.thread.join(10000);

		return area.exitCode;
	}

	@Override
	public byte[] view(String areaId, String path) throws Exception {
		AreaImpl area = getArea(areaId);
		File f = new File(area.cwd, path);

		if (f.isDirectory()) {
			StringBuilder sb = new StringBuilder();
			for (String s : f.list()) {
				sb.append(s)
					.append("\n");
			}
			return sb.toString()
				.getBytes(UTF_8);
		} else if (f.isFile()) {
			return IO.read(f);
		}
		return null;
	}

	@Override
	public void exit() throws Exception {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	@Override
	public Welcome getWelcome(int highest) {
		Welcome welcome = new Welcome();
		welcome.separatorChar = File.separatorChar;
		welcome.properties = (Map) System.getProperties();
		welcome.version = Math.min(highest, Sink.version);
		return welcome;
	}

	@Override
	public AreaImpl createArea(String areaId) throws Exception {
		AreaImpl area = new AreaImpl();
		if (areaId == null) {
			int n = 1000;
			while (!new File(areasDir, "" + n).isDirectory())
				n++;

			areaId = "" + n;
		}
		File dir = new File(areasDir, areaId);
		IO.mkdirs(dir);

		return read(dir);
	}

	@Override
	public Collection<? extends Area> getAreas() {
		return areas.values();
	}

	protected AreaImpl read(File areaDir) throws Exception {
		AreaImpl area = new AreaImpl();
		area.id = areaDir.getName();
		area.root = areaDir;
		area.running = false;
		area.cwd = new File(area.root, "cwd");
		IO.mkdirs(area.cwd);
		areas.put(area.id, area);
		return area;
	}

	public void setSources(Source... sources) {
		this.sources = sources;
		sinkfs.setSources(sources);
	}

	void event(Event e, AreaImpl area) {
		for (Source source : sources) {
			try {
				source.event(e, area);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public boolean sync(String areaId, Collection<Delta> deltas) throws Exception {
		AreaImpl area = getArea(areaId);
		return sinkfs.delta(area.cwd, deltas);
	}

	@Override
	public boolean clearCache() {
		try {
			IO.deleteWithException(shacache);
			IO.mkdirs(shacache);
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
