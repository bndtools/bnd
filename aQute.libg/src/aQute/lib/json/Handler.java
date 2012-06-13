package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

abstract class Handler {
	abstract void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception;

	Object decodeObject(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to object " + this);
	}

	Object decodeArray(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to array " + this);
	}

	Object decode(String s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to string " + this);
	}

	Object decode(Number s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to number " + this);
	}

	Object decode(boolean s) {
		throw new UnsupportedOperationException("Cannot be mapped to boolean " + this);
	}

	Object decode() {
		return null;
	}

}
