package aQute.bnd.metatype;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import aQute.bnd.osgi.WriteResource;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class TagResource extends WriteResource {
	final Tag tag;

	public TagResource(Tag tag) {
		this.tag = tag;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		PrintWriter pw = IO.writer(out, UTF_8);
		pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		try {
			tag.print(0, pw);
		} finally {
			pw.flush();
		}
	}

	@Override
	public long lastModified() {
		return 0;
	}

}
