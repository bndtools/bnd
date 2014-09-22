package aQute.lib.utf8properties;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import aQute.lib.io.*;

public class UTF8Properties extends Properties {
	private static final long	serialVersionUID	= 1L;
	private static Charset		UTF8				= Charset.forName("UTF-8");

	public UTF8Properties(Properties p) {
		super(p);
	}

	public UTF8Properties() {
	}

	@Override
	public void load(InputStream in) throws IOException {
		byte[] buffer = IO.read(in);
		try {
			super.load(new InputStreamReader(new ByteArrayInputStream(buffer), UTF8));
		}
		catch (IOException e) {
			// Try again with default encoding
			super.load(new ByteArrayInputStream(buffer));
		}
	}

	@Override
	public void store(OutputStream out, String msg) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(out, UTF8);
		super.store(osw, msg);
	}
}
