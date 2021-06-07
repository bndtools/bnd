package aQute.lib.redirect;

import java.io.IOException;
import java.io.OutputStream;

import aQute.lib.io.IO;

public class Tee extends OutputStream {

	final OutputStream[]	out;
	final Thread			thread;

	public Tee(OutputStream... out) {
		this(null, out);
	}

	public Tee(Thread thread, OutputStream... out) {
		this.thread = thread;
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		if (thread == null || Thread.currentThread() == thread)
			for (OutputStream o : out) {
				o.write(b);
			}
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (thread == null || Thread.currentThread() == thread)
			for (OutputStream o : out) {
				o.write(b);
			}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (thread == null || Thread.currentThread() == thread)
			for (OutputStream o : out) {
				o.write(b, off, len);
			}
	}

	@Override
	public void close() throws IOException {
		IO.closeAll((Object[]) out);
	}

}
