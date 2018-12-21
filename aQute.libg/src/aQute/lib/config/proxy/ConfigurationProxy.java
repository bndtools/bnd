package aQute.lib.config.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import aQute.lib.converter.Converter;

public class ConfigurationProxy {

	public static <T> T create(Class<T> type, Map<String,Object> properties) {

		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
				type
		}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals("toString") && method.getParameters().length == 0) {
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
			}
		}));
	}
}
