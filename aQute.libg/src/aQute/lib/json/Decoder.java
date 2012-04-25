package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;

public class Decoder implements Closeable {
	final JSONCodec	codec;
	Reader			reader;
	int				current;
	MessageDigest	digest;

	Decoder(JSONCodec codec) {
		this.codec = codec;
	}

	public Decoder from(File file) throws Exception {
		return from(new FileReader(file));
	}

	public Decoder from(InputStream in) throws Exception {
		return from(new InputStreamReader(in));

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

	@SuppressWarnings("unchecked") public <T> T get(Class<T> clazz) throws Exception {
		return (T) codec.decode(clazz, this);
	}

	public Object get(Type type) throws Exception {
		return codec.decode(type, this);
	}

	public Object get() throws Exception {
		return codec.decode(null, this);
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
	 * @return
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
	 * @return
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

	public void close() throws IOException {
		reader.close();
	}

}
