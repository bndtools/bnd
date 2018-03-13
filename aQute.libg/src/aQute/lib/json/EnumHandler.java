package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class EnumHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class type;

	public EnumHandler(Class<?> type) {
		this.type = type;
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		StringHandler.string(app, object.toString());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object decode(Decoder dec, String s) throws Exception {
		return Enum.valueOf(type, s);
	}

}
