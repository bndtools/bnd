package aQute.lib.config.proxy;

import java.lang.reflect.Proxy;
import java.util.Map;

import aQute.lib.converter.Converter;

public class ConfigurationProxy {

	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> type, Map<String, Object> properties) {

		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
			type
		}, (proxy, method, args) -> {
			if (method.getName()
				.equals("toString") && method.getParameters().length == 0) {
				return properties.toString();
			}
			String key = Converter.mangleMethodName(method.getName());

			Object value;

			if (properties.containsKey(key)) {
				value = properties.get(key);
			} else {
				key = "?." + key;
				if (properties.containsKey(key)) {
					value = properties.get(key);
				} else
					value = method.getDefaultValue();
			}

			return Converter.cnv(method.getGenericReturnType(), value);
		});
	}
}
