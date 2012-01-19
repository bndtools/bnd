package aQute.lib.osgi;

import java.io.*;
import java.lang.reflect.*;

import aQute.lib.json.*;

public class JSONResource extends WriteResource {
	final static JSONCodec	codec	= new JSONCodec();
	final Object			data;
	final Type				type;
	final long				time	= System.currentTimeMillis();

	public JSONResource(Type type, Object data) {
		this.type = type;
		this.data = data;
	}

	@Override public void write(OutputStream out) throws IOException, Exception {
		OutputStreamWriter ow = new OutputStreamWriter(out);
		try {
			codec.encode(type, data, ow);
		} finally {
			ow.flush();
		}
	}

	@Override public long lastModified() {
		return time;
	}

}
