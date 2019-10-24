package aQute.bnd.help;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import aQute.bnd.osgi.Processor;
import aQute.lib.converter.Converter;

/**
 * Handles a proxy on a Processor's properties. In contrast with the Converter,
 * this handler always returns an instance when the return type is a Syntax
 * interface even if this instruction is not set. This makes it easier to work
 * with defaults. To find out if an instruction is actually set, use an
 * Optional.
 */
class ProcessorHandler implements InvocationHandler {
	final Processor					processor;

	final static SpecialConverter	converter	= new SpecialConverter();

	public ProcessorHandler(Processor processor) {
		this.processor = processor;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		if (method.getDeclaringClass() == Object.class) {
			return method.invoke(this);
		}

		String name = Syntax.toInstruction(method);

		Object value;
		if (Converter.isMultiple(method.getReturnType()))
			value = processor.mergeProperties(name);
		else
			value = processor.getProperty(name);

		if (value == null) {
			if (args != null && args.length == 1) {
				return args[0];
			}
			if (method.getDefaultValue() != null)
				return method.getDefaultValue();
		}

		return converter.convertNeverNull(method.getGenericReturnType(), value);
	}


	@SuppressWarnings("unchecked")
	public static <T> T getInstructions(Processor processor, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
			type
		}, new ProcessorHandler(processor));
	}

	@Override
	public String toString() {
		return processor.toString() + "'";
	}
}
