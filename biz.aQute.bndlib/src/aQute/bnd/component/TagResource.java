package aQute.bnd.component;

import java.io.*;

import aQute.bnd.osgi.*;
import aQute.lib.tag.*;

public class TagResource extends WriteResource {
	final Tag	tag;

	public TagResource(Tag tag) {
		this.tag = tag;
	}

	@Override
	public void write(OutputStream out) throws UnsupportedEncodingException {
		OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
		PrintWriter pw = new PrintWriter(ow);
		pw.println("<?xml version=\"1.0\"?>");
		try {
			tag.print(0, pw);
		}
		finally {
			pw.flush();
		}
	}

	@Override
	public long lastModified() {
		return 0;
	}

}
