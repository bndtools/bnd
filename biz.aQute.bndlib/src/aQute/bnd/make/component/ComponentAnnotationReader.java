package aQute.bnd.make.component;

import static aQute.bnd.make.component.ServiceComponent.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.annotation.component.*;
import aQute.lib.osgi.*;
import aQute.libg.reporter.*;

public class ComponentAnnotationReader extends ClassDataCollector {
	String						EMPTY[]					= new String[0];
	private static final String	V1_1					= "1.1.0";																																// "1.1.0"
	static Pattern				BINDDESCRIPTOR			= Pattern
																.compile("\\(L([^;]*);(Ljava/util/Map;|Lorg/osgi/framework/ServiceReference;)*\\)V");
	static Pattern				BINDMETHOD				= Pattern.compile("(set|bind|add)(.)(.*)");

	static Pattern				ACTIVATEDESCRIPTOR		= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;))*\\)V");
	static Pattern				OLDACTIVATEDESCRIPTOR	= Pattern
																.compile("\\(Lorg/osgi/service/component/ComponentContext;\\)V");
	static Pattern				OLDBINDDESCRIPTOR		= Pattern.compile("\\(L([^;]*);\\)V");
	static Pattern				REFERENCEBINDDESCRIPTOR	= Pattern
																.compile("\\(Lorg/osgi/framework/ServiceReference;\\)V");

	Reporter					reporter				= new Processor();
	String						method;
	String						methodDescriptor;
	int							methodAccess;
	String						className;
	Clazz						clazz;
	String						interfaces[];
	Set<String>					multiple				= new HashSet<String>();
	Set<String>					optional				= new HashSet<String>();
	Set<String>					dynamic					= new HashSet<String>();

	Map<String, String>			map						= new TreeMap<String, String>();
	Set<String>					descriptors				= new HashSet<String>();
	List<String>				properties				= new ArrayList<String>();
	String						version					= null;

	// TODO make patterns for descriptors

	ComponentAnnotationReader(Clazz clazz) {
		this.clazz = clazz;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public Reporter getReporter() {
		return this.reporter;
	}

	public static Map<String, String> getDefinition(Clazz c) throws Exception {
		return getDefinition(c, new Processor());
	}

	public static Map<String, String> getDefinition(Clazz c, Reporter reporter) throws Exception {
		ComponentAnnotationReader r = new ComponentAnnotationReader(c);
		r.setReporter(reporter);
		c.parseClassFileWithCollector(r);
		r.finish();
		return r.map;
	}

	public void annotation(Annotation annotation) {

		if (annotation.getName().equals(Component.RNAME)) {
			set(COMPONENT_NAME, annotation.get(Component.NAME), "<>");
			set(COMPONENT_FACTORY, annotation.get(Component.FACTORY), false);
			setBoolean(COMPONENT_ENABLED, annotation.get(Component.ENABLED), true);
			setBoolean(COMPONENT_IMMEDIATE, annotation.get(Component.IMMEDIATE), false);
			setBoolean(COMPONENT_SERVICEFACTORY, annotation.get(Component.SERVICEFACTORY), false);

			if (annotation.get(Component.DESIGNATE) != null) {
				String configs = annotation.get(Component.DESIGNATE);
				if (configs != null) {
					set(COMPONENT_DESIGNATE, Clazz.objectDescriptorToFQN(configs), "");
				}
			}

			if (annotation.get(Component.DESIGNATE_FACTORY) != null) {
				String configs = annotation.get(Component.DESIGNATE_FACTORY);
				if (configs != null) {
					set(COMPONENT_DESIGNATEFACTORY, Clazz.objectDescriptorToFQN(configs), "");
				}
			}

			setVersion((String) annotation.get(Component.VERSION));

			String configurationPolicy = annotation.get(Component.CONFIGURATION_POLICY);
			if (configurationPolicy != null)
				set(COMPONENT_CONFIGURATION_POLICY, configurationPolicy.toLowerCase(), "");

			doProperties(annotation);

			Object[] provides = (Object[]) annotation.get(Component.PROVIDE);
			String[] p;
			if (provides == null) {
				// Use the found interfaces, but convert from internal to
				// fqn.
				if (interfaces != null) {
					List<String> result = new ArrayList<String>();
					for (int i = 0; i < interfaces.length; i++) {
						if (!interfaces[i].equals("scala/ScalaObject") )
							result.add(Clazz.internalToFqn(interfaces[i]));
					}
					p = result.toArray(EMPTY);
				} else
					p = EMPTY;
			} else {
				// We have explicit interfaces set
				p = new String[provides.length];
				for (int i = 0; i < provides.length; i++) {
					p[i] = descriptorToFQN(provides[i].toString());
				}
			}
			if (p.length > 0) {
				set(COMPONENT_PROVIDE, Processor.join(Arrays.asList(p)), "<>");
			}

		} else if (annotation.getName().equals(Activate.RNAME)) {
			if (!checkMethod())
				setVersion(V1_1);

			if (!ACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches())
				reporter.error(
						"Activate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
						className, methodDescriptor);

			if (method.equals("activate")
					&& OLDACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches()) {
				// this is the default!
			} else {
				setVersion(V1_1);
				set(COMPONENT_ACTIVATE, method, "<>");
			}

		} else if (annotation.getName().equals(Deactivate.RNAME)) {
			if (!checkMethod())
				setVersion(V1_1);

			if (!ACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches())
				reporter.error(
						"Deactivate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
						className, methodDescriptor);
			if (method.equals("deactivate")
					&& OLDACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches()) {
				// This is the default!
			} else {
				setVersion(V1_1);
				set(COMPONENT_DEACTIVATE, method, "<>");
			}
		} else if (annotation.getName().equals(Modified.RNAME)) {
			set(COMPONENT_MODIFIED, method, "<>");
			setVersion(V1_1);
		} else if (annotation.getName().equals(Reference.RNAME)) {

			String name = (String) annotation.get(Reference.NAME);
			String bind = method;
			String unbind = null;

			if (name == null) {
				Matcher m = BINDMETHOD.matcher(method);
				if (m.matches()) {
					name = m.group(2).toLowerCase() + m.group(3);
				} else {
					name = method.toLowerCase();
				}
			}
			String simpleName = name;

			unbind = annotation.get(Reference.UNBIND);

			if (bind != null) {
				name = name + "/" + bind;
				if (unbind != null)
					name = name + "/" + unbind;
			}
			String service = annotation.get(Reference.SERVICE);

			if (service != null) {
				service = Clazz.objectDescriptorToFQN(service);
			} else {
				// We have to find the type of the current method to
				// link it to the referenced service.
				Matcher m = BINDDESCRIPTOR.matcher(methodDescriptor);
				if (m.matches()) {
					service = Clazz.internalToFqn(m.group(1));
				} else
					throw new IllegalArgumentException(
							"Cannot detect the type of a Component Reference from the descriptor: "
									+ methodDescriptor);
			}

			// Check if we have a target, this must be a filter
			String target = annotation.get(Reference.TARGET);
			if (target != null) {
				Verifier.verifyFilter(target, 0);
				service = service + target;
			}

			Integer c = annotation.get(Reference.TYPE);
			if (c != null && !c.equals(0) && !c.equals((int) '1')) {
				service = service + (char) c.intValue();
			}

			if (map.containsKey(name))
				reporter.error(
						"In component %s, Multiple references with the same name: %s. Previous def: %s, this def: %s",
						name, map.get(name), service, "");
			map.put(name, service);

			if (isTrue(annotation.get(Reference.MULTIPLE)))
				multiple.add(simpleName);
			if (isTrue(annotation.get(Reference.OPTIONAL)))
				optional.add(simpleName);
			if (isTrue(annotation.get(Reference.DYNAMIC)))
				dynamic.add(simpleName);

			if (!checkMethod())
				setVersion(V1_1);
			else if (REFERENCEBINDDESCRIPTOR.matcher(methodDescriptor).matches()
					|| !OLDBINDDESCRIPTOR.matcher(methodDescriptor).matches())
				setVersion(V1_1);
		}
	}

	private void setVersion(String v) {
		if (v == null)
			return;

		if (version == null)
			version = v;
		else if (v.compareTo(version) > 0) // we're safe to 9.9.9
			version = v;
	}

	private boolean checkMethod() {
		return Modifier.isPublic(methodAccess) || Modifier.isProtected(methodAccess);
	}

	static Pattern	PROPERTY_PATTERN	= Pattern.compile("[^=]+=.+");

	private void doProperties(Annotation annotation) {
		Object[] properties = annotation.get(Component.PROPERTIES);

		if (properties != null) {
			for (Object o : properties) {
				String p = (String) o;
				if (PROPERTY_PATTERN.matcher(p).matches())
					this.properties.add(p);
				else
					throw new IllegalArgumentException("Malformed property '" + p + "' on: "
							+ annotation.get(Component.NAME));
			}
		}
	}

	private boolean isTrue(Object object) {
		if (object == null)
			return false;
		return (Boolean) object;
	}

	private void setBoolean(String string, Object object, boolean b) {
		if (object == null)
			object = b;

		Boolean bb = (Boolean) object;
		if (bb == b)
			return;

		map.put(string, bb.toString());
	}

	private void set(String string, Object object, Object deflt) {
		if (object == null || object.equals(deflt))
			return;

		map.put(string, object.toString());
	}

	/**
	 * Skip L and ; and replace / for . in an object descriptor.
	 * 
	 * A string like Lcom/acme/Foo; becomes com.acme.Foo
	 * 
	 * @param string
	 * @return
	 */

	private String descriptorToFQN(String string) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < string.length() - 1; i++) {
			char c = string.charAt(i);
			if (c == '/')
				c = '.';
			sb.append(c);
		}
		return sb.toString();
	}

	@Override public void classBegin(int access, String name) {
		className = name;
	}

	@Override public void implementsInterfaces(String[] interfaces) {
		this.interfaces = interfaces;
	}

	@Override public void method(int access, String name, String descriptor) {
		this.method = name;
		this.methodDescriptor = descriptor;
		this.methodAccess = access;
		descriptors.add(method);
	}

	void set(String name, Collection<String> l) {
		if (l.size() == 0)
			return;

		set(name, Processor.join(l), "<>");
	}

	public void finish() {
		set(COMPONENT_MULTIPLE, multiple);
		set(COMPONENT_DYNAMIC, dynamic);
		set(COMPONENT_OPTIONAL, optional);
		set(COMPONENT_IMPLEMENTATION, clazz.getFQN(), "<>");
		set(COMPONENT_PROPERTIES, properties);
		if (version != null) {
			set(COMPONENT_VERSION, version, "<>");
			reporter.trace("Component %s is v1.1", map);
		}
		set(COMPONENT_DESCRIPTORS, descriptors);
	}

}
