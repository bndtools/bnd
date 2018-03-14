package biz.aQute.bnd.diagnostics.gogo.osgi;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import biz.aQute.bnd.diagnostics.gogo.impl.Diagnostics;

public class Activator implements BundleActivator, Converter {

	private Diagnostics d;

	@Override
	public void start(BundleContext context) throws Exception {
		Hashtable<String, Object> p = new Hashtable<>();
		p.put(CommandProcessor.COMMAND_SCOPE, "bnd");
		Set<String> commands = new TreeSet<>();
		for (Method m : Diagnostics.class.getMethods()) {
			Descriptor d = m.getAnnotation(Descriptor.class);

			if (d != null)
				commands.add(m.getName());
		}
		p.put(CommandProcessor.COMMAND_SCOPE, "bnd");
		p.put(CommandProcessor.COMMAND_FUNCTION, commands.toArray(new String[0]));
		context.registerService(Diagnostics.class, d = new Diagnostics(context), p);

		context.registerService(Converter.class, this, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		d.close();
	}

	@Override
	public Object convert(Class<?> targetType, Object source) throws Exception {
		if (source instanceof Converter) {
			return ((Converter) source).convert(targetType, source);
		}
		return null;
	}

	@Override
	public CharSequence format(Object source, int level, Converter next) throws Exception {
		if (source instanceof Converter) {
			return ((Converter) source).format(source, level, next);
		}
		return null;
	}

}
