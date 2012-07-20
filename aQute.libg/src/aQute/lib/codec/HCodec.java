package aQute.lib.codec;

import java.io.*;
import java.lang.reflect.*;

public class HCodec implements Codec {
	final Codec	codec;

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
		InputStreamReader r = new InputStreamReader(in, "UTF-8");
		return codec.decode(r, t);
	}

	public void encode(Type t, Object o, Appendable out) throws Exception {
		codec.encode(t, o, out);
	}

	public void encode(Type t, Object o, OutputStream out) throws Exception {
		OutputStreamWriter wr = new OutputStreamWriter(out, "UTF-8");
		try {
			codec.encode(t, o, wr);
		}
		finally {
			wr.flush();
		}
	}

	public <T> T decode(File in, Class<T> t) throws Exception {
		FileInputStream fin = new FileInputStream(in);
		try {
			InputStreamReader rdr = new InputStreamReader(fin, "UTF-8");
			try {
				return t.cast(decode(rdr, t));
			}
			finally {
				rdr.close();
			}
		}
		finally {
			fin.close();
		}

	}

	public void encode(Type t, Object o, File out) throws Exception {
		OutputStream oout = new FileOutputStream(out);
		try {
			Writer wr = new OutputStreamWriter(oout, "UTF-8");
			try {
				codec.encode(t, o, wr);
			}
			finally {
				wr.close();
			}
		}
		finally {
			oout.close();
		}
	}

}
