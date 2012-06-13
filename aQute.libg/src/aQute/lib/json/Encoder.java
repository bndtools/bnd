package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

public class Encoder implements Appendable, Closeable, Flushable {
	final JSONCodec	codec;
	Appendable		app;
	MessageDigest	digest;
	boolean			writeDefaults;
	String			encoding	= "UTF-8";

	Encoder(JSONCodec codec) {
		this.codec = codec;
	}

	public Encoder put(Object object) throws Exception {
		if (app == null)
			to();

		codec.encode(this, object, null, new IdentityHashMap<Object,Type>());
		return this;
	}

	public Encoder mark() throws NoSuchAlgorithmException {
		if (digest == null)
			digest = MessageDigest.getInstance("SHA1");
		digest.reset();
		return this;
	}

	public byte[] digest() throws NoSuchAlgorithmException, IOException {
		if (digest == null)
			return null;
		append('\n');
		return digest.digest();
	}

	public Encoder to() throws IOException {
		to(new StringWriter());
		return this;
	}

	public Encoder to(File file) throws IOException {
		return to(new FileOutputStream(file));
	}

	public Encoder charset(String encoding) {
		this.encoding = encoding;
		return this;
	}

	public Encoder to(OutputStream out) throws IOException {
		return to(new OutputStreamWriter(out, encoding));
	}

	public Encoder to(Appendable out) throws IOException {
		app = out;
		return this;
	}

	public Appendable append(char c) throws IOException {
		if (digest != null) {
			digest.update((byte) (c / 256));
			digest.update((byte) (c % 256));
		}
		app.append(c);
		return this;
	}

	public Appendable append(CharSequence sq) throws IOException {
		return append(sq, 0, sq.length());
	}

	public Appendable append(CharSequence sq, int start, int length) throws IOException {
		if (digest != null) {
			for (int i = start; i < length; i++) {
				char c = sq.charAt(i);
				digest.update((byte) (c / 256));
				digest.update((byte) (c % 256));
			}
		}
		app.append(sq, start, length);
		return this;
	}

	public String toString() {
		return app.toString();
	}

	public void close() throws IOException {
		if (app != null && app instanceof Closeable)
			((Closeable) app).close();
	}

	void encode(Object object, Type type, Map<Object,Type> visited) throws Exception {
		codec.encode(this, object, type, visited);
	}

	public Encoder writeDefaults() {
		writeDefaults = true;
		return this;
	}

	public void flush() throws IOException {
		if (app instanceof Flushable) {
			((Flushable) app).flush();
		}
	}
}
