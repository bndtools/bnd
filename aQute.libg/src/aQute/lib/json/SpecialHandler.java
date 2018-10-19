package aQute.lib.json;

import static java.lang.invoke.MethodHandles.publicLookup;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Pattern;

public class SpecialHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class				type;
	final Method			valueOf;
	final Constructor<?>	constructor;

	public SpecialHandler(Class<?> type, Constructor<?> constructor, Method valueOf) {
		this.type = type;
		this.constructor = constructor;
		this.valueOf = valueOf;
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		StringHandler.string(app, object.toString());
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
		if (type == Pattern.class)
			return Pattern.compile(s);

		if (constructor != null) {
			try {
				return publicLookup().unreflectConstructor(constructor)
					.invoke(s);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		if (valueOf != null) {
			try {
				return publicLookup().unreflect(valueOf)
					.invoke(s);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		throw new IllegalArgumentException("Do not know how to convert a " + type + " from a string");
	}

}
