package aQute.bnd.component;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.service.component.annotations.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.version.*;
import aQute.lib.collections.*;

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
																.compile("\\s*([^=\\s:]+)\\s*(?::\\s*(Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(.*)");

	public static final Version	V1_0					= new Version("1.0.0");																												// "1.0.0"
	public static final Version	V1_1					= new Version("1.1.0");																												// "1.1.0"
	public static final Version	V1_2					= new Version("1.2.0");																												// "1.2.0"
	public static final Version	V1_3					= new Version("1.3.0");																												// "1.3.0"

	public static final String FELIX_1_2				= "http://felix.apache.org/xmlns/scr/v1.2.0-felix";
	
	static Pattern				BINDNAME				= Pattern.compile("(set|add|bind)?(.*)");
	
	static Pattern				BINDDESCRIPTORDS10			= Pattern
																.compile("\\(L(((org/osgi/framework/ServiceReference)|(org/osgi/framework/ServiceObjects)|(java/util/Map))|([^;]+));\\)(V|(Ljava/util/Map;))");
	static Pattern				BINDDESCRIPTORDS11			= Pattern
																.compile("\\(L([^;]+);(Ljava/util/Map;)?\\)(V|(Ljava/util/Map;))");

	//includes support for felix extensions
	static Pattern				BINDDESCRIPTORDS13			= Pattern
																.compile("\\(((Lorg/osgi/framework/ServiceReference;)|(Lorg/osgi/framework/ServiceObjects;)|(Ljava/util/Map;)|(L([^;]+);))+\\)(V|(Ljava/util/Map;))");

	static Pattern				LIFECYCLEDESCRIPTORDS10		= Pattern
																.compile("\\((Lorg/osgi/service/component/ComponentContext;)\\)(V|(Ljava/util/Map;))");
	static Pattern				LIFECYCLEDESCRIPTORDS11		= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;))*\\)(V|(Ljava/util/Map;))");
	static Pattern				LIFECYCLEDESCRIPTORDS13		= Pattern
																.compile("\\((L([^;]+);)*\\)(V|(Ljava/util/Map;))");

	static Pattern				DEACTIVATEDESCRIPTORDS11	= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;)|(Ljava/lang/Integer;)|(I))*\\)(V|(Ljava/util/Map;))");
	static Pattern				DEACTIVATEDESCRIPTORDS13	= Pattern
																.compile("\\(((L([^;]+);)|(I))*\\)(V|(Ljava/util/Map;))");

	ComponentDef				component				= new ComponentDef();

	Clazz						clazz;
	TypeRef						interfaces[];
	MethodDef					method;
	TypeRef						className;
	Analyzer					analyzer;
	MultiMap<String,String>		methods					= new MultiMap<String,String>();
	TypeRef						extendsClass;
	final boolean						inherit;
	boolean						baseclass				= true;
	
	final boolean						felixExtensions;

	AnnotationReader(Analyzer analyzer, Clazz clazz, boolean inherit, boolean felixExtensions) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.inherit = inherit;
		this.felixExtensions = felixExtensions;
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer) throws Exception {
		boolean inherit = Processor.isTrue(analyzer.getProperty("-dsannotations-inherit"));
		boolean felixExtensions = Processor.isTrue(analyzer.getProperty("-ds-felix-extensions"));
		AnnotationReader r = new AnnotationReader(analyzer, c, inherit, felixExtensions);
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
			if (rdef.bind != null) {
				rdef.unbind = referredMethod(analyzer, rdef, rdef.unbind, "add(.*)", "remove$1", "(.*)", "un$1");
				rdef.updated = referredMethod(analyzer, rdef, rdef.updated, "(add|set|bind)(.*)", "updated$2", "(.*)",
						"updated$1");
				if (rdef.policy == ReferencePolicy.DYNAMIC && rdef.unbind == null)
					analyzer.error("In component %s, reference %s is dynamic but has no unbind method.",
							component.name, rdef.name);
			}
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
				String service = determineReferenceType(descriptor, rdef, rdef.service, rdef.scope);
				if (service != null)
					return value;
			}
			analyzer.error(
					"None of the methods related method to %s in the class %s named %s for service type %s have an acceptable signature. The descriptors found are: %s",
					rdef.bind, component.implementation, value, rdef.service, methods);
		}
		return null;
	}

	@Override
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
	protected void doActivate() {
		String methodDescriptor = method.getDescriptor().toString();
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS10.matcher(methodDescriptor);
		if ("activate".equals(method.getName()) && m.matches()) {
			component.activate = method.getName();
			hasMapReturnType = m.group(3) != null;
		} else {
			m = LIFECYCLEDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				component.activate = method.getName();	
				component.updateVersion(V1_1);
				hasMapReturnType = m.group(6) != null;
			} else {
				m = LIFECYCLEDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					component.activate = method.getName();	
					component.updateVersion(V1_3);
					hasMapReturnType = m.group(4) != null;

				} else 
					analyzer.error(
							"Activate method for %s descriptor %s is not acceptable.",
							clazz, method.getDescriptor());
			}
		}
		checkMapReturnType(hasMapReturnType);

	}

	/**
	 * 
	 */
	protected void doDeactivate() {
		String methodDescriptor = method.getDescriptor().toString();
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS10.matcher(methodDescriptor);
		if ( "deactivate".equals(method.getName()) && m.matches()) {
			component.deactivate = method.getName();			
			hasMapReturnType = m.group(3) != null;
		} else {
			m = DEACTIVATEDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				component.deactivate = method.getName();
				component.updateVersion(V1_1);
				hasMapReturnType = m.group(8) != null;
			} else {
				m = DEACTIVATEDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					component.deactivate = method.getName();
					component.updateVersion(V1_3);
					hasMapReturnType = m.group(6) != null;
				} else
					analyzer.error(
							"Deactivate method for %s descriptor %s is not acceptable.",
							clazz, method.getDescriptor());
			}
		}
		checkMapReturnType(hasMapReturnType);
	}

	/**
	 * 
	 */
	protected void doModified() {
		String methodDescriptor = method.getDescriptor().toString();
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS11.matcher(methodDescriptor);
		if (m.matches()) {
			component.modified = method.getName();
			component.updateVersion(V1_1);
			hasMapReturnType = m.group(6) != null;
		} else {
			m = LIFECYCLEDESCRIPTORDS13.matcher(methodDescriptor);
			if (m.matches()) {
				component.modified = method.getName();
				component.updateVersion(V1_3);
				hasMapReturnType = m.group(4) != null;
			} else

				analyzer.error(
						"Modified method for %s descriptor %s is not acceptable.",
						clazz, method.getDescriptor());
		}
		checkMapReturnType(hasMapReturnType);
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	protected void doReference(Reference reference, Annotation raw) throws Exception {
		ReferenceDef def = new ReferenceDef();
		def.name = reference.name();


		if ( ! (method.isProtected() || method.isPublic()))
			def.updateVersion(V1_1);

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

		String annoService = raw.get("service");
		if (annoService != null) 
			annoService = Clazz.objectDescriptorToFQN(annoService);
		ReferenceScope scope = reference.scope();

		String service = determineReferenceType(method.getDescriptor().toString(), def, annoService, scope);
		
		def.service = service;
		if (service == null)
			analyzer.error(
					"In component %s, method %s,  cannot recognize the signature of the descriptor: %s",
					component.name, def.name, method.getDescriptor());

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
		def.scope = reference.scope();
	}

	private String determineReferenceType(String methodDescriptor, ReferenceDef def, String annoService, ReferenceScope scope) {
		String inferredService = null;
		boolean hasMapReturnType;
		// We have to find the type of the current method to
		// link it to the referenced service.
		Matcher m = BINDDESCRIPTORDS10.matcher(methodDescriptor);
		if (m.matches()) {
			inferredService = Descriptors.binaryToFQN(m.group(1));
			if (m.group(3) == null && noMatch(annoService, inferredService)) { //ServiceReference is always OK, match is always OK
				if (m.group(6) == null) {
					def.updateVersion(V1_3); // single arg, Map or ServiceObjects, and it's not the service type, so we must be V3.
				} //if the type is specified it may still not match as it could be a superclass of the specified service.
			}
			hasMapReturnType = m.group(8) != null;
		} else {
			m = BINDDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				inferredService = Descriptors.binaryToFQN(m.group(1));
				def.updateVersion(V1_1);
				hasMapReturnType = m.group(4) != null;
			} else {
				m = BINDDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					inferredService = m.group(6);
					if (inferredService != null)
						inferredService = Descriptors.binaryToFQN(inferredService);
					def.updateVersion(V1_3);
					if (!ReferenceScope.PROTOTYPE.equals(scope) && m.group(3) != null) {
						analyzer.error(
								"In component %s, to use ServiceObjects the scope must be 'prototype'",
								component.implementation, "");				
					}
					hasMapReturnType = m.group(8) != null;
				} else { 
//					analyzer.error(
//							"In component %s, cannot determine the type of a Component Reference from the descriptor: %s",
//							component.implementation, methodDescriptor);
					return null;
				}
			}
		}

		checkMapReturnType(hasMapReturnType);
		String service = annoService;
		if (service == null) 
			service = inferredService;
		return service;
	}

	private void checkMapReturnType(boolean hasMapReturnType) {
		if (hasMapReturnType) {
			if (!felixExtensions) {
				analyzer.error(
						"In component %s, to use a return type of Map you must specify -ds-felix-extensions",
						component.implementation, "");
			}
			//TODO rethink how this is signalled.
			if (component.xmlns == null) {
				component.xmlns = FELIX_1_2;
			}

		}
	}

	/**
	 * 
	 * @param annoService
	 * @param inferredService
	 * @return true if the inferred service is a non-parameter object because it differs from the specified service type.
	 */
	private boolean noMatch(String annoService, String inferredService) {
		if (annoService == null)
			return false;
		return !annoService.equals(inferredService);
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	protected void doComponent(Component comp, Annotation annotation) throws Exception {

		// Check if we are doing a super class
		if (component.implementation != null)
			return;

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
			component.scope = comp.servicefactory()? ServiceScope.BUNDLE: ServiceScope.SINGLETON;
		if (annotation.get("scope") != null && comp.scope() != ServiceScope.DEFAULT) {
			component.scope = comp.scope();
			if (comp.scope() == ServiceScope.PROTOTYPE) {
				component.updateVersion(V1_3);
			}
		}

		if (annotation.get("configurationPid") != null) {
			component.configurationPid = comp.configurationPid();
			component.updateVersion(V1_2);
		}

		if (annotation.get("xmlns") != null)
			component.xmlns = comp.xmlns();

		String properties[] = comp.properties();
		if (properties != null)
			for (String entry : properties) {
				if (entry.contains("=")) {
					analyzer.error(
							"Found an = sign in an OSGi DS Component annotation on %s. In the bnd annotation "
									+ "this is an actual property but in the OSGi, this element must refer to a path with Java properties. "
									+ "However, found a path with an '=' sign which looks like a mixup (%s) with the 'property' element.",
							clazz, entry);
				}
				component.properties.add(entry);
			}

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
		Object[] refAnnotations = annotation.get("reference");
		if (refAnnotations != null) {
			for (Object o: refAnnotations) {
				Annotation refAnnotation = (Annotation)o;
				LookupReference ref = refAnnotation.getAnnotation();
				ReferenceDef refdef = new ReferenceDef();
				refdef.name = ref.name();
				refdef.service = analyzer.getTypeRef((String) refAnnotation.get("service")).getFQN();
				refdef.cardinality = ref.cardinality();
				refdef.policy = ref.policy();
				refdef.policyOption = ref.policyOption();
				refdef.target = ref.target();
				refdef.scope = ref.scope();
				component.references.put(refdef.name, refdef);
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
					String type = m.group(2);
					if ( type != null)
						key += ":" + type;
					
					String value = m.group(3);
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
