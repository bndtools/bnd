package aQute.bnd.make.component;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.make.metatype.*;
import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Clazz.QUERY;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.libg.header.*;
import aQute.libg.version.*;

/**
 * This class is an analyzer plugin. It looks at the properties and tries to
 * find out if the Service-Component header contains the bnd shortut syntax. If
 * not, the header is copied to the output, if it does, an XML file is created
 * and added to the JAR and the header is modified appropriately.
 */
public class ServiceComponent implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		ComponentMaker m = new ComponentMaker(analyzer);

		Map<String,Map<String,String>> l = m.doServiceComponent();

		analyzer.setProperty(Constants.SERVICE_COMPONENT, Processor.printClauses(l));

		analyzer.getInfo(m, "Service-Component: ");
		m.close();

		return false;
	}

	private static class ComponentMaker extends Processor {
		Analyzer	analyzer;

		ComponentMaker(Analyzer analyzer) {
			super(analyzer);
			this.analyzer = analyzer;
		}

		/**
		 * Iterate over the Service Component entries. There are two cases:
		 * <ol>
		 * <li>An XML file reference</li>
		 * <li>A FQN/wildcard with a set of attributes</li>
		 * </ol>
		 * An XML reference is immediately expanded, an FQN/wildcard is more
		 * complicated and is delegated to
		 * {@link #componentEntry(Map, String, Map)}.
		 * 
		 * @throws Exception
		 */
		Map<String,Map<String,String>> doServiceComponent() throws Exception {
			Map<String,Map<String,String>> serviceComponents = newMap();
			String header = getProperty(SERVICE_COMPONENT);
			Parameters sc = parseHeader(header);

			for (Entry<String,Attrs> entry : sc.entrySet()) {
				String name = entry.getKey();
				Map<String,String> info = entry.getValue();

				try {
					if (name.indexOf('/') >= 0 || name.endsWith(".xml")) {
						// Normal service component, we do not process it
						serviceComponents.put(name, EMPTY);
					} else {
						componentEntry(serviceComponents, name, info);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					error("Invalid Service-Component header: %s %s, throws %s", name, info, e);
				}
			}
			return serviceComponents;
		}

		/**
		 * Parse an entry in the Service-Component header. This header supports
		 * the following types:
		 * <ol>
		 * <li>An FQN + attributes describing a component</li>
		 * <li>A wildcard expression for finding annotated components.</li>
		 * </ol>
		 * The problem is the distinction between an FQN and a wildcard because
		 * an FQN can also be used as a wildcard. If the info specifies
		 * {@link Constants#NOANNOTATIONS} then wildcards are an error and the
		 * component must be fully described by the info. Otherwise the
		 * FQN/wildcard is expanded into a list of classes with annotations. If
		 * this list is empty, the FQN case is interpreted as a complete
		 * component definition. For the wildcard case, it is checked if any
		 * matching classes for the wildcard have been compiled for a class file
		 * format that does not support annotations, this can be a problem with
		 * JSR14 who silently ignores annotations. An error is reported in such
		 * a case.
		 * 
		 * @param serviceComponents
		 * @param name
		 * @param info
		 * @throws Exception
		 * @throws IOException
		 */
		private void componentEntry(Map<String,Map<String,String>> serviceComponents, String name,
				Map<String,String> info) throws Exception, IOException {

			boolean annotations = !Processor.isTrue(info.get(NOANNOTATIONS));
			boolean fqn = Verifier.isFQN(name);

			if (annotations) {

				// Annotations possible!

				Collection<Clazz> annotatedComponents = analyzer.getClasses("", QUERY.ANNOTATED.toString(),
						Component.class.getName(), //
						QUERY.NAMED.toString(), name //
						);

				if (fqn) {
					if (annotatedComponents.isEmpty()) {

						// No annotations, fully specified in header

						createComponentResource(serviceComponents, name, info);
					} else {

						// We had a FQN so expect only one

						for (Clazz c : annotatedComponents) {
							annotated(serviceComponents, c, info);
						}
					}
				} else {

					// We did not have an FQN, so expect the use of wildcards

					if (annotatedComponents.isEmpty())
						checkAnnotationsFeasible(name);
					else
						for (Clazz c : annotatedComponents) {
							annotated(serviceComponents, c, info);
						}
				}
			} else {
				// No annotations
				if (fqn)
					createComponentResource(serviceComponents, name, info);
				else
					error("Set to %s but entry %s is not an FQN ", NOANNOTATIONS, name);

			}
		}

		/**
		 * Check if annotations are actually feasible looking at the class
		 * format. If the class format does not provide annotations then it is
		 * no use specifying annotated components.
		 * 
		 * @param name
		 * @return
		 * @throws Exception
		 */
		private Collection<Clazz> checkAnnotationsFeasible(String name) throws Exception {
			Collection<Clazz> not = analyzer.getClasses("", QUERY.NAMED.toString(), name //
					);

			if (not.isEmpty())
				if ("*".equals(name))
					return not;
				else
					error("Specified %s but could not find any class matching this pattern", name);

			for (Clazz c : not) {
				if (c.getFormat().hasAnnotations())
					return not;
			}

			warning("Wildcards are used (%s) requiring annotations to decide what is a component. Wildcard maps to classes that are compiled with java.target < 1.5. Annotations were introduced in Java 1.5",
					name);

			return not;
		}

		void annotated(Map<String,Map<String,String>> components, Clazz c, Map<String,String> info) throws Exception {
			// Get the component definition
			// from the annotations
			Map<String,String> map = ComponentAnnotationReader.getDefinition(c, this);

			// Pick the name, the annotation can override
			// the name.
			String localname = map.get(COMPONENT_NAME);
			if (localname == null)
				localname = c.getFQN();

			// Override the component info without manifest
			// entries. We merge the properties though.

			String merged = Processor.merge(info.remove(COMPONENT_PROPERTIES), map.remove(COMPONENT_PROPERTIES));
			if (merged != null && merged.length() > 0)
				map.put(COMPONENT_PROPERTIES, merged);
			map.putAll(info);
			createComponentResource(components, localname, map);
		}

		private void createComponentResource(Map<String,Map<String,String>> components, String name,
				Map<String,String> info) throws IOException {

			// We can override the name in the parameters
			if (info.containsKey(COMPONENT_NAME))
				name = info.get(COMPONENT_NAME);

			// Assume the impl==name, but allow override
			String impl = name;
			if (info.containsKey(COMPONENT_IMPLEMENTATION))
				impl = info.get(COMPONENT_IMPLEMENTATION);

			TypeRef implRef = analyzer.getTypeRefFromFQN(impl);
			// Check if such a class exists
			analyzer.referTo(implRef);

			boolean designate = designate(name, info.get(COMPONENT_DESIGNATE), false)
					|| designate(name, info.get(COMPONENT_DESIGNATEFACTORY), true);

			// If we had a designate, we want a default configuration policy of
			// require.
			if (designate && info.get(COMPONENT_CONFIGURATION_POLICY) == null)
				info.put(COMPONENT_CONFIGURATION_POLICY, "require");

			// We have a definition, so make an XML resources
			Resource resource = createComponentResource(name, impl, info);
			analyzer.getJar().putResource("OSGI-INF/" + name + ".xml", resource);

			components.put("OSGI-INF/" + name + ".xml", EMPTY);

		}

		/**
		 * Create a Metatype and Designate record out of the given
		 * configurations.
		 * 
		 * @param name
		 * @param config
		 */
		private boolean designate(String name, String config, boolean factory) {
			if (config == null)
				return false;

			for (String c : Processor.split(config)) {
				TypeRef ref = analyzer.getTypeRefFromFQN(c);
				Clazz clazz = analyzer.getClassspace().get(ref);
				if (clazz != null) {
					analyzer.referTo(ref);
					MetaTypeReader r = new MetaTypeReader(clazz, analyzer);
					r.setDesignate(name, factory);
					String rname = "OSGI-INF/metatype/" + name + ".xml";

					analyzer.getJar().putResource(rname, r);
				} else {
					analyzer.error("Cannot find designated configuration class %s for component %s", c, name);
				}
			}
			return true;
		}

		/**
		 * Create the resource for a DS component.
		 * 
		 * @param list
		 * @param name
		 * @param info
		 * @throws UnsupportedEncodingException
		 */
		Resource createComponentResource(String name, String impl, Map<String,String> info) throws IOException {
			String namespace = getNamespace(info);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Constants.DEFAULT_CHARSET));
			pw.println("<?xml version='1.0' encoding='utf-8'?>");
			if (namespace != null)
				pw.print("<scr:component xmlns:scr='" + namespace + "'");
			else
				pw.print("<component");

			doAttribute(pw, name, "name");
			doAttribute(pw, info.get(COMPONENT_FACTORY), "factory");
			doAttribute(pw, info.get(COMPONENT_IMMEDIATE), "immediate", "false", "true");
			doAttribute(pw, info.get(COMPONENT_ENABLED), "enabled", "true", "false");
			doAttribute(pw, info.get(COMPONENT_CONFIGURATION_POLICY), "configuration-policy", "optional", "require",
					"ignore");
			doAttribute(pw, info.get(COMPONENT_ACTIVATE), "activate", JIDENTIFIER);
			doAttribute(pw, info.get(COMPONENT_DEACTIVATE), "deactivate", JIDENTIFIER);
			doAttribute(pw, info.get(COMPONENT_MODIFIED), "modified", JIDENTIFIER);

			pw.println(">");

			// Allow override of the implementation when people
			// want to choose their own name
			pw.println("  <implementation class='" + (impl == null ? name : impl) + "'/>");

			String provides = info.get(COMPONENT_PROVIDE);
			boolean servicefactory = Processor.isTrue(info.get(COMPONENT_SERVICEFACTORY));

			if (servicefactory && Processor.isTrue(info.get(COMPONENT_IMMEDIATE))) {
				// TODO can become error() if it is up to me
				warning("For a Service Component, the immediate option and the servicefactory option are mutually exclusive for %(%s)",
						name, impl);
			}
			provide(pw, provides, servicefactory, impl);
			properties(pw, info);
			reference(info, pw);

			if (namespace != null)
				pw.println("</scr:component>");
			else
				pw.println("</component>");

			pw.close();
			byte[] data = out.toByteArray();
			out.close();
			return new EmbeddedResource(data, 0);
		}

		private void doAttribute(PrintWriter pw, String value, String name, String... matches) {
			if (value != null) {
				if (matches.length != 0) {
					if (matches.length == 1 && matches[0].equals(JIDENTIFIER)) {
						if (!Verifier.isIdentifier(value))
							error("Component attribute %s has value %s but is not a Java identifier", name, value);
					} else {

						if (!Verifier.isMember(value, matches))
							error("Component attribute %s has value %s but is not a member of %s", name, value,
									Arrays.toString(matches));
					}
				}
				pw.print(" ");
				pw.print(name);
				pw.print("='");
				pw.print(value);
				pw.print("'");
			}
		}

		/**
		 * Check if we need to use the v1.1 namespace (or later).
		 * 
		 * @param info
		 * @return
		 */
		private String getNamespace(Map<String,String> info) {
			String version = info.get(COMPONENT_VERSION);
			if (version != null) {
				try {
					Version v = new Version(version);
					return NAMESPACE_STEM + "/v" + v;
				}
				catch (Exception e) {
					error("version: specified on component header but not a valid version: " + version);
					return null;
				}
			}
			for (String key : info.keySet()) {
				if (SET_COMPONENT_DIRECTIVES_1_1.contains(key)) {
					return NAMESPACE_STEM + "/v1.1.0";
				}
			}
			return null;
		}

		/**
		 * Print the Service-Component properties element
		 * 
		 * @param pw
		 * @param info
		 */
		void properties(PrintWriter pw, Map<String,String> info) {
			Collection<String> properties = split(info.get(COMPONENT_PROPERTIES));
			for (Iterator<String> p = properties.iterator(); p.hasNext();) {
				String clause = p.next();
				int n = clause.indexOf('=');
				if (n <= 0) {
					error("Not a valid property in service component: " + clause);
				} else {
					String type = null;
					String name = clause.substring(0, n);
					if (name.indexOf('@') >= 0) {
						String parts[] = name.split("@");
						name = parts[1];
						type = parts[0];
					} else if (name.indexOf(':') >= 0) {
						String parts[] = name.split(":");
						name = parts[0];
						type = parts[1];
					}
					String value = clause.substring(n + 1).trim();
					// TODO verify validity of name and value.
					pw.print("  <property name='");
					pw.print(name);
					pw.print("'");

					if (type != null) {
						if (VALID_PROPERTY_TYPES.matcher(type).matches()) {
							pw.print(" type='");
							pw.print(type);
							pw.print("'");
						} else {
							warning("Invalid property type '" + type + "' for property " + name);
						}
					}

					String parts[] = value.split("\\s*(\\||\\n)\\s*");
					if (parts.length > 1) {
						pw.println(">");
						for (String part : parts) {
							pw.println(part);
						}
						pw.println("</property>");
					} else {
						pw.print(" value='");
						pw.print(parts[0]);
						pw.println("'/>");
					}
				}
			}
		}

		/**
		 * @param pw
		 * @param provides
		 */
		void provide(PrintWriter pw, String provides, boolean servicefactory, String impl) {
			if (provides != null) {
				if (!servicefactory)
					pw.println("  <service>");
				else
					pw.println("  <service servicefactory='true'>");

				StringTokenizer st = new StringTokenizer(provides, ",");
				while (st.hasMoreTokens()) {
					String interfaceName = st.nextToken();
					TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
					pw.println("    <provide interface='" + interfaceName + "'/>");
					analyzer.referTo(ref);

					// TODO verifies the impl. class extends or implements the
					// interface
				}
				pw.println("  </service>");
			} else if (servicefactory)
				warning("The servicefactory:=true directive is set but no service is provided, ignoring it");
		}

		public final static Pattern	REFERENCE	= Pattern.compile("([^(]+)(\\(.+\\))?");

		/**
		 * @param info
		 * @param pw
		 */

		void reference(Map<String,String> info, PrintWriter pw) {
			Collection<String> dynamic = new ArrayList<String>(split(info.get(COMPONENT_DYNAMIC)));
			Collection<String> optional = new ArrayList<String>(split(info.get(COMPONENT_OPTIONAL)));
			Collection<String> multiple = new ArrayList<String>(split(info.get(COMPONENT_MULTIPLE)));

			Collection<String> descriptors = split(info.get(COMPONENT_DESCRIPTORS));

			for (Map.Entry<String,String> entry : info.entrySet()) {

				// Skip directives
				String referenceName = entry.getKey();
				if (referenceName.endsWith(":")) {
					if (!SET_COMPONENT_DIRECTIVES.contains(referenceName))
						error("Unrecognized directive in Service-Component header: " + referenceName);
					continue;
				}

				// Parse the bind/unbind methods from the name
				// if set. They are separated by '/'
				String bind = null;
				String unbind = null;

				boolean unbindCalculated = false;

				if (referenceName.indexOf('/') >= 0) {
					String parts[] = referenceName.split("/");
					referenceName = parts[0];
					bind = parts[1];
					if (parts.length > 2) {
						unbind = parts[2];
					} else {
						unbindCalculated = true;
						if (bind.startsWith("add"))
							unbind = bind.replaceAll("add(.+)", "remove$1");
						else
							unbind = "un" + bind;
					}
				} else if (Character.isLowerCase(referenceName.charAt(0))) {
					unbindCalculated = true;
					bind = "set" + Character.toUpperCase(referenceName.charAt(0)) + referenceName.substring(1);
					unbind = "un" + bind;
				}

				String interfaceName = entry.getValue();
				if (interfaceName == null || interfaceName.length() == 0) {
					error("Invalid Interface Name for references in Service Component: " + referenceName + "="
							+ interfaceName);
					continue;
				}

				// If we have descriptors, we have analyzed the component.
				// So why not check the methods
				if (descriptors.size() > 0) {
					// Verify that the bind method exists
					if (!descriptors.contains(bind))
						error("The bind method %s for %s not defined", bind, referenceName);

					// Check if the unbind method exists
					if (!descriptors.contains(unbind)) {
						if (unbindCalculated)
							// remove it
							unbind = null;
						else
							error("The unbind method %s for %s not defined", unbind, referenceName);
					}
				}
				// Check tje cardinality by looking at the last
				// character of the value
				char c = interfaceName.charAt(interfaceName.length() - 1);
				if ("?+*~".indexOf(c) >= 0) {
					if (c == '?' || c == '*' || c == '~')
						optional.add(referenceName);
					if (c == '+' || c == '*')
						multiple.add(referenceName);
					if (c == '+' || c == '*' || c == '?')
						dynamic.add(referenceName);
					interfaceName = interfaceName.substring(0, interfaceName.length() - 1);
				}

				// Parse the target from the interface name
				// The target is a filter.
				String target = null;
				Matcher m = REFERENCE.matcher(interfaceName);
				if (m.matches()) {
					interfaceName = m.group(1);
					target = m.group(2);
				}
				TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
				analyzer.referTo(ref);

				pw.printf("  <reference name='%s'", referenceName);
				pw.printf(" interface='%s'", interfaceName);

				String cardinality = optional.contains(referenceName) ? "0" : "1";
				cardinality += "..";
				cardinality += multiple.contains(referenceName) ? "n" : "1";
				if (!cardinality.equals("1..1"))
					pw.print(" cardinality='" + cardinality + "'");

				if (bind != null) {
					pw.printf(" bind='%s'", bind);
					if (unbind != null) {
						pw.printf(" unbind='%s'", unbind);
					}
				}

				if (dynamic.contains(referenceName)) {
					pw.print(" policy='dynamic'");
				}

				if (target != null) {
					// Filter filter = new Filter(target);
					// if (filter.verify() == null)
					// pw.print(" target='" + filter.toString() + "'");
					// else
					// error("Target for " + referenceName
					// + " is not a correct filter: " + target + " "
					// + filter.verify());
					pw.print(" target='" + escape(target) + "'");
				}
				pw.println("/>");
			}
		}
	}

	/**
	 * Escape a string, do entity conversion.
	 */
	static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '<' :
					sb.append("&lt;");
					break;
				case '>' :
					sb.append("&gt;");
					break;
				case '&' :
					sb.append("&amp;");
					break;
				case '\'' :
					sb.append("&quot;");
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

}
