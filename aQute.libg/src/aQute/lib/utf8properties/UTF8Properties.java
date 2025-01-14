package aQute.lib.utf8properties;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
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

	record Provenance(String source) {}

	private final Map<String, Provenance>	provenance	= new HashMap<>();

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
		load(file, reporter, fromArray(syntaxHeaders));
	}

	public UTF8Properties(File file, Reporter reporter) throws Exception {
		load(file, reporter, (Collection<String>) null);
	}

	public UTF8Properties() {
	}

	private static Collection<String> fromArray(String[] array) {
		return (array != null) ? Arrays.asList(array) : null;
	}

	public void load(InputStream in, File file, Reporter reporter, String[] syntaxHeaders) throws IOException {
		load(in, file, reporter, fromArray(syntaxHeaders));
	}

	public void load(InputStream in, File file, Reporter reporter, Collection<String> syntaxHeaders)
		throws IOException {
		String source = decode(IO.read(in));
		load(source, file, reporter, syntaxHeaders);
	}

	public void load(InputStream in, File file, Reporter reporter) throws IOException {
		load(in, file, reporter, (Collection<String>) null);
	}

	public void load(String source, File file, Reporter reporter) throws IOException {
		load(source, file, reporter, (Collection<String>) null);
	}

	public void load(String source, File file, Reporter reporter, String[] syntaxHeaders) throws IOException {
		load(source, file, reporter, fromArray(syntaxHeaders));
	}

	public void load(String source, File file, Reporter reporter, Collection<String> syntaxHeaders) throws IOException {
		load(source, file, reporter, syntaxHeaders, file == null ? "" : file.getAbsolutePath());
	}

	public void load(String source, File file, Reporter reporter, Collection<String> syntaxHeaders, String provenance)
		throws IOException {
		PropertiesParser parser = new PropertiesParser(source, file == null ? null : file.getAbsolutePath(), reporter,
			this, syntaxHeaders, provenance);
		parser.parse();
	}

	public void load(File file, Reporter reporter, String[] syntaxHeaders) throws Exception {
		load(file, reporter, fromArray(syntaxHeaders));
	}

	public void load(File file, Reporter reporter, Collection<String> syntaxHeaders) throws Exception {
		String source = decode(IO.read(file));
		load(source, file, reporter, syntaxHeaders);
	}

	public void load(File file, Reporter reporter) throws Exception {
		this.load(file, reporter, (Collection<String>) null);
	}

	@Override
	public synchronized void load(InputStream in) throws IOException {
		load(new NonClosingInputStream(in), null, null, (Collection<String>) null);
	}

	@Override
	public synchronized void load(Reader r) throws IOException {
		String source = IO.collect(new NonClosingReader(r));
		load(source, null, null, (Collection<String>) null);
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

	private static final Pattern LINE_SPLITTER = Pattern.compile("\n\r?");

	@Override
	public void store(Writer writer, String msg) throws IOException {
		CharArrayWriter sw = new CharArrayWriter();
		super.store(sw, null);

		String[] lines = LINE_SPLITTER.split(sw.toString());

		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			writer.write(line);
			writer.write('\n');
		}
	}

	@Override
	public void store(OutputStream out, String msg) throws IOException {
		Writer writer = new OutputStreamWriter(out, UTF_8);
		try {
			store(writer, msg);
		} finally {
			writer.flush();
		}
	}

	public void store(File file) throws IOException {
		try (OutputStream out = IO.outputStream(file)) {
			store(out, null);
		}
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
			result.setProperty(key, value, getProvenance(key).orElse(null));
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

	public synchronized Object setProperty(String key, String value, String provenance) {
		if (provenance != null)
			getProvenance().put(key, new Provenance(provenance));
		return super.setProperty(key, value);
	}

	@Override
	public synchronized Object remove(Object key) {
		getProvenance().remove(key);
		return super.remove(key);
	}

	/**
	 * Get the provenance of the given key if set
	 *
	 * @param key the key
	 */

	public Optional<String> getProvenance(String key) {
		Provenance provenance = getProvenance().get(key);
		return Optional.ofNullable(provenance)
			.map(Provenance::source);
	}

	/**
	 * Set the provenance of the given key
	 *
	 * @param key the key
	 * @param provenance the provenance, maybe null to remove
	 */

	public UTF8Properties setProvenance(String key, String provenance) {
		if (provenance == null)
			getProvenance().remove(key);
		else
			getProvenance().put(key, new Provenance(provenance));
		return this;
	}

	/**
	 * Load the properties from a properties. If the properties is a
	 * UTF8Properties, we also copy the provenance.
	 *
	 * @param properties the properties
	 * @param overwriteIfPresent overwrite an exissting value in this properties
	 */
	public void load(Properties properties, boolean overwriteIfPresent) {
		BiConsumer<String, String> set = properties instanceof UTF8Properties p
			? (k, v) -> setProperty(k, v, p.getProvenance(k)
				.orElse(null))
			: (k, v) -> setProperty(k, v);

		properties.forEach((k, v) -> {
			String key = (String) k;
			String value = (String) v;
			if (overwriteIfPresent || !contains(key))
				set.accept(key, value);
		});
	}

	Map<String, Provenance> getProvenance() {
		return provenance;
	}

	@Override
	public synchronized void putAll(Map<?, ?> t) {
		if (t instanceof Properties p) {
			load(p, true);
		} else
			super.putAll(t);
	}

	/**
	 * Set the provenance on all current keys
	 *
	 * @param provenance
	 */
	public void setProvenance(String provenance) {
		keySet().forEach(k -> setProvenance((String) k, provenance));
	}
}
