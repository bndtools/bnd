package aQute.bnd.component;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.service.component.annotations.*;

import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Clazz.MethodDef;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.libg.version.*;

/**
 * fixup any unbind methods To declare no unbind method, the value "-" must be
 * used. If not specified, the name of the unbind method is derived from the
 * name of the annotated bind method. If the annotated method name begins with
 * set, that is replaced with unset to derive the unbind method name. If the
 * annotated method name begins with add, that is replaced with remove to derive
 * the unbind method name. Otherwise, un is prefixed to the annotated method
 * name to derive the unbind method name.
 * 
 * @return
 * @throws Exception
 */
public class AnnotationReader extends ClassDataCollector {
	final static TypeRef[]		EMPTY					= new TypeRef[0];
	final static Pattern		PROPERTY_PATTERN		= Pattern
																.compile("([^=]+(:(Boolean|Byte|Char|Short|Integer|Long|Float|Double|String))?)\\s*=(.*)");

	public static final Version	V1_1					= new Version("1.1.0");																												// "1.1.0"
	public static final Version	V1_2					= new Version("1.2.0");																												// "1.1.0"
	static Pattern				BINDNAME				= Pattern.compile("(set|add|bind)?(.*)");
	static Pattern				BINDDESCRIPTOR			= Pattern
																.compile("\\(((L([^;]+);)(Ljava/util/Map;)?|Lorg/osgi/framework/ServiceReference;)\\)V");

