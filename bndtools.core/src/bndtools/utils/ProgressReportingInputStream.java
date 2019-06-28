package bndtools.utils;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

public class ProgressReportingInputStream extends InputStream {

	private final InputStream		stream;
	private final IProgressMonitor	monitor;

	/**
	 * Wrap an existing input stream with a progress-reporting input stream.
	 * Subsequently reading data from the wrapped stream will cause progress to
	 * be shown on the supplied progress monitor. NB: It is the caller's
	 * responsibility to call the {@code beginTask()} method of the progress
	 * monitor BEFORE any of the stream methods (e.g. {@code #read()} etc) are
	 * accessed.
	 *
	 * @param stream
	 * @param monitor
	 */
	public ProgressReportingInputStream(InputStream stream, IProgressMonitor monitor) {
		this.stream = stream;
		this.monitor = monitor;
	}

	@Override
	public int read() throws IOException {
		int result = stream.read();
		if (result > 0)
			monitor.worked(1);
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int result = stream.read(b);
		if (result > 0)
			monitor.worked(result);
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = stream.read(b, off, len);
		if (result > 0)
			monitor.worked(result);
		return result;
	}

	@Override
	public long skip(long n) throws IOException {
		long result = stream.skip(n);
		if (result > 0)
			monitor.worked((int) result);
		return result;
	}

	@Override
	public int available() throws IOException {
		return stream.available();
	}

	@Override
	public void close() throws IOException {
		stream.close();
		monitor.done();
	}

	@Override
	public synchronized void mark(int readlimit) {
		stream.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		stream.reset();
	}

	@Override
	public boolean markSupported() {
		return stream.markSupported();
	}

}
