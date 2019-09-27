/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package aQute.bnd.runtime.gogo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import aQute.lib.dtoformatter.DTOFormatter;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.glob.Glob;

@SuppressWarnings("deprecation")
public class Basic {
	private final BundleContext context;

	public Basic(BundleContext bc, DTOFormatter formatter) {
		context = bc;
	}

	@Descriptor("Displays available commands")
	public Set<String> help() {
		return getCommands().keySet();
	}

	@Descriptor("Displays available commands for a given scope")
	public Set<String> scope(
	//@formatter:off

		@Descriptor("The scope name of a command, can be globbed")
		String scope


	//@formatter:on
	) {
		if (scope == null)
			scope = "*";

		Glob glob = new Glob(scope);
		return getCommands().keySet()
			.stream()
			.map(name -> name.split(":"))
			.filter(sn -> sn.length == 2 && glob.matcher(sn[0])
				.find())
			.map(sn -> sn[1])
			.collect(Collectors.toSet());
	}

	@Descriptor("displays information about a specific command")
	public void help(@Descriptor("target command") String name) {
		Map<String, List<Method>> commands = getCommands();

		List<Method> methods = null;

		// If the specified command doesn't have a scope, then
		// search for matching methods by ignoring the scope.
		int scopeIdx = name.indexOf(':');
		if (scopeIdx < 0) {
			for (Entry<String, List<Method>> entry : commands.entrySet()) {
				String k = entry.getKey()
					.substring(entry.getKey()
						.indexOf(':') + 1);
				if (name.equals(k)) {
					name = entry.getKey();
					methods = entry.getValue();
					break;
				}
			}
		}
		// Otherwise directly look up matching methods.
		else {
			methods = commands.get(name);
		}

		if ((methods != null) && (methods.size() > 0)) {
			for (Method m : methods) {
				Descriptor d = m.getAnnotation(Descriptor.class);
				if (d == null) {
					System.out.println("\n" + m.getName());
				} else {
					System.out.println("\n" + m.getName() + " - " + d.value());
				}

				System.out.println("   scope: " + name.substring(0, name.indexOf(':')));

				// Get flags and options.
				Class<?>[] paramTypes = m.getParameterTypes();
				Map<String, Parameter> flags = new TreeMap<>();
				Map<String, String> flagDescs = new TreeMap<>();
				Map<String, Parameter> options = new TreeMap<>();
				Map<String, String> optionDescs = new TreeMap<>();
				List<String> params = new ArrayList<>();
				Annotation[][] anns = m.getParameterAnnotations();
				for (int paramIdx = 0; paramIdx < anns.length; paramIdx++) {
					Class<?> paramType = m.getParameterTypes()[paramIdx];
					if (paramType == CommandSession.class) {
						/* Do not bother the user with a CommandSession. */
						continue;
					}
					Parameter p = findAnnotation(anns[paramIdx], Parameter.class);
					d = findAnnotation(anns[paramIdx], Descriptor.class);
					if (p != null) {
						if (p.presentValue()
							.equals(Parameter.UNSPECIFIED)) {
							options.put(p.names()[0], p);
							if (d != null) {
								optionDescs.put(p.names()[0], d.value());
							}
						} else {
							flags.put(p.names()[0], p);
							if (d != null) {
								flagDescs.put(p.names()[0], d.value());
							}
						}
					} else if (d != null) {
						params.add(paramTypes[paramIdx].getSimpleName());
						params.add(d.value());
					} else {
						params.add(paramTypes[paramIdx].getSimpleName());
						params.add("");
					}
				}

				// Print flags and options.
				if (flags.size() > 0) {
					System.out.println("   flags:");
					for (Entry<String, Parameter> entry : flags.entrySet()) {
						// Print all aliases.
						String[] names = entry.getValue()
							.names();
						System.out.print("      " + names[0]);
						for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
							System.out.print(", " + names[aliasIdx]);
						}
						System.out.println("   " + flagDescs.get(entry.getKey()));
					}
				}
				if (options.size() > 0) {
					System.out.println("   options:");
					for (Entry<String, Parameter> entry : options.entrySet()) {
						// Print all aliases.
						String[] names = entry.getValue()
							.names();
						System.out.print("      " + names[0]);
						for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
							System.out.print(", " + names[aliasIdx]);
						}
						System.out.println("   " + optionDescs.get(entry.getKey()) + ((entry.getValue()
							.absentValue() == null) ? "" : " [optional]"));
					}
				}
				if (params.size() > 0) {
					System.out.println("   parameters:");
					for (Iterator<String> it = params.iterator(); it.hasNext();) {
						System.out.println("      " + it.next() + "   " + it.next());
					}
				}
			}
		}
	}

	private static <T extends Annotation> T findAnnotation(Annotation[] anns, Class<T> clazz) {
		for (int i = 0; (anns != null) && (i < anns.length); i++) {
			if (clazz.isInstance(anns[i])) {
				return clazz.cast(anns[i]);
			}
		}
		return null;
	}

	private Map<String, List<Method>> getCommands() {
		ServiceReference<?>[] refs = null;
		try {
			refs = context.getAllServiceReferences(null, "(osgi.command.scope=*)");
		} catch (InvalidSyntaxException ex) {
			throw Exceptions.duck(ex);
		}

		Map<String, List<Method>> commands = new TreeMap<>();

		for (ServiceReference<?> ref : refs) {
			Object svc = context.getService(ref);
			if (svc != null) {
				String scope = (String) ref.getProperty("osgi.command.scope");
				Object ofunc = ref.getProperty("osgi.command.function");
				String[] funcs = (ofunc instanceof String[]) ? (String[]) ofunc : new String[] {
					String.valueOf(ofunc)
				};

				for (String func : funcs) {
					commands.put(scope + ":" + func, new ArrayList<Method>());
				}

				if (!commands.isEmpty()) {
					Method[] methods = svc.getClass()
						.getMethods();
					for (Method method : methods) {
						List<Method> commandMethods = commands.get(scope + ":" + method.getName());
						if (commandMethods != null) {
							commandMethods.add(method);
						}
					}
				}

				// Remove any missing commands.
				Iterator<Entry<String, List<Method>>> it = commands.entrySet()
					.iterator();
				while (it.hasNext()) {
					if (it.next()
						.getValue()
						.isEmpty()) {
						it.remove();
					}
				}
			}
		}

		return commands;
	}

}
