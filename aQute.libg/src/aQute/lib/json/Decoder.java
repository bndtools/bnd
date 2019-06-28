package aQute.lib.json;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;

public class Decoder implements Closeable {
	final JSONCodec		codec;
	Reader				reader;
	int					current;
	MessageDigest		digest;
	Map<String, Object>	extra;
	Charset				encoding	= UTF_8;

	boolean				strict;
	boolean				inflate;
	boolean				keepOpen	= false;

	Decoder(JSONCodec codec) {
		this.codec = codec;
	}

	public Decoder from(File file) throws Exception {
		return from(IO.stream(file));
	}

	public Decoder from(InputStream in) throws Exception {

		if (inflate)
			in = new InflaterInputStream(in);

		return from(new InputStreamReader(in, encoding));
	}

	public Decoder from(byte[] data) throws Exception {
		return from(new ByteArrayInputStream(data));
	}

	public Decoder charset(String encoding) {
		return charset(Charset.forName(encoding));
	}

	public Decoder charset(Charset encoding) {
		this.encoding = encoding;
		return this;
	}

	public Decoder strict() {
		this.strict = true;
		return this;
	}

	public Decoder from(Reader in) throws Exception {
		reader = in;
		read();
		return this;
	}

	public Decoder faq(String in) throws Exception {
		return from(in.replace('\'', '"'));
	}

	public Decoder from(String in) throws Exception {
		return from(new StringReader(in));
	}

	public Decoder mark() throws NoSuchAlgorithmException {
		if (digest == null)
			digest = MessageDigest.getInstance("SHA1");
		digest.reset();
		return this;
	}

	public byte[] digest() {
		if (digest == null)
			return null;

		return digest.digest();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz) throws Exception {
		try {
			return (T) codec.decode(clazz, this);
		} finally {
			if (!keepOpen)
				close();
		}
	}

	public Object get(Type type) throws Exception {
		try {
			return codec.decode(type, this);
		} finally {
			if (!keepOpen)
				close();
		}
	}

	public Object get() throws Exception {
		try {
			return codec.decode(null, this);
		} finally {
			if (!keepOpen)
				close();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T get(TypeReference<T> ref) throws Exception {
		try {
			return (T) codec.decode(ref.getType(), this);
		} finally {
			if (!keepOpen)
				close();
		}
	}

	public Decoder keepOpen() {
		keepOpen = true;
		return this;
	}

	int read() throws Exception {
		current = reader.read();
		if (digest != null) {
			digest.update((byte) (current / 256));
			digest.update((byte) (current % 256));
		}
		return current;
	}

	int current() {
		return current;
	}

	/**
	 * Skip any whitespace.
	 *
	 * @throws Exception
	 */
	int skipWs() throws Exception {
		while (Character.isWhitespace(current()))
			read();
		return current();
	}

	/**
	 * Skip any whitespace.
	 *
	 * @throws Exception
	 */
	int next() throws Exception {
		read();
		return skipWs();
	}

	void expect(String s) throws Exception {
		for (int i = 0; i < s.length(); i++)
			if (!(s.charAt(i) == read()))
				throw new IllegalArgumentException("Expected " + s + " but got something different");
		read();
	}

	public boolean isEof() throws Exception {
		int c = skipWs();
		return c < 0;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	public Map<String, Object> getExtra() {
		if (extra == null)
			extra = new HashMap<>();
		return extra;
	}

	public Decoder inflate() {
		if (reader != null)
			throw new IllegalStateException("Reader already set, inflate must come before from()");
		inflate = true;
		return this;
	}
}
