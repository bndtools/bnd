package aQute.lib.codec;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import aQute.lib.io.IO;

public class HCodec implements Codec {
	final Codec codec;

	public HCodec(Codec codec) {
		this.codec = codec;
	}

	public Object decode(Reader in, Type type) throws Exception {
		return codec.decode(in, type);
	}

	public <T> T decode(InputStream in, Class<T> t) throws Exception {
		return t.cast(decode(in, (Type) t));
	}

	public <T> T decode(Reader in, Class<T> t) throws Exception {
		return t.cast(decode(in, (Type) t));
	}

	public Object decode(InputStream in, Type t) throws Exception {
		InputStreamReader r = new InputStreamReader(in, UTF_8);
		return codec.decode(r, t);
	}

	public void encode(Type t, Object o, Appendable out) throws Exception {
		codec.encode(t, o, out);
	}

	public void encode(Type t, Object o, OutputStream out) throws Exception {
		OutputStreamWriter wr = new OutputStreamWriter(out, UTF_8);
		try {
			codec.encode(t, o, wr);
		} finally {
			wr.flush();
		}
	}

	public <T> T decode(File in, Class<T> t) throws Exception {
		try (InputStream fin = IO.stream(in);
				InputStreamReader rdr = new InputStreamReader(fin, UTF_8)) {
			return t.cast(decode(rdr, t));
		}

	}

	public void encode(Type t, Object o, File out) throws Exception {
		try (OutputStream oout = IO.outputStream(out); Writer wr = new OutputStreamWriter(oout, UTF_8)) {
			codec.encode(t, o, wr);
		}
	}

}
