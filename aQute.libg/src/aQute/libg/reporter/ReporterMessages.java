package aQute.libg.reporter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.IllegalFormatException;

import aQute.service.reporter.Messages.ERROR;
import aQute.service.reporter.Messages.WARNING;
import aQute.service.reporter.Report.Location;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;

public class ReporterMessages {

	private static final Object[] EMPTY = new Object[0];

	static class WARNINGImpl implements WARNING {
		Reporter.SetLocation loc;

		@Override
		public SetLocation file(String file) {
			return loc.file(file);
		}

		@Override
		public SetLocation header(String header) {
			return loc.header(header);
		}

		@Override
		public SetLocation context(String context) {
			return loc.context(context);
		}

		@Override
		public SetLocation method(String methodName) {
			return loc.method(methodName);
		}

		@Override
		public SetLocation line(int n) {
			return loc.line(n);
		}

		@Override
		public SetLocation reference(String reference) {
			return loc.reference(reference);
		}

		public WARNINGImpl(Reporter.SetLocation loc) {
			this.loc = loc;
		}

		@Override
		public SetLocation details(Object details) {
			return loc.details(details);
		}

		@Override
		public Location location() {
			return loc.location();
		}

		@Override
		public SetLocation length(int length) {
			loc.length(length);
			return this;
		}
	}

	static class ERRORImpl extends WARNINGImpl implements ERROR {
		public ERRORImpl(SetLocation e) {
			super(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T base(final Reporter reporter, Class<T> messages) {
		return (T) Proxy.newProxyInstance(messages.getClassLoader(), new Class[] {
			messages
		}, new InvocationHandler() {

			@Override
			@SuppressWarnings("deprecation")
			public Object invoke(Object target, Method method, Object[] args) throws Throwable {

				if (args == null)
					args = EMPTY;

				String format;
				Message d = method.getAnnotation(Message.class);
				if (d == null) {
					String name = method.getName();
					StringBuilder sb = new StringBuilder();
					sb.append(name.charAt(0));
					int n = 0;
					for (int i = 1; i < name.length(); i++) {
						char c = name.charAt(i);
						switch (c) {
							case '_' :
								sb.append(" %s");
								if (i + 1 < name.length()) {
									sb.append(", ");
								}
								n++;
								break;

							case '$' :
								sb.append(" ");
								break;

							default :
								if (Character.isUpperCase(c)) {
									sb.append(" ");
									c = Character.toLowerCase(c);
								}
								sb.append(c);
						}
					}
					while (n < method.getParameterTypes().length) {
						sb.append(": %s");
						n++;
					}
					format = sb.toString();
				} else {
					format = d.value();
				}

				try {
					if (method.getReturnType() == ERROR.class) {
						for (int i = args.length - 1; i >= 0; i--) {
							if (args[i] instanceof Throwable) {
								return new ERRORImpl(reporter.exception((Throwable) args[i], format, args));
							}
						}
						return new ERRORImpl(reporter.error(format, args));
					} else if (method.getReturnType() == WARNING.class) {
						return new WARNINGImpl(reporter.warning(format, args));
					} else {
						reporter.trace(format, args);
					}
				} catch (IllegalFormatException e) {
					reporter.error("Formatter failed: %s %s %s", method.getName(), format, Arrays.toString(args));
				}
				return null;
			}
		});
	}
}
