/*
 * Copyright (c) OSGi Alliance (2012). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.libg.reporter;

import java.lang.reflect.*;
import java.util.*;

import aQute.libg.reporter.Messages.ERROR;
import aQute.libg.reporter.Messages.WARNING;

public class ReporterMessages {

	@SuppressWarnings("unchecked")
	public static <T> T base(final Reporter reporter, Class<T> messages) {
		return (T) Proxy.newProxyInstance(messages.getClassLoader(), new Class[] {
			messages
		}, new InvocationHandler() {

			public Object invoke(Object target, Method method, Object[] args) throws Throwable {
				if (reporter.isExceptions()) {
					for (Object o : args) {
						if (o instanceof Throwable)
							((Throwable) o).printStackTrace();
					}
				}
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
