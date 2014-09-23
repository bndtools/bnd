package aQute.lib.utf8properties;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import aQute.lib.io.*;

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
 * An additional problem is backslash encoding. When reading properties, the
 * Properties class skips backslashes that have no correct next character. This
 * is not a real problem if it was not for bndtools where it turned out to be
 * real tricky to get the UI to understand this since text widgets read from the
 * properties (backslash removed) while the main text is just, well, the main
 * text and has the backslash present. Since we strife to fidelity, we actually
 * fixup the
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

	@Override
	public void load(InputStream in) throws IOException {

		byte[] buffer = IO.read(in);
		try {
			try {
				convert(buffer, UTF8);
				return;
			}
			catch (CharacterCodingException e) {
				// Ok, not good, fallback to old encoding
			}

			try {
				convert(buffer, ISO8859_1);
				return;
			}
			catch (CharacterCodingException e) {
				// Ok, not good, fallback to platform encoding
			}

			super.load(new ByteArrayInputStream(buffer));
		}
		finally {
			//System.out.println("UTF8Props: " + this);
		}
	}

	private void convert(byte[] buffer, Charset charset) throws IOException {
		CharBuffer decode = charset.decode(ByteBuffer.wrap(buffer));
		CharsetDecoder decoder = charset.newDecoder();
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		CharBuffer cb = CharBuffer.allocate(buffer.length * 4);
		CoderResult result = decoder.decode(bb, cb, true);
		if (!result.isError()) {
			
			String s = new String(cb.array(), 0, cb.position());
			s = doBackslashEncoding(s);

			super.load(new StringReader(s));
			return;
		}
		throw new CharacterCodingException();
	}

	private String doBackslashEncoding(String s) {
//		if (s.indexOf('\\') >= 0) {
//			s = s.replaceAll("\\\\(?!u|\\r|\\n|n|r|t|\\\\)", "\\\\\\\\");
//		}
		return s;
	}

	@Override
	public void load(Reader r) throws IOException {
		String s = doBackslashEncoding(IO.collect(r));
		super.load(new StringReader(s));
	}

	@Override
	public void store(OutputStream out, String msg) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(out, UTF8);
		super.store(osw, msg);
	}
}
