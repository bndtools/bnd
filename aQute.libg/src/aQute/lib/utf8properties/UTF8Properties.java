package aQute.lib.utf8properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Properties;

import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

/**
 * Properties were by default read as ISO-8859-1 characters. However, in the
 * last 10 years most builds use UTF-8. Since this is in general a global
 * setting, it is very awkward to use ISO-8859-1. In general, it is not often a
 * problem since most of Java is written with the basic ASCII encoding. However,
 * we want to do this right. So in bnd we generally use this UTF-8 Properties
 * class. This class always writes UTF-8. However, it will try to read UTF-8
 * first. If this fails, it will try ISO-8859-1, and the last attempt is the
 * platform default.
 * <p>
 * This class can (and probably should) be used anywhere a Properties class is
 * used.
 */
public class UTF8Properties extends Properties {
	private static final long	serialVersionUID	= 1L;
	private static Charset		UTF8				= Charset.forName("UTF-8");
	private static Charset		ISO8859_1			= Charset.forName("ISO8859-1");

	public UTF8Properties(Properties p) {
		super(p);
	}

	public UTF8Properties() {}

	public void load(InputStream in, File file, Reporter reporter) throws IOException {
		String source = read(in);
		load(source, file, reporter);
	}

	public void load(String source, File file, Reporter reporter) throws IOException {
		PropertiesParser parser = new PropertiesParser(source, file == null ? null : file.getAbsolutePath(), reporter,
				this);
		parser.parse();
	}

	public void load(File file, Reporter reporter) throws Exception {
		FileInputStream fin = new FileInputStream(file);
		try {
			load(fin, file, reporter);
		}
		finally {
			fin.close();
		}
	}

	@Override
	public void load(InputStream in) throws IOException {
		load(in, null, null);
	}

	@Override
	public void load(Reader r) throws IOException {
		String s = IO.collect(r);
		PropertiesParser parser = new PropertiesParser(s, null, null, this);
		parser.parse();
	}

	String read(InputStream in) throws IOException {

		byte[] buffer = IO.read(in);
		try {
			try {
				return convert(buffer, UTF8);
			}
			catch (CharacterCodingException e) {
				// Ok, not good, fallback to old encoding
			}

			try {
				return convert(buffer, ISO8859_1);
			}
			catch (CharacterCodingException e) {
				// Ok, not good, fallback to platform encoding
			}

			return new String(buffer);
		}
		finally {
			// System.out.println("UTF8Props: " + this);
		}
	}

	private String convert(byte[] buffer, Charset charset) throws IOException {
		CharBuffer decode = charset.decode(ByteBuffer.wrap(buffer));
		CharsetDecoder decoder = charset.newDecoder();
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		CharBuffer cb = CharBuffer.allocate(buffer.length * 4);
		CoderResult result = decoder.decode(bb, cb, true);
		if (!result.isError()) {
			return new String(cb.array(), 0, cb.position());
		}
		throw new CharacterCodingException();
	}

	@Override
	public void store(OutputStream out, String msg) throws IOException {
		StringWriter sw = new StringWriter();
		super.store(sw, null);

		String[] lines = sw.toString().split("\n\r?");

		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			out.write(line.getBytes(UTF8));
			out.write("\n".getBytes(UTF8));
		}
	}

	@Override
	public void store(Writer out, String msg) throws IOException {
		StringWriter sw = new StringWriter();
		super.store(sw, null);

		String[] lines = sw.toString().split("\n\r?");

		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			out.write(line);
			out.write("\n");
		}
	}

	public void store(OutputStream out) throws IOException {
		store(out, null);
	}

}
