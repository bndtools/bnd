package aQute.bnd.metatype;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import aQute.bnd.osgi.WriteResource;
import aQute.lib.tag.Tag;

public class TagResource extends WriteResource {
	final Tag tag;

	public TagResource(Tag tag) {
		this.tag = tag;
	}

	@Override
	public void write(OutputStream out) throws UnsupportedEncodingException {
		OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
		PrintWriter pw = new PrintWriter(ow);
		pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
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
