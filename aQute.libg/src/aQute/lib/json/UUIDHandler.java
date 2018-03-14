package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class UUIDHandler extends Handler {
	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		StringHandler.string(app, object.toString());
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
		return UUID.fromString(s);
	}

}
