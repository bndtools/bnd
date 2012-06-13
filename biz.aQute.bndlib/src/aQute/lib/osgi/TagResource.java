package aQute.lib.osgi;

import java.io.*;

import aQute.lib.tag.*;

public class TagResource extends WriteResource {
	final Tag	tag;

	public TagResource(Tag tag) {
		this.tag = tag;
	}

	public void write(OutputStream out) throws UnsupportedEncodingException {
		OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
		PrintWriter pw = new PrintWriter(ow);
		pw.println("<?xml version='1.1'?>");
		try {
			tag.print(0, pw);
		}
		finally {
			pw.flush();
		}
	}

	public long lastModified() {
		return 0;
	}

}
