package aQute.lib.codec;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import aQute.lib.io.IO;

public class HCodec implements Codec {
	final Codec codec;

	public HCodec(Codec codec) {
		this.codec = codec;
	}

	@Override
	public Object decode(Reader in, Type type) throws Exception {
		return codec.decode(in, type);
	}

	public <T> T decode(InputStream in, Class<T> t) throws Exception {
		return t.cast(codec.decode(IO.reader(in), t));
	}

	public <T> T decode(Reader in, Class<T> t) throws Exception {
		return t.cast(codec.decode(in, t));
	}

	public Object decode(InputStream in, Type t) throws Exception {
		return codec.decode(IO.reader(in), t);
	}

	@Override
	public void encode(Type t, Object o, Appendable out) throws Exception {
		codec.encode(t, o, out);
	}

	public void encode(Type t, Object o, OutputStream out) throws Exception {
		Writer wr = IO.writer(out);
		try {
			codec.encode(t, o, wr);
		} finally {
			wr.flush();
		}
	}

	public <T> T decode(File in, Class<T> t) throws Exception {
		try (Reader fin = IO.reader(in)) {
			return decode(fin, t);
		}
	}

	public void encode(Type t, Object o, File out) throws Exception {
		try (Writer wr = IO.writer(out)) {
			codec.encode(t, o, wr);
			wr.flush();
		}
	}

}
