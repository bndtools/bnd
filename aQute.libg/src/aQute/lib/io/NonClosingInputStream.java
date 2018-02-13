package aQute.lib.io;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.Objects;

public class NonClosingInputStream extends FilterInputStream {
	public NonClosingInputStream(InputStream in) {
		super(Objects.requireNonNull(in));
	}

	@Override
	public void close() {
		// leave the underlying InputStream open
	}
}
