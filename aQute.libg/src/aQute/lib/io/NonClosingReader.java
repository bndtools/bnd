package aQute.lib.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class NonClosingReader extends FilterReader {
	public NonClosingReader(Reader in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		// leave the underlying Reader open
	}
}
