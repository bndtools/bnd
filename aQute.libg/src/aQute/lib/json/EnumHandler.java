package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class EnumHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class	type;

	public EnumHandler(Class< ? > type) {
		this.type = type;
	}

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception {
		StringHandler.string(app, object.toString());
	}

	@SuppressWarnings("unchecked")
	Object decode(String s) throws Exception {
		return Enum.valueOf(type, s);
	}

}
