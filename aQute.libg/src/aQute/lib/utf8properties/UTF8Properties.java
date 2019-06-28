package aQute.lib.utf8properties;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.io.IO;
import aQute.lib.io.NonClosingInputStream;
import aQute.lib.io.NonClosingReader;
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
	private static final long								serialVersionUID	= 1L;
	private static final List<ThreadLocal<CharsetDecoder>>	decoders			= Collections.unmodifiableList(
		Arrays.asList(ThreadLocal.withInitial(UTF_8::newDecoder), ThreadLocal.withInitial(ISO_8859_1::newDecoder)));

	public UTF8Properties(Properties p) {
		super(p);
	}

	public UTF8Properties(File file, String[] syntaxHeaders) throws Exception {
		this(file, null, syntaxHeaders);
	}

	public UTF8Properties(File file) throws Exception {
		this(file, null, null);
	}

	public UTF8Properties(File file, Reporter reporter, String[] syntaxHeaders) throws Exception {
		load(file, reporter, syntaxHeaders);
	}

	public UTF8Properties(File file, Reporter reporter) throws Exception {
		load(file, reporter, null);
	}

	public UTF8Properties() {}

	public void load(InputStream in, File file, Reporter reporter, String[] syntaxHeaders) throws IOException {
		String source = decode(IO.read(in));
		load(source, file, reporter, syntaxHeaders);
	}

	public void load(InputStream in, File file, Reporter reporter) throws IOException {
		load(in, file, reporter, null);
	}

	public void load(String source, File file, Reporter reporter) throws IOException {
		load(source, file, reporter, null);
	}

	public void load(String source, File file, Reporter reporter, String[] syntaxHeaders) throws IOException {
		PropertiesParser parser = new PropertiesParser(source, file == null ? null : file.getAbsolutePath(), reporter,
			this);
		if (syntaxHeaders != null)
			parser.setSyntaxHeaders(syntaxHeaders);

		parser.parse();
	}

	public void load(File file, Reporter reporter, String[] syntaxHeaders) throws Exception {
		String source = decode(IO.read(file));
		load(source, file, reporter, syntaxHeaders);
	}

	public void load(File file, Reporter reporter) throws Exception {
		this.load(file, reporter, null);
	}

	@Override
	public void load(InputStream in) throws IOException {
		load(new NonClosingInputStream(in), null, null, null);
	}

	@Override
	public void load(Reader r) throws IOException {
		String source = IO.collect(new NonClosingReader(r));
		load(source, null, null, null);
	}

	private String decode(byte[] buffer) {
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		CharBuffer cb = CharBuffer.allocate(buffer.length * 4);
		for (ThreadLocal<CharsetDecoder> tl : decoders) {
			CharsetDecoder decoder = tl.get();
			boolean success = !decoder.decode(bb, cb, true)
				.isError();
			if (success) {
				decoder.flush(cb);
			}
			decoder.reset();
			if (success) {
				return cb.flip()
					.toString();
			}
			bb.rewind();
			cb.clear();
		}
		return new String(buffer); // default decoding
	}

	@Override
	public void store(OutputStream out, String msg) throws IOException {
		StringWriter sw = new StringWriter();
		super.store(sw, null);

		String[] lines = sw.toString()
			.split("\n\r?");

		byte[] newline = "\n".getBytes(UTF_8);
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			out.write(line.getBytes(UTF_8));
			out.write(newline);
		}
	}

	@Override
	public void store(Writer out, String msg) throws IOException {
		StringWriter sw = new StringWriter();
		super.store(sw, null);

		String[] lines = sw.toString()
			.split("\n\r?");

		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			out.write(line);
			out.write('\n');
		}
	}

	public void store(File out) throws IOException {
		StringWriter sw = new StringWriter();
		super.store(sw, null);
		IO.store(sw.toString(), out);
	}

	public void store(OutputStream out) throws IOException {
		store(out, null);
	}

	/**
	 * Replace a string in all the values. This can be used to preassign
	 * variables that change. For example, the base directory ${.} for a loaded
	 * properties.
	 *
	 * @return A new UTF8Properties with the replacement.
	 */
	public UTF8Properties replaceAll(String pattern, String replacement) {
		return replaceAll(Pattern.compile(pattern), replacement);
	}

	private UTF8Properties replaceAll(Pattern regex, String replacement) {
		UTF8Properties result = new UTF8Properties(defaults);
		for (Map.Entry<Object, Object> entry : entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			value = regex.matcher(value)
				.replaceAll(replacement);
			result.put(key, value);
		}
		return result;
	}

	private static final Pattern HERE_PATTERN = Pattern.compile("${.}", Pattern.LITERAL);

	/**
	 * Replace the string "${.}" in all the values with the path of the
	 * specified file.
	 *
	 * @return A new UTF8Properties with the replacement.
	 */
	public UTF8Properties replaceHere(File file) {
		String here = (file == null) ? "." : IO.absolutePath(file);
		if (here.endsWith("/")) {
			here += ".";
		}
		return replaceAll(HERE_PATTERN, Matcher.quoteReplacement(here));
	}
}
