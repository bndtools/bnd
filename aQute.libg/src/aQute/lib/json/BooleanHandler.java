package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class BooleanHandler extends Handler {

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		app.append(object.toString());
	}

	@Override
	public Object decode(Decoder dec, boolean s) {
		return s;
	}

	@Override
	public Object decode(Decoder dec, String s) {
		return Boolean.parseBoolean(s);
	}

	@Override
	public Object decode(Decoder dec, Number s) {
		return s.intValue() != 0;
	}

	@Override
	public Object decode(Decoder dec) {
		return false;
	}

}
