package aQute.lib.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NonClosingInputStream extends FilterInputStream {
	public NonClosingInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		// leave the underlying InputStream open
	}
}
