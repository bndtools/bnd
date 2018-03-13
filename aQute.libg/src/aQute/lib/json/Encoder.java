package aQute.lib.json;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import aQute.lib.io.IO;

public class Encoder implements Appendable, Closeable, Flushable {
	final JSONCodec	codec;
	Appendable		app;
	MessageDigest	digest;
	boolean			writeDefaults;
	Charset			encoding	= UTF_8;
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

		codec.encode(this, object, null, new IdentityHashMap<>());
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
		return to(IO.outputStream(file));
	}

	public Encoder charset(String encoding) {
		return charset(Charset.forName(encoding));
	}

	public Encoder charset(Charset encoding) {
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

	@Override
	public Appendable append(char c) throws IOException {
		if (digest != null) {
			digest.update((byte) (c / 256));
			digest.update((byte) (c % 256));
		}
		app.append(c);
		return this;
	}

	@Override
	public Appendable append(CharSequence sq) throws IOException {
		return append(sq, 0, sq.length());
	}

	@Override
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

	@Override
	public void close() throws IOException {
		if (app != null && app instanceof Closeable) {
			((Closeable) app).close();
			closed = true;
		}
	}

	void encode(Object object, Type type, Map<Object, Type> visited) throws Exception {
		codec.encode(this, object, type, visited);
	}

	public Encoder writeDefaults() {
		writeDefaults = true;
		return this;
	}

	@Override
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
