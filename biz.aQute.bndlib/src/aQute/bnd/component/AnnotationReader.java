package aQute.bnd.component;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.service.component.annotations.*;

import aQute.bnd.component.error.*;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError.ErrorType;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.FieldDef;
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
																	.compile("\\(L(((org/osgi/framework/ServiceReference)|(org/osgi/service/component/ComponentServiceObjects)|(java/util/Map\\$Entry)|(java/util/Map))|([^;]+));\\)(V|(Ljava/util/Map;))");
	static Pattern				BINDDESCRIPTORDS11			= Pattern
																.compile("\\(L([^;]+);(Ljava/util/Map;)?\\)(V|(Ljava/util/Map;))");

	//includes support for felix extensions
	static Pattern				BINDDESCRIPTORDS13			= Pattern
																	.compile("\\(((Lorg/osgi/framework/ServiceReference;)|(Lorg/osgi/service/component/ComponentServiceObjects;)|(Ljava/util/Map;)|(Ljava/util/Map\\$Entry;)|(L([^;]+);))+\\)(V|(Ljava/util/Map;))");

	static Pattern				LIFECYCLEDESCRIPTORDS10		= Pattern
																.compile("\\((Lorg/osgi/service/component/ComponentContext;)\\)(V|(Ljava/util/Map;))");
	static Pattern				LIFECYCLEDESCRIPTORDS11		= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;))*\\)(V|(Ljava/util/Map;))");
	static Pattern				LIFECYCLEDESCRIPTORDS13		= Pattern
																.compile("\\((L([^;]+);)*\\)(V|(Ljava/util/Map;))");
	static Pattern				LIFECYCLEARGUMENT			= Pattern
																.compile("((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;)|(L([^;]+);))");	

	static Pattern				IDENTIFIERTOPROPERTY		= Pattern
																.compile("(__)|(_)|(\\$\\$)|(\\$)");

	static Pattern				DEACTIVATEDESCRIPTORDS11	= Pattern
																.compile("\\(((Lorg/osgi/service/component/ComponentContext;)|(Lorg/osgi/framework/BundleContext;)|(Ljava/util/Map;)|(Ljava/lang/Integer;)|(I))*\\)(V|(Ljava/util/Map;))");
	static Pattern				DEACTIVATEDESCRIPTORDS13	= Pattern
																.compile("\\(((L([^;]+);)|(I))*\\)(V|(Ljava/util/Map;))");

	ComponentDef				component				= new ComponentDef();

	Clazz						clazz;
	TypeRef						interfaces[];
	FieldDef					member;
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
							+ clazz.getClassName()).details(new DeclarativeServicesAnnotationError(className.getFQN(), null, null, 
									ErrorType.UNABLE_TO_LOCATE_SUPER_CLASS));
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
					analyzer.error("In component %s, reference %s is dynamic but has no unbind method.", component.name, rdef.name)
					.details(new DeclarativeServicesAnnotationError(className.getFQN(), rdef.bind, rdef.bindDescriptor, 
							ErrorType.DYNAMIC_REFERENCE_WITHOUT_UNBIND));
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
				String service = determineReferenceType(descriptor, rdef, rdef.service, null);
				if (service != null)
					return value;
			}
			analyzer.warning(
					"None of the methods related method to %s in the class %s named %s for service type %s have an acceptable signature. The descriptors found are:",
					rdef.bind, component.implementation, value, rdef.service, methods);
			//We make this a separate loop because we shouldn't add warnings until we know that there was no match
			for(String descriptor : methods.get(value)) {
				analyzer.warning(
					"  descriptor: %s", descriptor).details(
							new DeclarativeServicesAnnotationError(className.getFQN(), value, descriptor, 
							ErrorType.UNSET_OR_MODIFY_WITH_WRONG_SIGNATURE));
			}
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
		String methodDescriptor = member.getDescriptor().toString();
		if (!(member instanceof MethodDef)) {
			analyzer.error(
					"Activate annotation on a field",
					clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
							ErrorType.ACTIVATE_SIGNATURE_ERROR));
			return;
		}
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS10.matcher(methodDescriptor);
		if ("activate".equals(member.getName()) && m.matches()) {
			component.activate = member.getName();
			hasMapReturnType = m.group(3) != null;
		} else {
			m = LIFECYCLEDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				component.activate = member.getName();	
				component.updateVersion(V1_1);
				hasMapReturnType = m.group(6) != null;
			} else {
				m = LIFECYCLEDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					component.activate = member.getName();	
					component.updateVersion(V1_3);
					hasMapReturnType = m.group(4) != null;
					processAnnotationArguments(methodDescriptor);
				} else 
					analyzer.error(
							"Activate method for %s descriptor %s is not acceptable.",
							clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
									ErrorType.ACTIVATE_SIGNATURE_ERROR));
			}
		}
		checkMapReturnType(hasMapReturnType);

	}


	/**
	 * 
	 */
	protected void doDeactivate() {
		String methodDescriptor = member.getDescriptor().toString();
		if (!(member instanceof MethodDef)) {
			analyzer.error(
					"Deactivate annotation on a field",
					clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
							ErrorType.DEACTIVATE_SIGNATURE_ERROR));
			return;
		}
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS10.matcher(methodDescriptor);
		if ( "deactivate".equals(member.getName()) && m.matches()) {
			component.deactivate = member.getName();			
			hasMapReturnType = m.group(3) != null;
		} else {
			m = DEACTIVATEDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				component.deactivate = member.getName();
				component.updateVersion(V1_1);
				hasMapReturnType = m.group(8) != null;
			} else {
				m = DEACTIVATEDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					component.deactivate = member.getName();
					component.updateVersion(V1_3);
					hasMapReturnType = m.group(6) != null;
					processAnnotationArguments(methodDescriptor);
				} else
					analyzer.error(
							"Deactivate method for %s descriptor %s is not acceptable.",
							clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
									ErrorType.DEACTIVATE_SIGNATURE_ERROR));
			}
		}
		checkMapReturnType(hasMapReturnType);
	}

	/**
	 * 
	 */
	protected void doModified() {
		String methodDescriptor = member.getDescriptor().toString();
		if (!(member instanceof MethodDef)) {
			analyzer.error(
					"Modified annotation on a field",
					clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
							ErrorType.MODIFIED_SIGNATURE_ERROR));
			return;
		}
		boolean hasMapReturnType = false;
		Matcher m = LIFECYCLEDESCRIPTORDS11.matcher(methodDescriptor);
		if (m.matches()) {
			component.modified = member.getName();
			component.updateVersion(V1_1);
			hasMapReturnType = m.group(6) != null;
		} else {
			m = LIFECYCLEDESCRIPTORDS13.matcher(methodDescriptor);
			if (m.matches()) {
				component.modified = member.getName();
				component.updateVersion(V1_3);
				hasMapReturnType = m.group(4) != null;
				processAnnotationArguments(methodDescriptor);
			} else

				analyzer.error(
						"Modified method for %s descriptor %s is not acceptable.",
						clazz, member.getDescriptor()).details(new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(), methodDescriptor, 
								ErrorType.MODIFIED_SIGNATURE_ERROR));
		}
		checkMapReturnType(hasMapReturnType);
	}

	/**
	 * look for annotation arguments and extract properties from them
	 * @param methodDescriptor
	 */
	private void processAnnotationArguments(final String methodDescriptor) {
		Matcher m = LIFECYCLEARGUMENT.matcher(methodDescriptor);
		while (m.find()) {
			String type = m.group(6);
			if (type != null) {
				TypeRef typeRef = analyzer.getTypeRef(type);
				try {
					Clazz clazz = analyzer.findClass(typeRef);
					if (clazz.isAnnotation()) {
						final MultiMap<String, String> props = new MultiMap<String, String>();
						clazz.parseClassFileWithCollector(new ClassDataCollector() {

							@Override
							public void annotationDefault(Clazz.MethodDef defined) {
								Object value = defined.getConstant();
								//check type, exit with warning if annotation or annotation array
								boolean isClass = false;
								boolean isCharacter = false;
								TypeRef type = defined.getType().getClassRef();
								if (!type.isPrimitive()) {
									if (Class.class.getName().equals(type.getFQN())) {
										isClass = true;
									} else {
										try {
											Clazz r = analyzer.findClass(type);
											if (r.isAnnotation()) {
												analyzer.warning("Nested annotation type found in field % s, %s", defined.getName(), type.getFQN());
												return;
											}
										} catch (Exception e) {
											analyzer.error("Exception looking at annotation type to lifecycle method with descriptor %s,  type %s", e, methodDescriptor, type);
										}
									}
								} else if ("char".equals(type.getFQN())) {
									isCharacter = true;
								}
								if (value != null) {
									String name = identifierToPropertyName(defined.getName());
									if (value.getClass().isArray()) {
										//add element individually
										for (int i = 0; i< Array.getLength(value); i++) {
											Object element = Array.get(value, i);
											valueToProperty(name, element, isClass, isCharacter);
										}
									} else
										valueToProperty(name, value, isClass, isCharacter);
								}
							}

							private void valueToProperty(String name, Object value, boolean isClass, boolean isCharacter) {
								if (isClass) {
									value = Clazz.objectDescriptorToFQN((String) value);
								}
								Class<?> typeClass = isCharacter? Character.class: value.getClass();
								//enums already come out as the enum name, no processing needed.
								String type = typeClass.getSimpleName();
								component.propertyType.put(name, type);
								props.add(name, value.toString());
							}

							private String identifierToPropertyName(String name) {
								Matcher m = IDENTIFIERTOPROPERTY.matcher(name);
								StringBuffer b = new StringBuffer();
								while (m.find()) {
									String replace = "";
									if (m.group(1) != null) // __ to _
										replace = "_";
									else if (m.group(2) != null) // _ to .
										replace = ".";
									else if (m.group(3) != null) // $$ to $
										replace = "\\$";
									//group 4 $ removed.
									m.appendReplacement(b, replace); 
								}
								m.appendTail(b);
								return b.toString();
							}

						});
						component.property.putAll(props);
					} else if (clazz.isInterface() && felixExtensions) {
						//ok
					} else {
						analyzer.error("Non annotation argument to lifecycle method with descriptor %s,  type %s", methodDescriptor, type);
					}
				}
				catch (Exception e) {
					analyzer.error("Exception looking at annotation argument to lifecycle method with descriptor %s,  type %s", e, methodDescriptor, type);
				}
			}
		}
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	protected void doReference(Reference reference, Annotation raw) throws Exception {
		ReferenceDef def = new ReferenceDef();
		def.className = className.getFQN();
		def.name = reference.name();
		def.bind = reference.bind();
		def.unbind = reference.unbind();
		def.updated = reference.updated();
		def.field = reference.field();
		def.fieldOption = reference.fieldOption();
		def.cardinality = reference.cardinality();
		def.policy = reference.policy();
		def.policyOption = reference.policyOption();
		def.scope = reference.scope();

		// Check if we have a target, this must be a filter
		def.target = reference.target();

		if (def.target != null) {
			String error = Verifier.validateFilter(def.target);
			if (error != null)
				analyzer.error("Invalid target filter %s for %s: %s", def.target, def.name, error).details(
						new DeclarativeServicesAnnotationError(className.getFQN(), def.bind, null,
								ErrorType.INVALID_TARGET_FILTER));
		}

		String annoService = raw.get("service");
		if (annoService != null) 
			annoService = Clazz.objectDescriptorToFQN(annoService);

		if (member != null) {
			if (member instanceof MethodDef) {
				def.bindDescriptor = member.getDescriptor().toString();
				if (!(member.isProtected() || member.isPublic()))
					def.updateVersion(V1_1);
				def.bind = member.getName();
				if (def.name == null) {
					Matcher m = BINDNAME.matcher(member.getName());
					if (m.matches())
						def.name = m.group(2);
					else
						analyzer.error("Invalid name for bind method %s", member.getName()).details(
								new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(),
										def.bindDescriptor, ErrorType.INVALID_REFERENCE_BIND_METHOD_NAME));
				}

				def.service = determineReferenceType(def.bindDescriptor, def, annoService, member.getSignature());

				if (def.service == null)
					analyzer.error(
							"In component %s, method %s,  cannot recognize the signature of the descriptor: %s",
							component.name, def.name, member.getDescriptor());

			} else if (member instanceof FieldDef) {
				def.updateVersion(V1_3);
				def.field = member.getName();
				if (def.name == null)
					def.name = def.field;

				String sig = member.getSignature();
				if (sig == null)
					// no generics, the descriptor will be the class name.
					sig = member.getDescriptor().toString();
				String[] sigs = sig.split("[<;>]");
				int sigLength = sigs.length;
				int index = 0;
				FieldCollectionType fieldCollectionType = null;
				boolean isCollection = false;
				if ("Ljava/util/Collection".equals(sigs[index]) || "Ljava/util/List".equals(sigs[index])) {
					index++;
					isCollection = true;
				}
				if (sufficientGenerics(index, sigLength, def, sig)) {
					if ("Lorg/osgi/framework/ServiceReference".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							fieldCollectionType = FieldCollectionType.reference;
						}
					} else if ("Lorg/osgi/framework/ServiceObjects".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							fieldCollectionType = FieldCollectionType.serviceobjects;
						}
					} else if ("Ljava/util/Map".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							fieldCollectionType = FieldCollectionType.properties;
						}
					} else if ("Ljava/util/Map$Entry".equals(sigs[index])
							&& sufficientGenerics(index++ + 5, sigLength, def, sig)) {
						if ("Ljava/util/Map".equals(sigs[index++]) && "Ljava/lang/String".equals(sigs[index++])
								&& "Ljava/lang/Object".equals(sigs[index++])) {
							fieldCollectionType = FieldCollectionType.tuple;
							index += 2; // ;>;
						}
					} else {
						fieldCollectionType = FieldCollectionType.service;
					}
				}
				if (isCollection)
					def.fieldCollectionType = fieldCollectionType;
				if (annoService == null && index <= sigs.length) {
					annoService = sigs[index].substring(1).replace('/', '.');

				}
				def.service = annoService;
				if (def.service == null)
					analyzer.error("In component %s, method %s,  cannot recognize the signature of the descriptor: %s",
							component.name, def.name, member.getDescriptor());

			} // end field
		} else {// not a member
			def.service = annoService;
		}

		if (component.references.containsKey(def.name))
			analyzer.error(
					"In component %s, multiple references with the same name: %s. Previous def: %s, this def: %s",
					component.implementation, component.references.get(def.name), def.service, "").details(
					new DeclarativeServicesAnnotationError(className.getFQN(), null, null,
							ErrorType.MULTIPLE_REFERENCES_SAME_NAME));
		else
			component.references.put(def.name, def);

	}

	private boolean sufficientGenerics(int index, int sigLength, ReferenceDef def, String sig) {
		if (index + 1 > sigLength) {
			analyzer.error(
					"In component %s, method %s,  signature: %s does not have sufficient generic type information",
					component.name, def.name, sig);
			return false;
		}
		return true;
	}

	private String determineReferenceType(String methodDescriptor, ReferenceDef def, String annoService, String signature) {
		String inferredService = null;
		String plainType = null;
		boolean hasMapReturnType;
		// We have to find the type of the current method to
		// link it to the referenced service.
		Matcher m = BINDDESCRIPTORDS10.matcher(methodDescriptor);
		if (m.matches()) {
			inferredService = Descriptors.binaryToFQN(m.group(1));
			if (m.group(3) == null && noMatch(annoService, inferredService)) { //ServiceReference is always OK, match is always OK
				if (m.group(7) == null) {
					def.updateVersion(V1_3); // single arg, Map or ServiceObjects, and it's not the service type, so we must be V3.
				} //if the type is specified it may still not match as it could be a superclass of the specified service.
			}
			if (annoService == null)
				if (m.group(3) != null) {
					plainType = "Lorg/osgi/framework/ServiceReference<";
					inferredService = null;
				} else if (m.group(4) != null) {
					plainType = "Lorg/osgi/service/component/ComponentServiceObjects<";
					inferredService = null;
				} else if (m.group(5) != null) {
					plainType = "Ljava/util/Map$Entry<Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;";
					inferredService = null;
				}
			hasMapReturnType = m.group(9) != null;
		} else {
			m = BINDDESCRIPTORDS11.matcher(methodDescriptor);
			if (m.matches()) {
				inferredService = Descriptors.binaryToFQN(m.group(1));
				def.updateVersion(V1_1);
				hasMapReturnType = m.group(4) != null;
			} else {
				m = BINDDESCRIPTORDS13.matcher(methodDescriptor);
				if (m.matches()) {
					inferredService = m.group(7);
					if (inferredService != null)
						inferredService = Descriptors.binaryToFQN(inferredService);
					def.updateVersion(V1_3);
					if (!ReferenceScope.PROTOTYPE.equals(def.scope) && m.group(3) != null) {
						analyzer.error(
								"In component %s, to use ServiceObjects the scope must be 'prototype'",
								component.implementation, "");				
					}
					if (annoService == null)
						if (m.group(2) != null)
							plainType = "Lorg/osgi/framework/ServiceReference<";
						else if (m.group(3) != null)
							plainType = "Lorg/osgi/service/component/ComponentServiceObjects<";
						else if (m.group(5) != null)
							plainType = "Ljava/util/Map$Entry<Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;";

					hasMapReturnType = m.group(9) != null;
				} else { 
					return null;
				}
			}
		}

		checkMapReturnType(hasMapReturnType);
		String service = annoService;
		if (service == null) 
			service = inferredService;
		if (service == null && signature != null && plainType != null) {
			int start = signature.indexOf(plainType);
			if (start > -1) {
				start += plainType.length();
				String[] sigs = signature.substring(start).split("[<;>]");
				if (sigs.length > 0) {
					service = sigs[0].substring(1).replace('/', '.');
				}
			}
		}
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
			if (component.configurationPid.length > 1) {
				component.updateVersion(V1_3);
			} else {
				component.updateVersion(V1_2);
			}
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
							clazz, entry).details(new DeclarativeServicesAnnotationError(className.getFQN(), null, null, 
									ErrorType.COMPONENT_PROPERTIES_ERROR));
				}
				component.properties.add(entry);
			}

		doProperty(comp.property());
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
		
		//make sure reference processing knows this is a Reference in Component
		member = null;
		Object[] refAnnotations = annotation.get("reference");
		if (refAnnotations != null) {
			for (Object o: refAnnotations) {
				Annotation refAnnotation = (Annotation)o;
				Reference ref = refAnnotation.getAnnotation();
				doReference(ref, refAnnotation);
//				ReferenceDef refdef = new ReferenceDef();
//				refdef.name = ref.name();
//				refdef.bind = ref.bind();
//				refdef.updated = ref.updated();
//				refdef.unbind = ref.unbind();
//				refdef.field = ref.field();
//				refdef.fieldOption = ref.fieldOption();
//				
//				refdef.service = analyzer.getTypeRef((String) refAnnotation.get("service")).getFQN();
//				refdef.cardinality = ref.cardinality();
//				refdef.policy = ref.policy();
//				refdef.policyOption = ref.policyOption();
//				refdef.target = ref.target();
//				refdef.scope = ref.scope();
//				component.references.put(refdef.name, refdef);
			}
		}

	}

	/**
	 * Parse the properties
	 */

	private void doProperty(String[] properties) {
		if (properties != null && properties.length > 0) {
			MultiMap<String, String> props = new MultiMap<String, String>();
			for (String p : properties) {
				Matcher m = PROPERTY_PATTERN.matcher(p);

				if (m.matches()) {
					String key = m.group(1);
					String type = m.group(2);
					if ( type == null)
						type = "String";
					
					component.propertyType.put(key,  type);
					
					String value = m.group(3);
					props.add(key, value);
				} else
					analyzer.error("Malformed property '" + p + "' on component: " + className);
			}
			component.property.putAll(props);
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

		this.member = method;
		methods.add(method.getName(), method.getDescriptor().toString());
	}
	
	@Override
	public void field(FieldDef field) {
		this.member = field;
	}

	@Override
	public void extendsClass(TypeRef name) {
		this.extendsClass = name;
	}

}
