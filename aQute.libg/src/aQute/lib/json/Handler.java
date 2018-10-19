package aQute.lib.json;

import static java.lang.invoke.MethodHandles.publicLookup;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

public abstract class Handler {
	public abstract void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception;

	public Object decodeObject(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to object " + this);
	}

	public Object decodeArray(Decoder isr) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to array " + this);
	}

	public Object decode(Decoder dec, String s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to string " + this);
	}

	public Object decode(Decoder dec, Number s) throws Exception {
		throw new UnsupportedOperationException("Cannot be mapped to number " + this);
	}

	public Object decode(Decoder dec, boolean s) {
		throw new UnsupportedOperationException("Cannot be mapped to boolean " + this);
	}

	public Object decode(Decoder dec) {
		return null;
	}

	private static final MethodType defaultConstructor = MethodType.methodType(void.class);

	static <T> T newInstance(Class<T> rawClass) throws Exception {
		try {
			return (T) MethodHandles.publicLookup()
				.findConstructor(rawClass, defaultConstructor)
				.invoke();
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}

	static void setField(Field f, Object targetObject, Object value) throws Exception {
		try {
			publicLookup().unreflectSetter(f)
				.invoke(targetObject, value);
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}

	static <T> T getField(Field f, Object targetObject) throws Exception {
		try {
			return (T) publicLookup().unreflectGetter(f)
				.invoke(targetObject);
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}

}
