package aQute.libg.reporter;

import java.lang.reflect.*;
import java.util.*;

import aQute.libg.reporter.Messages.ERROR;
import aQute.libg.reporter.Messages.WARNING;
import aQute.service.reporter.*;

public class ReporterMessages {

	public static <T> T base(final Reporter reporter, Class<T> messages ) {
		return (T) Proxy.newProxyInstance(messages.getClassLoader(), new Class[] {
			messages
		}, new InvocationHandler() {

			public Object invoke(Object target, Method method, Object[] args) throws Throwable {
				String format;
				Message d = method.getAnnotation(Message.class);
				if (d == null) {
					String name = method.getName();
					StringBuilder sb = new StringBuilder();
					sb.append(name.charAt(0));
					for (int i = 1; i < name.length(); i++) {
						char c = name.charAt(i);
						switch (c) {
							case '_' :
								sb.append(" %s, ");
								break;

							default :
								if (Character.isUpperCase(c)) {
									sb.append(" ");
									c = Character.toLowerCase(c);
								}
								sb.append(c);
						}
					}
					format = sb.toString();
				} else
					format = d.value();

				try {
					if (method.getReturnType() == ERROR.class) {
						reporter.error(format, args);
					} else if (method.getReturnType() == WARNING.class) {
						reporter.warning(format, args);
					} else
						reporter.trace(format, args);
				}
				catch (IllegalFormatException e) {
					reporter.error("Formatter failed: %s %s %s", method.getName(), format, Arrays.toString(args));
				}
				return null;
			}
		});
	}
}
