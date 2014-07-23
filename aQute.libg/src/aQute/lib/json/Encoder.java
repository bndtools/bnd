package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

public class Encoder implements Appendable, Closeable, Flushable {
	final JSONCodec	codec;
	Appendable		app;
	MessageDigest	digest;
	boolean			writeDefaults;
	String			encoding	= "UTF-8";
	boolean			deflate;
	String			tabs		= null;
	String			indent		= "";
	boolean			keepOpen	= false;
	boolean			closed		= false;

	Encoder(JSONCodec codec) {
		this.codec = codec;
	}

	public Encoder put(Object object) throws Exception {
		if (app == null)
			to();

		codec.encode(this, object, null, new IdentityHashMap<Object,Type>());
		flush();
		if (!keepOpen)
			close();
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
		if (deflate)
			out = new DeflaterOutputStream(out);

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

	@Override
	public String toString() {
		return app.toString();
	}

	public void close() throws IOException {
		if (app != null && app instanceof Closeable) {
			((Closeable) app).close();
			closed = true;
		}
	}

	void encode(Object object, Type type, Map<Object,Type> visited) throws Exception {
		codec.encode(this, object, type, visited);
	}

	public Encoder writeDefaults() {
		writeDefaults = true;
		return this;
	}

	public void flush() throws IOException {
		if (closed)
			return;

		if (app instanceof Flushable) {
			((Flushable) app).flush();
		}
	}

	public Encoder deflate() {
		if (app != null)
			throw new IllegalStateException("Writer already set, deflate must come before to(...)");
		deflate = true;
		return this;
	}

	public Encoder indent(String tabs) {
		this.tabs = tabs;
		return this;
	}

	void undent() throws IOException {
		if (tabs != null) {
			app.append("\n");
			indent = indent.substring(tabs.length());
			app.append(indent);
		}
	}

	void indent() throws IOException {
		if (tabs != null) {
			app.append("\n");
			indent += tabs;
			app.append(indent);
		}
	}

	public Encoder keepOpen() {
		keepOpen = true;
		return this;
	}
}
