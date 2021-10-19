package aQute.lib.redirect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import aQute.lib.io.ByteBufferOutputStream;

public class Capture extends OutputStream {
	private final ByteBufferOutputStream bout = new ByteBufferOutputStream();

	@Override
	public void write(int b) throws IOException {
		bout.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		bout.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		bout.write(b, off, len);
	}

	@Override
	public String toString() {
		return new String(bout.toByteArray(), StandardCharsets.UTF_8);
	}
}
