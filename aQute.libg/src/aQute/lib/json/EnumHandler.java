package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EnumHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class type;
	final Map<String, Object>	mapping	= new HashMap<>();

	public EnumHandler(Class<?> type) {
		this.type = type;
		for ( Object constant : type.getEnumConstants()) {
			String s = JSONCodec.keyword(constant.toString().toLowerCase());
			mapping.put(s, constant);
		}
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		StringHandler.string(app, JSONCodec.keyword(object.toString()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object decode(Decoder dec, String s) throws Exception {
		try {
			return Enum.valueOf(type, s);
		} catch (IllegalArgumentException e) {
			Object result = mapping.get(s);
			if (result != null) {
				throw new IllegalArgumentException(
					"enum constant " + s + " not found for " + type.getSimpleName() + " " + mapping.values());
			}
			return result;
		}
	}

}
