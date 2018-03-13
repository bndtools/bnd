package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public abstract class Handler {
	public abstract void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception;

	public Object decodeObject(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to object " + this);
	}

	public Object decodeArray(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to array " + this);
	}

	public Object decode(Decoder dec, String s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to string " + this);
	}

	public Object decode(Decoder dec, Number s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to number " + this);
	}

	public Object decode(Decoder dec, boolean s) {
		throw new UnsupportedOperationException("Cannot be mapped to boolean " + this);
	}

	public Object decode(Decoder dec) {
		return null;
	}

}
