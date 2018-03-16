package aQute.lib.collections;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import aQute.lib.io.IO;

public class LineCollection implements Iterator<String>, Closeable {
	private final BufferedReader	reader;
	private String					next;

	public LineCollection(InputStream in) throws IOException {
		this(new InputStreamReader(in, UTF_8));
	}

	public LineCollection(File in) throws IOException {
		this(IO.reader(in, UTF_8));
	}

	public LineCollection(Reader reader) throws IOException {
		this(new BufferedReader(reader));
	}

	public LineCollection(BufferedReader reader) throws IOException {
		this.reader = reader;
		next = reader.readLine();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public String next() {
		if (next == null)
			throw new NoSuchElementException("Iterator has finished");
		try {
			String result = next;
			next = reader.readLine();
			if (next == null)
				reader.close();
			return result;
		} catch (Exception e) {
			// ignore
			return null;
		}
	}

	@Override
	public void remove() {
		if (next == null)
			throw new UnsupportedOperationException("Cannot remove");
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
}
