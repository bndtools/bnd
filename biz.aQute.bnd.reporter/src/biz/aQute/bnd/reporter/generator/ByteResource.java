package biz.aQute.bnd.reporter.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import aQute.bnd.osgi.WriteResource;

public class ByteResource extends WriteResource {

	final ByteArrayOutputStream bytes;

	public ByteResource(final ByteArrayOutputStream bytes) {
		this.bytes = bytes;
	}

	@Override
	public void write(final OutputStream out) throws IOException {
		try {
			bytes.writeTo(out);
		} finally {
			out.flush();
		}
	}

	@Override
	public long lastModified() {
		return 0;
	}
}
