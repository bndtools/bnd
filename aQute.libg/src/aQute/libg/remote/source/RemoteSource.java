package aQute.libg.remote.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.libg.remote.Area;
import aQute.libg.remote.Event;
import aQute.libg.remote.Sink;
import aQute.libg.remote.Source;
import aQute.libg.remote.Welcome;

/**
 * Controls a different file system trough a Sink. It can translate files with
 * local absolute file paths to remote absolute file paths (also if it is a
 * windows system). It also detects changes in the local file system and will
 * update the remote. Updates are checked for SHAs so we only transfer the files
 * when they really are not there, even allowing a remote SHA cache to do its
 * magic.
 */
public class RemoteSource implements Source {

	private Sink			sink;
	private Appendable		stdout;
	private Appendable		stderr;
	private Thread			thread;
	volatile AtomicBoolean	running	= new AtomicBoolean();
	Welcome					welcome;
	SourceFS				fsync;
	String					areaId;
	File					cwd;

	public void open(Sink sink, File cwd, String areaId) {
		this.sink = sink;
		this.cwd = cwd;
		this.areaId = areaId;
		this.welcome = sink.getWelcome(Sink.version);
		this.fsync = new SourceFS(welcome.separatorChar, cwd, sink, areaId);
	}

	/**
	 * Check for all files in our scope if they have changed or been referred to
	 * recently. Remote files will be deleted or updated when necessary.
	 */

	/**
	 * Called from the remote sink to get the data when it lacks the given sha.
	 */
	@Override
	public byte[] getData(String sha) throws Exception {
		return fsync.getData(sha);
	}

	/**
	 * Close
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {}

	@Override
	public void event(Event e, Area area) throws Exception {
		switch (e) {

			case created :
				break;
			case deleted :
				break;
			case exited :
				exit();
				break;
			case launching :
				break;
			case restarted :
				break;
			case running :
				break;
			case started :
				break;
			case virginal :
				break;
			default :
				break;

		}
	}

	private void exit() {
		if (running.getAndSet(false)) {
			this.thread.interrupt();
			this.stdout = null;
			this.stderr = null;
		} else
			;// TODO log
	}

	@Override
	public void output(String areaId, CharSequence text, boolean err) throws IOException {
		if (running.get()) {
			if (err)
				this.stderr.append(text);
			else
				this.stdout.append(text);
		}
	}

	public Sink getSink() {
		return sink;
	}

	public void launch(Map<String, String> env, List<String> args, final InputStream stdin, Appendable stdout,
		Appendable stderr) throws Exception {
		if (!running.getAndSet(true)) {
			for (int i = 0; i < args.size(); i++) {
				args.set(i, fsync.transform(args.get(i)));
			}
			for (Map.Entry<String, String> e : env.entrySet()) {
				e.setValue(fsync.transform(e.getValue()));
			}

			fsync.sync();

			this.stdout = stdout;
			this.stderr = stderr;
			this.thread = new Thread("source::" + areaId) {
				@Override
				public void run() {
					byte[] data = new byte[10000];
					while (!Thread.currentThread()
						.isInterrupted() && running.get())
						try {
							int length = stdin.read(data);
							if (length < 0)
								cancel();
							else {
								if (length > 0) {
									getSink().input(areaId, new String(data));
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			};
			if (sink.launch(areaId, env, args)) {
				this.thread.start();
			} else {
				exit();
			}
		} else
			throw new IllegalStateException("Already running " + areaId);
	}

	public void cancel() throws Exception {
		getSink().cancel(areaId);
	}

	public void update(File f) throws Exception {
		fsync.markTransform(f);
	}

	public void sync() throws Exception {
		fsync.sync();
	}

	public void add(File file) throws Exception {
		fsync.add(file);
	}

	public void join() throws InterruptedException {
		while (running.get()) {
			Thread.sleep(500);
		}
	}
}
