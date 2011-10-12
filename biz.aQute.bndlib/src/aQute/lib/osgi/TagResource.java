package aQute.lib.osgi;

import java.io.*;

import aQute.lib.io.*;
import aQute.lib.tag.*;

public class TagResource implements Resource {
	final Tag	tag;
	String		extra;

	public TagResource(Tag tag) {
		this.tag = tag;
	}

	public InputStream openInputStream() throws Exception {
		final PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream(pin);
		Processor.getExecutor().execute(new Runnable() {
			public void run() {
				try {
					write(pout);
				} catch (Exception e) {
					e.printStackTrace();
					// ignore
				}
				IO.close(pout);
			}
		});
		return pin;
	}

	public void write(OutputStream out) throws UnsupportedEncodingException {
		OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
		PrintWriter pw = new PrintWriter(ow);
		pw.println("<?xml version='1.1'?>");
		try {
			tag.print(0, pw);
		} finally {
			pw.flush();
		}
	}

	public long lastModified() {
		return 0;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getExtra() {
		return extra;
	}

}
