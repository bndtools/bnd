package aQute.bnd.component;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.service.component.annotations.*;

import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class AnnotationReader extends ClassDataCollector {
	final static String[]			EMPTY					= new String[0];
	final static Pattern			PROPERTY_PATTERN		= Pattern
																	.compile("([^=]+(:(Boolean|Byte|Char|Short|Integer|Long|Float|Double|String))?)\\s*=(.*)");

	public static final Version	V1_1					= new Version("1.1.0");																												// "1.1.0"
	public static final Version	V1_2					= new Version("1.2.0");																												// "1.1.0"
	static Pattern					BINDDESCRIPTOR			= Pattern
																	.compile("\\(((L([^;]+);)(Ljava/util/Map;)?|Lorg/osgi/framework/ServiceReference;)\\)V");
	static Pattern					BINDMETHOD				= Pattern
																	.compile("(set|bind|add)?(.*)");

	static Pattern					ACTIVATEDESCRIPTOR		= Pattern
																	.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;))*\\)V");
	static Pattern					REFERENCEBINDDESCRIPTOR	= Pattern
																	.compile("\\(Lorg/osgi/framework/ServiceReference;\\)V");

	ComponentDef					component				= new ComponentDef();

	Clazz							clazz;
	String							interfaces[];
	String							methodDescriptor;
	String							method;
	String							className;
	int								methodAccess;
	Analyzer						analyzer;
	MultiMap<String, String>		methods					= new MultiMap<String, String>();
	String							extendsClass;

	AnnotationReader(Analyzer analyzer, Clazz clazz) {
		this.analyzer = analyzer;
		this.clazz = clazz;
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer) throws Exception {
		AnnotationReader r = new AnnotationReader(analyzer, c);

		return r.getDef(c, analyzer);
	}

	/**
	 * fixup any unbind methods To declare no unbind method, the value "-" must
	 * be used. If not specified, the name of the unbind method is derived from
	 * the name of the annotated bind method. If the annotated method name
	 * begins with set, that is replaced with unset to derive the unbind method
	 * name. If the annotated method name begins with add, that is replaced with
	 * remove to derive the unbind method name. Otherwise, un is prefixed to the
	 * annotated method name to derive the unbind method name.
	 * 
	 * @return
	 * @throws Exception
	 */
	private ComponentDef getDef(Clazz c, Analyzer analyzer) throws Exception {
		c.parseClassFileWithCollector(this);
		if (component.implementation == null)
			return null;

		while (extendsClass != null) {
			if (extendsClass.startsWith("java/"))
				break;

			Clazz ec = analyzer.findClass(extendsClass);
			if (ec == null) {
				analyzer.error("Missing super class for DS annotations: "
						+ Clazz.pathToFqn(extendsClass) + " from " + c.getFQN());
			} else {
				c.parseClassFileWithCollector(this);
			}
		}

		if (component.implementation != null) {
			for (ReferenceDef rdef : component.references.values()) {
				rdef.unbind = referredMethod(analyzer, rdef, rdef.unbind, "add(.*)", "remove$1", "(.*)",
						"un$1");
				rdef.modified = referredMethod(analyzer, rdef, rdef.modified, "(add|set)(.*)", "modified$2",
						"(.*)", "modified$1");
			}
			return component;
		} else
			return null;
	}

	/**
	 * 
	 * @param analyzer
	 * @param rdef
	 */
	protected String referredMethod(Analyzer analyzer, ReferenceDef rdef, String value,
			String... matches) {
		if (value == null) {
			String bind = rdef.bind;
			for (int i = 0; i < matches.length; i += 2) {
				Matcher m = Pattern.compile(matches[i]).matcher(bind);
				if (m.matches()) {
					value = m.replaceFirst(matches[i+1]);
					break;
				}
			}
		} else if (value.equals("-"))
			return null;

		if (methods.containsKey(value)) {
			for (String descriptor : methods.get(value)) {
				Matcher matcher = BINDDESCRIPTOR.matcher(descriptor);
				if (matcher.matches()) {
					String type = matcher.group(2);
					if (rdef.interfce.equals(Clazz.objectDescriptorToFQN(type))
							|| type.equals("Ljava/util/Map;")
							|| type.equals("Lorg/osgi/framework/ServiceReference;")) {

						return value;
					}
				}
			}
			analyzer.error(
					"A related method to %s from the reference %s has no proper prototype for class %s. Expected void %s(%s s [,Map m] | ServiceReference r)",
					rdef.bind, value, component.implementation, value, rdef.interfce);
		}
		return null;
	}

	public void annotation(Annotation annotation) {
		try {
			java.lang.annotation.Annotation a = annotation.getAnnotation();

			if (a instanceof Component)
				doComponent((Component) a, annotation);
			else if (a instanceof Activate)
				doActivate();
			else if (a instanceof Deactivate)
				doDeactivate();
			else if (a instanceof Modified)
				doModified();
			else if (a instanceof Reference)
				doReference((Reference) a, annotation);

		} catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	/**
	 * 
	 */
	protected void doDeactivate() {
		if (!ACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches())
			analyzer.error(
					"Deactivate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, methodDescriptor);
		else {
			component.deactivate = method;
		}
	}

	/**
	 * 
	 */
	protected void doModified() {
		if (!ACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches())
			analyzer.error(
					"Modified method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, methodDescriptor);
		else {
			component.modified = method;
		}
	}
	/**
	 * @param annotation
	 * @throws Exception
	 */
	protected void doReference(Reference reference, Annotation raw) throws Exception {
		ReferenceDef def = new ReferenceDef();
		def.name = reference.name();

		if (def.name == null) {
			Matcher m = BINDMETHOD.matcher(method);
			if (m.matches()) {
				def.name = m.group(2);
			} else {
				def.name = method;
			}
		}

		def.unbind = reference.unbind();
		def.bind = method;

		def.interfce = raw.get("service");
		if (def.interfce != null) {
			def.interfce = Clazz.objectDescriptorToFQN(def.interfce);
		} else {
			// We have to find the type of the current method to
			// link it to the referenced service.
			Matcher m = BINDDESCRIPTOR.matcher(methodDescriptor);
			if (m.matches()) {
				def.interfce = Clazz.internalToFqn(m.group(3));
			} else
				throw new IllegalArgumentException(
						"Cannot detect the type of a Component Reference from the descriptor: "
								+ methodDescriptor);
		}

		// Check if we have a target, this must be a filter
		def.target = reference.target();
		if (def.target != null) {
			Verifier.verifyFilter(def.target, 0);
		}

		if (component.references.containsKey(def.name))
			analyzer.error(
					"In component %s, multiple references with the same name: %s. Previous def: %s, this def: %s",
					component.implementation, component.references.get(def.name), def.interfce, "");
		else
			component.references.put(def.name, def);

		def.cardinality = reference.cardinality();
		def.policy = reference.policy();
	}

	/**
	 * 
	 */
	protected void doActivate() {
		if (!ACTIVATEDESCRIPTOR.matcher(methodDescriptor).matches())
			analyzer.error(
					"Activate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, methodDescriptor);
		else {
			component.activate = method;
		}
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	protected void doComponent(Component comp, Annotation annotation) throws Exception {

		// Check if we are doing a super class
		if (component.implementation != null)
			return;

		component.version = V1_1;
		component.implementation = clazz.getFQN();
		component.name = comp.name();
		component.factory = comp.factory();
		component.configurationPolicy = comp.configurationPolicy();
		if (annotation.get("enabled") != null)
			component.enabled = comp.enabled();
		if (annotation.get("factory") != null)
			component.factory = comp.factory();
		if (annotation.get("immediate") != null)
			component.immediate = comp.immediate();
		if (annotation.get("servicefactory") != null)
			component.servicefactory = comp.servicefactory();

		String properties[] = comp.properties();
		if (properties != null)
			for (String entry : properties)
				component.properties.add(entry);

		doProperties(comp.property());
		Object [] x = annotation.get("service");
		
		if (x == null) {
			// Use the found interfaces, but convert from internal to
			// fqn.
			if (interfaces != null) {
				List<String> result = new ArrayList<String>();
				for (int i = 0; i < interfaces.length; i++) {
					if (!interfaces[i].equals("scala/ScalaObject"))
						result.add(Clazz.internalToFqn(interfaces[i]));
				}
				component.service = result.toArray(EMPTY);
			}
		} else {
			// We have explicit interfaces set
			component.service= new String[x.length];
			for (int i = 0; i < x.length; i++) {
				component.service[i] = Clazz.objectDescriptorToFQN(x[i].toString());
			}
		}

	}

	/**
	 * Parse the properties
	 */

	private void doProperties(String[] properties) {
		if (properties != null) {
			for (String p : properties) {
				Matcher m = PROPERTY_PATTERN.matcher(p);

				if (m.matches()) {
					String key = m.group(1);
					String value = m.group(4);
					component.property.add(key, value);
				} else
					throw new IllegalArgumentException("Malformed property '" + p
							+ "' on component: " + className);
			}
		}
	}

	/**
	 * Are called during class parsing
	 */

	@Override public void classBegin(int access, String name) {
		className = name;
	}

	@Override public void implementsInterfaces(String[] interfaces) {
		this.interfaces = interfaces;
	}

	@Override public void method(int access, String name, String descriptor) {
		if (Modifier.isPrivate(access) || Modifier.isAbstract(access) || Modifier.isStatic(access))
			return;

		this.method = name;
		this.methodDescriptor = descriptor;
		this.methodAccess = access;
		methods.add(name, descriptor);
	}

	@Override public void extendsClass(String name) {
		this.extendsClass = name;
	}

}
