package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

abstract class Handler {
	abstract void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception;

	Object decodeObject(@SuppressWarnings("unused") Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to object " + this);
	}

	Object decodeArray(@SuppressWarnings("unused") Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to array " + this);
	}

	Object decode(Decoder dec, @SuppressWarnings("unused") String s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to string " + this);
	}

	Object decode(Decoder dec, @SuppressWarnings("unused") Number s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to number " + this);
	}

	Object decode(Decoder dec, @SuppressWarnings("unused") boolean s) {
		throw new UnsupportedOperationException("Cannot be mapped to boolean " + this);
	}

	Object decode(Decoder dec) {
		return null;
	}

}
