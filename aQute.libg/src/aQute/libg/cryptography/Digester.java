package aQute.libg.cryptography;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import aQute.lib.io.IO;

public abstract class Digester<T extends Digest> extends OutputStream {
	protected MessageDigest	md;
	OutputStream			out[];

	public Digester(MessageDigest instance, OutputStream... out) {
		md = instance;
		this.out = out;
	}

	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException {
		md.update(buffer, offset, length);
		for (OutputStream o : out) {
			o.write(buffer, offset, length);
		}
	}

	@Override
	public void write(int b) throws IOException {
		md.update((byte) b);
		for (OutputStream o : out) {
			o.write(b);
		}
	}

	public MessageDigest getMessageDigest() throws Exception {
		return md;
	}

	public T from(InputStream in) throws Exception {
		IO.copy(in, this);
		return digest();
	}

	public void setOutputs(OutputStream... out) {
		this.out = out;
	}

	public abstract T digest() throws Exception;

	public abstract T digest(byte[] bytes) throws Exception;

	public abstract String getAlgorithm();

	public T from(File f) throws Exception {
		IO.copy(f, this);
		return digest();
	}

	public T from(byte[] f) throws Exception {
		IO.copy(f, this);
		return digest();
	}
}
