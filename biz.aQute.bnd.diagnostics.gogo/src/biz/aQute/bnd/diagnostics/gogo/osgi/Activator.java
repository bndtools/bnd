package biz.aQute.bnd.diagnostics.gogo.osgi;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import biz.aQute.bnd.diagnostics.gogo.impl.ComponentAnalyzer;
import biz.aQute.bnd.diagnostics.gogo.impl.Diagnostics;

public class Activator implements BundleActivator {

	final Set<Closeable>	closeables	= new HashSet<>();
	BundleContext			context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		register(Diagnostics.class);
		register(ComponentAnalyzer.class);
	}

	<T extends Converter> void register(Class<T> c) throws Exception {
		try {
			Constructor<T> constructor = c.getConstructor(BundleContext.class);

			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(CommandProcessor.COMMAND_SCOPE, "bnd");
			Set<String> commands = new TreeSet<>();
			for (Method m : c.getMethods()) {
				Descriptor d = m.getAnnotation(Descriptor.class);

				if (d != null)
					commands.add(m.getName());
			}
			T service = constructor.newInstance(context);

			properties.put(CommandProcessor.COMMAND_SCOPE, "bnd");
			properties.put(CommandProcessor.COMMAND_FUNCTION, commands.toArray(new String[0]));
			System.out.println("Cmd " + service + " " + properties);
			ServiceRegistration<Converter> registration = context.registerService(Converter.class, service, properties);
			closeables.add(() -> {
				registration.unregister();
				if (service instanceof Closeable) {
					((Closeable) service).close();
				}
			});

		} catch (InvocationTargetException e) {
			System.out.println("Oops " + e.getTargetException());
		} catch (Exception e) {
			System.out.println("Oops " + e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		closeables.forEach(c -> {
			try {
				c.close();
			} catch (Exception e) {
				// ignore
			}
		});
	}

}
