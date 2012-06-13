package aQute.lib.collections;

import java.io.*;
import java.util.*;

public class LineCollection implements Iterator<String>, Closeable {
	final BufferedReader	reader;
	String					next;

	public LineCollection(InputStream in) throws IOException {
		this(new InputStreamReader(in, "UTF8"));
	}

	public LineCollection(File in) throws IOException {
		this(new InputStreamReader(new FileInputStream(in), "UTF-8"));
	}

	public LineCollection(Reader reader) throws IOException {
		this(new BufferedReader(reader));
	}

	public LineCollection(BufferedReader reader) throws IOException {
		this.reader = reader;
		next = reader.readLine();
	}

	public boolean hasNext() {
		return next != null;
	}

	public String next() {
		if (next == null)
			throw new IllegalStateException("Iterator has finished");
		try {
			String result = next;
			next = reader.readLine();
			if (next == null)
				reader.close();
			return result;
		}
		catch (Exception e) {
			// ignore
			return null;
		}
	}

	public void remove() {
		if (next == null)
			throw new UnsupportedOperationException("Cannot remove");
	}

	public void close() throws IOException {
		reader.close();
	}
}
