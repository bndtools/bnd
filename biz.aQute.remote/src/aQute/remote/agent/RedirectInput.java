package aQute.remote.agent;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class RedirectInput extends FilterInputStream {
	
	private PipedOutputStream out;
	private PipedInputStream in;
	private OutputStreamWriter writer;
	private InputStream org;

	public RedirectInput(InputStream in) throws IOException {
		super(new PipedInputStream());
		this.in = (PipedInputStream) super.in;
		this.out = new PipedOutputStream(this.in);
		this.writer = new OutputStreamWriter(out);
		this.org = in;
	}

	public InputStream getOrg() {
		return org;
	}

	public synchronized void add(String s) throws IOException {
		writer.write(s);
		writer.flush();
	}

}