	static Pattern				LIFECYCLEDESCRIPTOR		= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;))*\\)V");
	static Pattern				REFERENCEBINDDESCRIPTOR	= Pattern
																.compile("\\(Lorg/osgi/framework/ServiceReference;\\)V");

	ComponentDef				component				= new ComponentDef();

	Clazz						clazz;
	TypeRef						interfaces[];
	MethodDef					method;
	TypeRef						className;
	Analyzer					analyzer;
	MultiMap<String,String>		methods					= new MultiMap<String,String>();
	TypeRef						extendsClass;
	boolean						inherit;
	boolean						baseclass				= true;

	AnnotationReader(Analyzer analyzer, Clazz clazz, boolean inherit) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.inherit = inherit;
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer) throws Exception {
		boolean inherit = Processor.isTrue(analyzer.getProperty("-dsannotations-inherit"));
		AnnotationReader r = new AnnotationReader(analyzer, c, inherit);
		return r.getDef();
	}

	private ComponentDef getDef() throws Exception {
		clazz.parseClassFileWithCollector(this);
		if (component.implementation == null)
			return null;

		if (inherit) {
			baseclass = false;
			while (extendsClass != null) {
				if (extendsClass.isJava())
					break;

				Clazz ec = analyzer.findClass(extendsClass);
				if (ec == null) {
					analyzer.error("Missing super class for DS annotations: " + extendsClass + " from "
							+ clazz.getClassName());
				} else {
					ec.parseClassFileWithCollector(this);
				}
			}
		}
		for (ReferenceDef rdef : component.references.values()) {
			rdef.unbind = referredMethod(analyzer, rdef, rdef.unbind, "add(.*)", "remove$1", "(.*)", "un$1");
			rdef.updated = referredMethod(analyzer, rdef, rdef.updated, "(add|set|bind)(.*)", "updated$2", "(.*)",
					"updated$1");
		}
		return component;
	}

	/**
	 * @param analyzer
	 * @param rdef
	 */
	protected String referredMethod(Analyzer analyzer, ReferenceDef rdef, String value, String... matches) {
		if (value == null) {
			String bind = rdef.bind;
			for (int i = 0; i < matches.length; i += 2) {
				Matcher m = Pattern.compile(matches[i]).matcher(bind);
				if (m.matches()) {
					value = m.replaceFirst(matches[i + 1]);
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
					if (rdef.service.equals(Clazz.objectDescriptorToFQN(type)) || type.equals("Ljava/util/Map;")
							|| type.equals("Lorg/osgi/framework/ServiceReference;")) {

						return value;
					}
				}
			}
			analyzer.error(
					"A related method to %s from the reference %s has no proper prototype for class %s. Expected void %s(%s s [,Map m] | ServiceReference r)",
					rdef.bind, value, component.implementation, value, rdef.service);
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
		}
		catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	/**
	 * 
	 */
	protected void doDeactivate() {
		if (!LIFECYCLEDESCRIPTOR.matcher(method.getDescriptor().toString()).matches())
			analyzer.error(
					"Deactivate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, method.getDescriptor());
		else {
			component.deactivate = method.getName();
		}
	}

	/**
	 * 
	 */
	protected void doModified() {
		if (!LIFECYCLEDESCRIPTOR.matcher(method.getDescriptor().toString()).matches())
			analyzer.error(
					"Modified method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, method.getDescriptor());
		else {
			component.modified = method.getName();
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
			Matcher m = BINDNAME.matcher(method.getName());
			if (m.matches())
				def.name = m.group(2);
			else
				analyzer.error("Invalid name for bind method %s", method.getName());
		}

		def.unbind = reference.unbind();
		def.updated = reference.updated();
		def.bind = method.getName();

		def.service = raw.get("service");
		if (def.service != null) {
			def.service = Clazz.objectDescriptorToFQN(def.service);
		} else {
			// We have to find the type of the current method to
			// link it to the referenced service.
			Matcher m = BINDDESCRIPTOR.matcher(method.getDescriptor().toString());
			if (m.matches()) {
				def.service = Descriptors.binaryToFQN(m.group(3));
			} else
				throw new IllegalArgumentException(
						"Cannot detect the type of a Component Reference from the descriptor: "
								+ method.getDescriptor());
		}

		// Check if we have a target, this must be a filter
		def.target = reference.target();

		if (component.references.containsKey(def.name))
			analyzer.error(
					"In component %s, multiple references with the same name: %s. Previous def: %s, this def: %s",
					component.implementation, component.references.get(def.name), def.service, "");
		else
			component.references.put(def.name, def);

		def.cardinality = reference.cardinality();
		def.policy = reference.policy();
		def.policyOption = reference.policyOption();
	}

	/**
	 * 
	 */
	protected void doActivate() {
		if (!LIFECYCLEDESCRIPTOR.matcher(method.getDescriptor().toString()).matches())
			analyzer.error(
					"Activate method for %s does not have an acceptable prototype, only Map, ComponentContext, or BundleContext is allowed. Found: %s",
					clazz, method.getDescriptor());
		else {
			component.activate = method.getName();
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
		component.implementation = clazz.getClassName();
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

		if (annotation.get("configurationPid") != null)
			component.configurationPid = comp.configurationPid();

		if (annotation.get("xmlns") != null)
			component.xmlns = comp.xmlns();

		String properties[] = comp.properties();
		if (properties != null)
			for (String entry : properties)
				component.properties.add(entry);

		doProperties(comp.property());
		Object[] x = annotation.get("service");

		if (x == null) {
			// Use the found interfaces, but convert from internal to
			// fqn.
			if (interfaces != null) {
				List<TypeRef> result = new ArrayList<TypeRef>();
				for (int i = 0; i < interfaces.length; i++) {
					if (!interfaces[i].equals(analyzer.getTypeRef("scala/ScalaObject")))
						result.add(interfaces[i]);
				}
				component.service = result.toArray(EMPTY);
			}
		} else {
			// We have explicit interfaces set
			component.service = new TypeRef[x.length];
			for (int i = 0; i < x.length; i++) {
				String s = (String) x[i];
				TypeRef ref = analyzer.getTypeRefFromFQN(s);
				component.service[i] = ref;
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
					throw new IllegalArgumentException("Malformed property '" + p + "' on component: " + className);
			}
		}
	}

	/**
	 * Are called during class parsing
	 */

	@Override
	public void classBegin(int access, TypeRef name) {
		className = name;
	}

	@Override
	public void implementsInterfaces(TypeRef[] interfaces) {
		this.interfaces = interfaces;
	}

	@Override
	public void method(Clazz.MethodDef method) {
		int access = method.getAccess();

		if (Modifier.isAbstract(access) || Modifier.isStatic(access))
			return;

		if (!baseclass && Modifier.isPrivate(access))
			return;

		this.method = method;
		methods.add(method.getName(), method.getDescriptor().toString());
	}

	@Override
	public void extendsClass(TypeRef name) {
		this.extendsClass = name;
	}

}
