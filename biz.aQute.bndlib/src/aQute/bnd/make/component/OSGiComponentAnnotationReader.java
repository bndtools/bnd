package aQute.bnd.make.component;

import java.util.*;
import java.util.regex.*;

import org.osgi.service.component.annotations.*;

import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class OSGiComponentAnnotationReader extends ClassDataCollector {
	final static String[]			EMPTY					= new String[0];
	final static Pattern			PROPERTY_PATTERN		= Pattern
																	.compile("([^=]+(:(Boolean|Byte|Char|Short|Integer|Long|Float|Double|String))?)\\s*=(.*)");

	private static final Version	V1_1					= new Version("1.1.0");																												// "1.1.0"
	static Pattern					BINDDESCRIPTOR			= Pattern
																	.compile("\\(L([^;]*);(Ljava/util/Map;|Lorg/osgi/framework/ServiceReference;)*\\)V");
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
	Set<String>						methods					= new HashSet<String>();

	OSGiComponentAnnotationReader(Analyzer analyzer, Clazz clazz) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		component.version = V1_1;
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer) throws Exception {
		OSGiComponentAnnotationReader r = new OSGiComponentAnnotationReader(analyzer, c);
		c.parseClassFileWithCollector(r);
		return r.component;
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
				component.modified = method;
			else if (a instanceof Reference)
				doReference((Reference) a, annotation);

		} catch (Exception e) {
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
				def.interfce = Clazz.internalToFqn(m.group(1));
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
					"In component %s, Multiple references with the same name: %s. Previous def: %s, this def: %s",
					def.name, component.references.get(def.name), def.interfce, "");
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
		component.name = comp.name();
		component.factory = comp.factory();
		component.configurationPolicy = comp.configurationPolicy();
		component.enabled = comp.enabled();
		component.factory = comp.factory();
		component.immediate = comp.immediate();
		component.servicefactory = comp.servicefactory();

		doProperties(comp.property());

		component.service = annotation.get("service");
		if (component.service == null) {
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
			for (int i = 0; i < component.service.length; i++) {
				component.service[i] = Clazz.objectDescriptorToFQN(component.service[i]);
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
		this.method = name;
		this.methodDescriptor = descriptor;
		this.methodAccess = access;
		methods.add(name);
	}
}
