package aQute.bnd.help;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import aQute.bnd.header.Attrs;

/**
 * The invocation handler that is based on Attrs. Although we could use the
 * normal Map handler built into the Converter, it is better to convert from the
 * actual typed values in Attrs.
 */
class AttrsHandler implements InvocationHandler {

	final Attrs attrs;

	public AttrsHandler(Attrs attrs) {
		assert attrs != null;
		this.attrs = attrs;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Object.class) {
			return method.invoke(this);
		}

		String name = Syntax.toProperty(method);

		Object value = attrs.getTyped(name);
		if (value == null) {
			if (method.getDefaultValue() != null) {
				return method.getDefaultValue();
			}
			if (args != null && args.length == 1) {
				return args[0];
			}
		}
		return ProcessorHandler.converter.convertNeverNull(method.getGenericReturnType(), value);
	}

	@SuppressWarnings("unchecked")
	public static <X> X getProperties(Attrs attrs, Class<X> type) {
		return (X) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
			type
		}, new AttrsHandler(attrs));
	}

	@Override
	public String toString() {
		return attrs.toString() + "'";
	}
}
