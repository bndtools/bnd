package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class BooleanHandler extends Handler {

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception {
		app.append(object.toString());
	}

	@Override
	Object decode(boolean s) {
		return s;
	}

	@Override
	Object decode(String s) {
		return Boolean.parseBoolean(s);
	}

	@Override
	Object decode(Number s) {
		return s.intValue() != 0;
	}

	@Override
	Object decode() {
		return false;
	}

}
