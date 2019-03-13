package aQute.bnd.runtime.gogo;

import java.io.Closeable;
import java.lang.reflect.Constructor;
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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import aQute.lib.dtoformatter.DTOFormatter;
import aQute.lib.exceptions.Exceptions;

public class Activator implements BundleActivator {

	final Set<Closeable>	closeables	= new HashSet<>();
	BundleContext			context;
	DTOFormatter			formatter	= new DTOFormatter();

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		registerConverter(context);
		register(Diagnostics.class);
		register(DS.class);
		register(Basic.class);
		register(Files.class);
		register(Inspect.class);
		register(Resources.class);
		register(Core.class);
		register(View.class);
		register(Log.class);
	}

	private void registerConverter(BundleContext context) {
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, 10000);
		ServiceRegistration<Converter> registration = context.registerService(Converter.class, new Converter() {

			@Override
			public Object convert(Class<?> arg0, Object arg1) throws Exception {
				return null;
			}

			@Override
			public CharSequence format(Object from, int level, Converter backup) throws Exception {
				try {
					return formatter.format(from, level, (o, l, f) -> {
						try {
							return backup.format(o, l, null);
						} catch (Exception e) {
							throw Exceptions.duck(e);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					throw Exceptions.duck(e);
				}
			}

		}, properties);
		closeables.add(() -> {
			registration.unregister();
		});
	}

	<T> void register(Class<T> c) throws Exception {
		try {
			Constructor<T> constructor = c.getConstructor(BundleContext.class, DTOFormatter.class);

			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(CommandProcessor.COMMAND_SCOPE, "bnd");
			Set<String> commands = new TreeSet<>();
			for (Method m : c.getMethods()) {
				Descriptor d = m.getAnnotation(Descriptor.class);

				if (d != null)
					commands.add(m.getName());
			}
			T service = constructor.newInstance(context, formatter);

			properties.put(CommandProcessor.COMMAND_SCOPE, "bnd");
			properties.put(CommandProcessor.COMMAND_FUNCTION, commands.toArray(new String[0]));

			ServiceRegistration<Object> registration = context.registerService(Object.class, service, properties);
			closeables.add(() -> {
				registration.unregister();
				if (service instanceof Closeable) {
					((Closeable) service).close();
				}
			});
		} catch (Throwable e) {
			// ignore
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
