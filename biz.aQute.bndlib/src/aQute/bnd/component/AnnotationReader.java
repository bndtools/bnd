package aQute.bnd.component;

import static aQute.bnd.osgi.Clazz.QUERY.ANNOTATED;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.CollectionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.component.DSAnnotations.Options;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError.ErrorType;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.collections.MultiMap;

/**
 * Processes spec DS annotations into xml.
 */
public class AnnotationReader extends ClassDataCollector {
	private static final Logger			logger						= LoggerFactory.getLogger(AnnotationReader.class);

	public static final Version			V1_0						= new Version("1.0.0");																										// "1.0.0"
	public static final Version			V1_1						= new Version("1.1.0");																										// "1.1.0"
	public static final Version			V1_2						= new Version("1.2.0");																										// "1.2.0"
	public static final Version			V1_3						= new Version("1.3.0");																										// "1.3.0"
	public static final Version			V1_4						= new Version("1.4.0");																										// "1.3.0"

	private static final Pattern				BINDNAME				= Pattern
		.compile("(?:set|add|bind)?(?<name>.*)");

	/**
	 * Felix SCR allows Map return type. See
	 * {@link #checkMapReturnType(DeclarativeServicesAnnotationError)}.
	 */
	private static final String					RETURNTYPE				= "(?:V|L(?<return>java/util/Map);)";
	private static final Pattern				BINDDESCRIPTOR			= Pattern.compile("\\("
		+ "L(?:(?<inferrer>org/osgi/framework/ServiceReference|org/osgi/service/component/ComponentServiceObjects)|java/util/Map|(?<service>[^;]+));(?:(?<ds10>)|(?<ds11>Ljava/util/Map;)|(?<ds13>.+))"
		+ "\\)"																																															//
		+ RETURNTYPE);
	private static final Pattern				ACTIVATEDESCRIPTOR		= Pattern.compile("\\((?:"
		+ "(?<ds10>Lorg/osgi/service/component/ComponentContext;)"
		+ "|(?<ds11>(?:L(?:org/osgi/service/component/ComponentContext|org/osgi/framework/BundleContext|java/util/Map);)*)"
		+ "|(?<ds13>(?:L(?:[^;]+);)*)"
		+ ")\\)"																																													//
		+ RETURNTYPE);
	private static final Pattern				DEACTIVATEDESCRIPTOR	= Pattern.compile("\\((?:"
		+ "(?<ds10>Lorg/osgi/service/component/ComponentContext;)"
		+ "|(?<ds11>(?:L(?:org/osgi/service/component/ComponentContext|org/osgi/framework/BundleContext|java/util/Map|java/lang/Integer);|I)*)"
		+ "|(?<ds13>(?:L(?:[^;]+);|I)*)"																																							//
		+ ")\\)"
		+ RETURNTYPE);
	private static final Pattern				ACTIVATIONOBJECTS		= Pattern.compile(
		"L(?:org/osgi/service/component/ComponentContext|org/osgi/framework/BundleContext|java/util/Map|(?<propertytype>[^;]+));");
	private static final Pattern				DEACTIVATIONOBJECTS		= Pattern.compile(
		"L(?:org/osgi/service/component/ComponentContext|org/osgi/framework/BundleContext|java/util/Map|java/lang/Integer|(?<propertytype>[^;]+));|I");

	static final Pattern						IDENTIFIERTOPROPERTY		= Pattern
		.compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");

	private static final Pattern				SIGNATURE_SPLIT			= Pattern.compile("[<;>]");

	// We avoid using the XXX.class.getName() because it breaks the launcher
	// tests when run in gradle. This is because gradle uses the bin/
	// folder, not the bndlib jar which packages the ds annotations
	private static final Instruction			COMPONENT_INSTR				= new Instruction(
		"org.osgi.service.component.annotations.Component");
	private static final Instruction			COMPONENT_PROPERTY_INSTR	= new Instruction(
		"org.osgi.service.component.annotations.ComponentPropertyType");

	private final static Map<String, Class<?>>	wrappers;

	static {
		Map<String, Class<?>> map = new HashMap<>();
		map.put("boolean", Boolean.class);
		map.put("byte", Byte.class);
		map.put("short", Short.class);
		map.put("char", Character.class);
		map.put("int", Integer.class);
		map.put("long", Long.class);
		map.put("float", Float.class);
		map.put("double", Double.class);
		wrappers = Collections.unmodifiableMap(map);
	}

	ComponentDef											component;

	Clazz													clazz;
	TypeRef[]												interfaces;
	FieldDef												member;
	TypeRef													className;
	Analyzer												analyzer;
	MultiMap<String, Clazz.MethodDef>						methods					= new MultiMap<>();
	TypeRef													extendsClass;
	boolean													baseclass				= true;
	final EnumSet<Options>									options;

	final Map<FieldDef, ReferenceDef>						referencesByMember		= new HashMap<>();

	final XMLAttributeFinder								finder;

	Map<String, List<DeclarativeServicesAnnotationError>>	mismatchedAnnotations	= new HashMap<>();
	private int												componentPropertyTypeCount	= 0;

	AnnotationReader(Analyzer analyzer, Clazz clazz, EnumSet<Options> options, XMLAttributeFinder finder,
		Version minVersion) {
		this.analyzer = requireNonNull(analyzer);
		this.clazz = clazz;
		this.options = options;
		this.finder = finder;
		this.component = new ComponentDef(analyzer, finder, minVersion);
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer, EnumSet<Options> options,
		XMLAttributeFinder finder, Version minVersion) throws Exception {
		AnnotationReader r = new AnnotationReader(analyzer, c, options, finder, minVersion);
		return r.getDef();
	}

	private ComponentDef getDef() throws Exception {
		if (clazz.isEnum() || clazz.isInterface() || clazz.isAnnotation()) {
			// These types cannot be components so don't bother scanning them

			if (clazz.is(ANNOTATED, COMPONENT_INSTR, analyzer)) {
				analyzer
					.error("The type %s is not a class and therfore not suitable for the @Component annotation",
						clazz.getFQN())
					.details(
						new DeclarativeServicesAnnotationError(clazz.getFQN(), null, ErrorType.INVALID_COMPONENT_TYPE));
			}
			return null;
		}

		if (!clazz.is(ANNOTATED, COMPONENT_INSTR, analyzer)) {
			// This class is not annotated with @Component and so not suitable
			// for processing
			return null;
		}

		clazz.parseClassFileWithCollector(this);
		if (component.implementation == null)
			return null;

		if (options.contains(Options.inherit)) {
			baseclass = false;
			while (extendsClass != null) {
				if (extendsClass.isJava())
					break;

				Clazz ec = analyzer.findClass(extendsClass);
				if (ec == null) {
					analyzer
						.error("Missing super class for DS annotations: %s from %s", extendsClass, clazz.getClassName())
						.details(new DeclarativeServicesAnnotationError(className.getFQN(), null, null,
							ErrorType.UNABLE_TO_LOCATE_SUPER_CLASS));
					break;
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
					analyzer
						.error("In component class %s, reference %s is dynamic but has no unbind method.",
							className.getFQN(), rdef.name)
						.details(getDetails(rdef, ErrorType.DYNAMIC_REFERENCE_WITHOUT_UNBIND));
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
				Matcher m = Pattern.compile(matches[i])
					.matcher(bind);
				if (m.matches()) {
					value = m.replaceFirst(matches[i + 1]);
					break;
				}
			}
		} else if (value.equals("-"))
			return null;

		if (methods.containsKey(value)) {
			for (Clazz.MethodDef method : methods.get(value)) {
				String service = determineReferenceType(method, rdef, rdef.service, null);
				if (service != null) {
					return value;
				}
			}
			analyzer.warning(
				"None of the methods related to '%s' in the class '%s' named '%s' for service type '%s' have an acceptable signature. The descriptors found are:",
				rdef.bind, component.implementation, value, rdef.service);
			// We make this a separate loop because we shouldn't add warnings
			// until we know that there was no match
			// We need to include the method name in the warning or it may be
			// ignored as duplicate (from another non-match)
			for (Clazz.MethodDef method : methods.get(value)) {
				analyzer.warning("  methodname: %s descriptor: %s", value, method.getDescriptor()
					.toString())
					.details(getDetails(rdef, ErrorType.UNSET_OR_MODIFY_WITH_WRONG_SIGNATURE));
			}
		}
		return null;
	}

	@Override
	public void classEnd() throws Exception {
		member = null;
	}

	@Override
	public void memberEnd() {
		member = null;
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
			else if (a instanceof Designate)
				doDesignate((Designate) a);
			else if (annotation.getName()
				.getFQN()
				.startsWith("aQute.bnd.annotation.component"))
				handleMixedUsageError(annotation);
			else {
				handlePossibleComponentPropertyAnnotation(annotation);

				XMLAttribute xmlAttr = finder.getXMLAttribute(annotation);
				if (xmlAttr != null) {
					doXmlAttribute(annotation, xmlAttr);
				}
			}
		} catch (Exception e) {
			analyzer.exception(e, "During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	private void handleMixedUsageError(Annotation annotation) throws Exception {
		DeclarativeServicesAnnotationError errorDetails;

		String fqn = annotation.getName()
			.getFQN();

		switch (annotation.getElementType()) {
			case METHOD :
				errorDetails = new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(),
					member.getDescriptor()
						.toString(),
					ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
				break;
			case FIELD :
				errorDetails = new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(),
					ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
				break;
			default :
				errorDetails = new DeclarativeServicesAnnotationError(className.getFQN(), null,
					ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
		}
		List<DeclarativeServicesAnnotationError> errors = mismatchedAnnotations.get(fqn);
		if (errors == null) {
			errors = new ArrayList<>();
			mismatchedAnnotations.put(fqn, errors);
		}
		errors.add(errorDetails);
	}

	private void handlePossibleComponentPropertyAnnotation(Annotation annotation) throws Exception {
		DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(), null,
			null, ErrorType.COMPONENT_PROPERTY_ANNOTATION_PROBLEM);

		try {
			Clazz clazz = analyzer.findClass(annotation.getName());

			if (clazz == null) {
				analyzer.warning(
					"Unable to determine whether the annotation %s applied to type %s is a component property type as it is not on the project build path. If this annotation is a component property type then it must be present on the build path in order to be processed",
					annotation.getName()
						.getFQN(),
					className.getFQN())
					.details(details);
				return;
			}

			if (clazz.is(ANNOTATED, COMPONENT_PROPERTY_INSTR, analyzer)) {
				String propertyDefKey = String.format(ComponentDef.PROPERTYDEF_ANNOTATIONFORMAT,
					++componentPropertyTypeCount);
				clazz.parseClassFileWithCollector(
					new ComponentPropertyTypeDataCollector(propertyDefKey, annotation, details));
			} else {
				logger.debug(
					"The annotation {} on component type {} will not be used for properties as the annotation is not annotated with @ComponentPropertyType",
						clazz.getFQN(), className.getFQN());
				return;
			}
		} catch (Exception e) {
			analyzer
				.exception(e, "An error occurred when attempting to process annotation %s, applied to component %s",
					annotation.getName()
						.getFQN(),
					className.getFQN())
				.details(details);
		}
	}

	private void doXmlAttribute(Annotation annotation, XMLAttribute xmlAttr) {
		// make sure doc is namespace aware, since we are adding namespaced
		// attributes.
		component.updateVersion(V1_1);
		if (member == null)
			component.addExtensionAttribute(xmlAttr, annotation);
		else {
			ReferenceDef ref = referencesByMember.get(member);
			if (ref == null) {
				ref = new ReferenceDef(finder);
				referencesByMember.put(member, ref);
			}
			ref.addExtensionAttribute(xmlAttr, annotation);
		}
	}

	protected void doDesignate(Designate a) {
		if (a.factory() && component.configurationPolicy == null)
			component.configurationPolicy = ConfigurationPolicy.REQUIRE;
	}

	/**
	 * 
	 */
	protected void doActivate() {
		if (member instanceof MethodDef) {
			String memberDescriptor = member.getDescriptor()
				.toString();
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), memberDescriptor, ErrorType.ACTIVATE_SIGNATURE_ERROR);
			Matcher m = ACTIVATEDESCRIPTOR.matcher(memberDescriptor);
			if (m.matches()) {
				component.activate = member.getName();
				if (m.group("ds10") != null) {
					if (!member.isProtected() || !"activate".equals(member.getName())) {
						component.updateVersion(V1_1);
					}
				} else if (m.group("ds11") != null) {
					component.updateVersion(V1_1);
				} else if (m.group("ds13") != null) {
					component.updateVersion(V1_3);
					processActivationObjects(ComponentDef.PROPERTYDEF_ACTIVATEFORMAT, ACTIVATIONOBJECTS,
						memberDescriptor,
						details);
				}
				if (m.group("return") != null) {
					checkMapReturnType(details);
				}
			} else {
				analyzer
					.error("Activate method for %s.%s%s is not acceptable.", details.className, details.methodName,
						details.methodSignature)
					.details(details);
			}
		} else if (member instanceof FieldDef) {
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), ErrorType.ACTIVATE_SIGNATURE_ERROR);
			// TODO handle activation-fields
			analyzer.error("Activate annotation on a field %s.%s", details.className, details.fieldName)
				.details(details);
		}
	}

	/**
	 * 
	 */
	protected void doDeactivate() {
		if (member instanceof MethodDef) {
			String memberDescriptor = member.getDescriptor()
				.toString();
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), memberDescriptor, ErrorType.DEACTIVATE_SIGNATURE_ERROR);
			Matcher m = DEACTIVATEDESCRIPTOR.matcher(memberDescriptor);
			if (m.matches()) {
				component.deactivate = member.getName();
				if (m.group("ds10") != null) {
					if (!member.isProtected() || !"deactivate".equals(member.getName())) {
						component.updateVersion(V1_1);
					}
				} else if (m.group("ds11") != null) {
					component.updateVersion(V1_1);
				} else if (m.group("ds13") != null) {
					component.updateVersion(V1_3);
					processActivationObjects(ComponentDef.PROPERTYDEF_DEACTIVATEFORMAT, DEACTIVATIONOBJECTS,
						memberDescriptor,
						details);
				}
				if (m.group("return") != null) {
					checkMapReturnType(details);
				}
			} else {
				analyzer
					.error("Deactivate method for %s.%s%s is not acceptable.", details.className, details.methodName,
						details.methodSignature)
					.details(details);
			}
		} else if (member instanceof FieldDef) {
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), ErrorType.DEACTIVATE_SIGNATURE_ERROR);
			analyzer.error("Deactivate annotation on a field %s.%s", details.className, details.fieldName)
				.details(details);
		}
	}

	/**
	 * 
	 */
	protected void doModified() {
		if (member instanceof MethodDef) {
			String memberDescriptor = member.getDescriptor()
				.toString();
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), memberDescriptor, ErrorType.MODIFIED_SIGNATURE_ERROR);
			Matcher m = ACTIVATEDESCRIPTOR.matcher(memberDescriptor);
			if (m.matches()) {
				component.modified = member.getName();
				if (m.group("ds10") != null) {
					component.updateVersion(V1_1);
				} else if (m.group("ds11") != null) {
					component.updateVersion(V1_1);
				} else if (m.group("ds13") != null) {
					component.updateVersion(V1_3);
					processActivationObjects(ComponentDef.PROPERTYDEF_MODIFIEDFORMAT, ACTIVATIONOBJECTS,
						memberDescriptor,
						details);
				}
				if (m.group("return") != null) {
					checkMapReturnType(details);
				}
			} else {
				analyzer
					.error("Modified method for %s.%s%s is not acceptable.", details.className, details.methodName,
						details.methodSignature)
					.details(details);
			}
		} else if (member instanceof FieldDef) {
			DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
				member.getName(), ErrorType.MODIFIED_SIGNATURE_ERROR);
			analyzer.error("Modified annotation on a field %s.%s", details.className, details.fieldName)
				.details(details);
		}
	}

	/**
	 * look for annotation activation objects and extract properties from them
	 */
	private void processActivationObjects(String propertyDefKeyFormat, Pattern activationObjects,
		String methodDescriptor,
		DeclarativeServicesAnnotationError details) {
		Matcher m = activationObjects.matcher(methodDescriptor);
		int activationObjectCount = 0;

		while (m.find()) {
			String type = m.group("propertytype");
			if (type != null) {
				TypeRef typeRef = analyzer.getTypeRef(type);
				try {
					Clazz clazz = analyzer.findClass(typeRef);
					if (clazz.isAnnotation()) {
						String propertyDefKey = String.format(propertyDefKeyFormat, ++activationObjectCount);
						clazz.parseClassFileWithCollector(
							new ComponentPropertyTypeDataCollector(propertyDefKey, methodDescriptor, details));
					} else if (clazz.isInterface() && options.contains(Options.felixExtensions)) {
						// ok
					} else {
						analyzer
							.error("Non annotation type for activation object with descriptor %s,  type %s",
								methodDescriptor, type)
							.details(details);
					}
				} catch (Exception e) {
					analyzer
						.exception(e,
							"Exception looking at annotation type for activation object with descriptor %s,  type %s",
							methodDescriptor, type)
						.details(details);
				}
			}
		}
	}

	private final class ComponentPropertyTypeDataCollector extends ClassDataCollector {
		private final String								propertyDefKey;
		private final String								methodDescriptor;
		private final DeclarativeServicesAnnotationError	details;
		private final PropertyDef							propertyDef		= new PropertyDef(analyzer);
		private int											hasNoDefault	= 0;
		private boolean										hasValue		= false;
		private boolean										hasMethods		= false;
		private FieldDef									prefixField		= null;
		private TypeRef										typeRef			= null;

		ComponentPropertyTypeDataCollector(String propertyDefKey, String methodDescriptor,
			DeclarativeServicesAnnotationError details) {
			this.propertyDefKey = requireNonNull(propertyDefKey);
			this.methodDescriptor = methodDescriptor;
			this.details = details;
		}

		ComponentPropertyTypeDataCollector(String propertyDefKey, Annotation componentPropertyAnnotation,
			DeclarativeServicesAnnotationError details) {
			this.propertyDefKey = requireNonNull(propertyDefKey);
			// Component Property annotations added in 1.4, but they just map to
			// normal DS properties, so there's not really a need to require DS
			// 1.4. Therefore we just leave the required version as is

			this.methodDescriptor = null;
			this.details = details;

			// Add in the defined attributes
			for (String key : componentPropertyAnnotation.keySet()) {
				Object value = componentPropertyAnnotation.get(key);
				handleValue(key, value, value instanceof TypeRef, null);
			}
		}

		@Override
		public void classBegin(int access, TypeRef name) {
			typeRef = name;
		}

		@Override
		public void field(FieldDef defined) {
			if (defined.isStatic() && defined.getName()
				.equals("PREFIX_")) {
				prefixField = defined;
			}
		}

		@Override
		public void method(MethodDef defined) {
			if (defined.isStatic()) {
				return;
			}
			hasMethods = true;
			if (defined.getName()
				.equals("value")) {
				hasValue = true;
			} else {
				hasNoDefault++;
			}
		}

		@Override
		public void annotationDefault(MethodDef defined, Object value) {
			String name = defined.getName();
			if (!name.equals("value")) {
				hasNoDefault--;
			}
			// check type, exit with warning if annotation
			// or annotation array
			boolean isClass = false;
			Class<?> typeClass = null;
			TypeRef type = defined.getType()
				.getClassRef();
			if (!type.isPrimitive()) {
				if (type == analyzer.getTypeRef("java/lang/Class")) {
					isClass = true;
				} else {
					try {
						Clazz r = analyzer.findClass(type);
						if (r.isAnnotation()) {
							analyzer
								.warning("Nested annotation type found in member %s, %s", name,
									type.getFQN())
								.details(details);
							return;
						}
					} catch (Exception e) {
						if (methodDescriptor != null) {
							analyzer.exception(e,
								"Exception looking at annotation type to lifecycle method with descriptor %s,  type %s",
								methodDescriptor, type)
								.details(details);
						} else {
							analyzer
								.exception(e, "Exception looking at annotation %s applied to type %s", typeRef.getFQN(),
									className.getFQN())
								.details(details);
						}
					}
				}
			} else {
				typeClass = wrappers.get(type.getFQN());
			}

			if ((value != null) && !propertyDef.containsKey(name)) {
				handleValue(name, value, isClass, typeClass);
			}
		}

		@Override
		public void classEnd() throws Exception {
			String prefix;
			if (prefixField != null) {
				Object c = prefixField.getConstant();
				if (prefixField.isFinal() && (prefixField.getType() == analyzer.getTypeRef("java/lang/String"))
					&& (c instanceof String)) {
					prefix = (String) c;
		
					// If we have a method descriptor then this is an injected
					// component property type and the prefix must be processed
					// and understood by DS 1.4. Otherwise bnd handles the
					// property prefix mapping transparently here and DS doesn't
					// need to care
					if (methodDescriptor != null) {
						component.updateVersion(V1_4);
					}
				} else {
					prefix = null;
					analyzer.warning(
						"Field PREFIX_ in %s is not a static final String field with a compile-time constant value: %s",
						typeRef.getFQN(), c)
						.details(details);
				}
			} else {
				prefix = null;
			}
		
			if (!hasMethods) {
				// This is a marker annotation so treat it like it is a single
				// element annotation with a value of Boolean.TRUE
				hasValue = true;
				handleValue("value", Boolean.TRUE, false, Boolean.class);
			}
		
			String singleElementAnnotation;
			if (hasValue && (hasNoDefault == 0)) {
				StringBuilder sb = new StringBuilder(typeRef.getShorterName());
				boolean lastLowerCase = false;
				for (int i = 0; i < sb.length(); i++) {
					char c = sb.charAt(i);
					if (Character.isUpperCase(c)) {
						sb.setCharAt(i, Character.toLowerCase(c));
						if (lastLowerCase) {
							sb.insert(i++, '.');
						}
						lastLowerCase = false;
					} else {
						lastLowerCase = Character.isLowerCase(c);
					}
				}
				singleElementAnnotation = sb.toString();
				// If we have a method descriptor then this is an injected
				// component property type and the single elementness must
				// be processed and understood by DS 1.4. Otherwise bnd handles
				// the property mapping transparently here and DS doesn't
				// need to care
				if (methodDescriptor != null) {
					component.updateVersion(V1_4);
				}
			} else {
				singleElementAnnotation = null;
			}
		
			if (!propertyDef.isEmpty()) {
				component.propertyDefs.put(propertyDefKey, propertyDef.copy(key -> {
					if ((singleElementAnnotation != null) && key.equals("value")) {
						key = singleElementAnnotation;
					} else {
						key = identifierToPropertyName(key);
					}
					if (prefix != null) {
						key = prefix + key;
					}
					return key;
				}));
			}
		}

		/**
		 * This method relies on {@link ConcreteRef#toString()} returning
		 * {@link TypeRef#getFQN()}
		 */
		private void handleValue(String name, Object value, boolean isClass, Class<?> typeClass) {
			if (value.getClass()
				.isArray()) {
				Object[] array = (Object[]) value;
				switch (array.length) {
					case 0 : {
						String type = valueType(typeClass, "", isClass);
						propertyDef.setProperty(name, type, new ArrayList<>());
						break;
					}
					case 1 : {
						value = array[0];
						String type = valueType(typeClass, value, isClass);
						List<String> values = new ArrayList<>(2);
						values.add(value.toString());
						// To make sure the output is an array, we must make
						// sure there is more than one entry
						values.add(PropertyDef.MARKER);
						propertyDef.setProperty(name, type, values);
						break;
					}
					default : {
						String type = valueType(typeClass, array[0], isClass);
						List<String> values = Stream.of(array)
							.map(Object::toString)
							.collect(toList());
						propertyDef.setProperty(name, type, values);
						break;
					}
				}
			} else {
				String type = valueType(typeClass, value, isClass);
				propertyDef.setProperty(name, type, value.toString());
			}
		}

		private String valueType(Class<?> typeClass, Object value, boolean isClass) {
			if (typeClass != null) {
				return typeClass.getSimpleName();
			}
			if (isClass) {
				return "String";
			}
			return value.getClass()
					.getSimpleName();
		}

		private String identifierToPropertyName(String name) {
			Matcher m = IDENTIFIERTOPROPERTY.matcher(name);
			if (!m.find()) {
				return name;
			}
			StringBuffer b = new StringBuffer();
			do {
				switch (m.group()) {
					case "__" : // __ to _
						m.appendReplacement(b, "_");
						break;
					case "_" : // _ to .
						m.appendReplacement(b, ".");
						break;
					case "$_$" : // $_$ to -
						m.appendReplacement(b, "-");
						break;
					case "$$" : // $$ to $
						m.appendReplacement(b, "\\$");
						break;
					case "$" : // $ removed
						m.appendReplacement(b, "");
						break;
					default : // unknown!
						m.appendReplacement(b, m.group());
						analyzer.error("unknown mapping %s in property name %s", m.group(), name);
						break;
				}
			} while (m.find());
			m.appendTail(b);
			return b.toString();
		}
	}

	/**
	 * @param reference @Reference proxy backed by annotation.
	 * @param annotation @Reference contents
	 * @throws Exception
	 */
	protected void doReference(Reference reference, Annotation annotation) throws Exception {
		ReferenceDef def;
		if (member == null)
			def = new ReferenceDef(finder);
		else if (referencesByMember.containsKey(member))
			def = referencesByMember.get(member);
		else {
			def = new ReferenceDef(finder);
			referencesByMember.put(member, def);
		}
		def.className = className.getFQN();
		if (annotation.get("name") != null)
			def.name = reference.name();
		if (annotation.get("bind") != null)
			def.bind = reference.bind();
		if (annotation.get("unbind") != null)
			def.unbind = reference.unbind();
		if (annotation.get("updated") != null)
			def.updated = reference.updated();
		if (annotation.get("field") != null)
			def.field = reference.field();
		if (annotation.get("fieldOption") != null)
			def.fieldOption = reference.fieldOption();
		if (annotation.get("cardinality") != null)
			def.cardinality = reference.cardinality();
		if (annotation.get("policy") != null)
			def.policy = reference.policy();
		if (annotation.get("policyOption") != null)
			def.policyOption = reference.policyOption();
		if (annotation.get("scope") != null)
			def.scope = reference.scope();

		// Check if we have a target, this must be a filter
		if (annotation.get("target") != null)
			def.target = reference.target();

		DeclarativeServicesAnnotationError details = getDetails(def, ErrorType.REFERENCE);

		if (def.target != null) {
			String error = Verifier.validateFilter(def.target);
			if (error != null)
				analyzer.error("Invalid target filter %s for %s: %s", def.target, def.name, error)
					.details(getDetails(def, ErrorType.INVALID_TARGET_FILTER));
		}

		String annoService = null;
		TypeRef annoServiceTR = annotation.get("service");
		if (annoServiceTR != null)
			annoService = annoServiceTR.getFQN();

		if (member != null) {
			if (member instanceof MethodDef) {
				def.bindDescriptor = member.getDescriptor()
					.toString();
				def.bind = member.getName();
				if (def.name == null) {
					Matcher m = BINDNAME.matcher(member.getName());
					if (m.matches())
						def.name = m.group("name");
					else
						analyzer.error("Invalid name for bind method %s", member.getName())
							.details(getDetails(def, ErrorType.INVALID_REFERENCE_BIND_METHOD_NAME));
				}

				def.service = determineReferenceType((MethodDef) member, def, annoService, member.getSignature());

				if (def.service == null)
					analyzer.error("In component %s, method %s,  cannot recognize the signature of the descriptor: %s",
						component.effectiveName(), def.name, member.getDescriptor());

			} else if (member instanceof FieldDef) {
				def.updateVersion(V1_3);
				def.field = member.getName();
				if (def.name == null) {
					def.name = def.field;
				}
				if (def.policy == null && member.isVolatile()) {
					def.policy = ReferencePolicy.DYNAMIC;
				}

				String sig = member.getSignature();
				if (sig == null) {
					// no generics, the descriptor will be the class name.
					sig = member.getDescriptor()
						.toString();
				}
				String[] sigs = SIGNATURE_SPLIT.split(sig);
				int sigLength = sigs.length;
				int index = 0;
				boolean isCollection = false;
				if ("Ljava/util/Collection".equals(sigs[index]) || "Ljava/util/List".equals(sigs[index])) {
					index++;
					isCollection = true;
				}
				// Along with determining the CollectionType, the following
				// code positions index to read the service type.
				CollectionType collectionType = null;
				if (sufficientGenerics(index, sigLength, def, sig)) {
					if ("Lorg/osgi/framework/ServiceReference".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							collectionType = CollectionType.REFERENCE;
						}
					} else if ("Lorg/osgi/service/component/ComponentServiceObjects".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							collectionType = CollectionType.SERVICEOBJECTS;
						}
					} else if ("Ljava/util/Map".equals(sigs[index])) {
						if (sufficientGenerics(index++, sigLength, def, sig)) {
							collectionType = CollectionType.PROPERTIES;
						}
					} else if ("Ljava/util/Map$Entry".equals(sigs[index])
						&& sufficientGenerics(index++ + 5, sigLength, def, sig)) {
						if ("Ljava/util/Map".equals(sigs[index++]) && "Ljava/lang/String".equals(sigs[index++])) {
							if ("Ljava/lang/Object".equals(sigs[index]) || "+Ljava/lang/Object".equals(sigs[index])) {
								collectionType = CollectionType.TUPLE;
								index += 3; // ;>;
							} else if ("*".equals(sigs[index])) {
								collectionType = CollectionType.TUPLE;
								index += 2; // >;
							} else {
								index = sigLength; // no idea what service might
													// be.
							}
						}
					} else {
						collectionType = CollectionType.SERVICE;
					}
				}
				if (isCollection) {
					if (def.cardinality == null) {
						def.cardinality = ReferenceCardinality.MULTIPLE;
					}
					if (annotation.get("collectionType") != null) {
						def.collectionType = reference.collectionType();
					} else {
						def.collectionType = collectionType;
					}
				}
				if (def.policy == ReferencePolicy.DYNAMIC && (def.cardinality == ReferenceCardinality.MULTIPLE
					|| def.cardinality == ReferenceCardinality.AT_LEAST_ONE) && member.isFinal()) {
					if (def.fieldOption == FieldOption.REPLACE) {
						analyzer.error(
							"In component %s, collection type field: %s is final and dynamic but marked with 'replace' fieldOption. Changing this to 'update'.",
							className, def.field)
							.details(getDetails(def, ErrorType.DYNAMIC_FINAL_FIELD_WITH_REPLACE));
					}
					def.fieldOption = FieldOption.UPDATE;
				}
				if (annoService == null && index < sigs.length) {
					annoService = Descriptors.binaryToFQN(sigs[index].substring(1));
				}
				def.service = annoService;
				if (def.service == null) {
					analyzer
						.error("In component %s, method %s,  cannot recognize the signature of the descriptor: %s",
							component.effectiveName(), def.name, member.getDescriptor())
						.details(details);
				}

			} // end field
		} else {// not a member
			def.service = annoService;
			if (def.name == null) {
				analyzer
					.error("Name must be supplied for a @Reference specified in the @Component annotation. Service: %s",
						def.service)
					.details(getDetails(def, ErrorType.MISSING_REFERENCE_NAME));
				return;
			}
		}

		if (component.references.containsKey(def.name))
			analyzer
				.error("In component %s, multiple references with the same name: %s. Previous def: %s, this def: %s",
					className, component.references.get(def.name), def.service, "")
				.details(getDetails(def, ErrorType.MULTIPLE_REFERENCES_SAME_NAME));
		else
			component.references.put(def.name, def);

	}

	private DeclarativeServicesAnnotationError getDetails(ReferenceDef def, ErrorType type) {
		if (def == null)
			return null;

		return new DeclarativeServicesAnnotationError(className.getFQN(), def.bind, def.bindDescriptor, type);
	}

	private boolean sufficientGenerics(int index, int sigLength, ReferenceDef def, String sig) {
		if (index + 1 > sigLength) {
			analyzer.error(
				"In component %s, method %s,  signature: %s does not have sufficient generic type information",
				component.effectiveName(), def.name, sig);
			return false;
		}
		return true;
	}

	private String determineReferenceType(MethodDef method, ReferenceDef def, String annoService,
		String signature) {
		// We have to find the type of the current method to
		// link it to the referenced service.
		String methodDescriptor = method.getDescriptor()
			.toString();
		Matcher m = BINDDESCRIPTOR.matcher(methodDescriptor);
		if (!m.matches()) {
			return null;
		}

		Version minVersion = null;
		if (m.group("ds10") != null) {
			if (!method.isProtected()) {
				minVersion = V1_1;
			}
		} else if (m.group("ds11") != null) {
			minVersion = V1_1;
		} else if (m.group("ds13") != null) {
			minVersion = V1_3;
		}

		String inferredService = m.group("service");
		if (inferredService != null) {
			inferredService = Descriptors.binaryToFQN(inferredService);
		} else {
			String inferrer = m.group("inferrer");
			if (inferrer != null) {
				// ServiceReference or ComponentServiceObjects
				if ((annoService == null) || !annoService.equals(Descriptors.binaryToFQN(inferrer))) {
					if (inferrer.equals("org/osgi/service/component/ComponentServiceObjects")) {
						// first argument using type supported starting in 1.3
						minVersion = V1_3;
					}
					if (signature != null) {
						inferrer = "L" + inferrer + "<";
						int start = signature.indexOf(inferrer);
						if (start > -1) {
							String[] sigs = SIGNATURE_SPLIT.split(signature.substring(start + inferrer.length()));
							if (sigs.length > 0) {
								String sig = sigs[0];
								switch (sig.charAt(0)) {
									case '*' :
									case '-' :
										inferredService = Object.class.getName();
										break;
									case '+' :
										inferredService = Descriptors.binaryToFQN(sig.substring(2));
										break;
									default :
										inferredService = Descriptors.binaryToFQN(sig.substring(1));
										break;
								}
							}
						}
					}
				}
			} else {
				// Map
				if ((annoService == null) || !annoService.equals("java.util.Map")) {
					// first argument using type supported starting in 1.3
					minVersion = V1_3;
				}
			}
		}

		// if the type is specified it may still not match as it could
		// be a superclass of the specified service.
		if (!analyzer.assignable(annoService, inferredService)) {
			return null;
		}
		if (m.group("return") != null) {
			DeclarativeServicesAnnotationError details = getDetails(def, ErrorType.REFERENCE);
			checkMapReturnType(details);
		}
		if (minVersion != null) {
			def.updateVersion(minVersion);
		}
		return annoService != null ? annoService : inferredService;
	}

	private void checkMapReturnType(DeclarativeServicesAnnotationError details) {
		if (!options.contains(Options.felixExtensions)) {
			analyzer.error(
				"In component %s, to use a return type of Map you must specify the -dsannotations-options felixExtensions flag "
					+ " and use a felix extension attribute or explicitly specify the appropriate xmlns.",
					component.implementation, "")
				.details(details);
		}
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	protected void doComponent(Component comp, Annotation annotation) throws Exception {

		String componentName = annotation.containsKey("name") ? comp.name() : className.getFQN();

		if (!mismatchedAnnotations.isEmpty()) {
			for (Entry<String, List<DeclarativeServicesAnnotationError>> e : mismatchedAnnotations.entrySet()) {
				for (DeclarativeServicesAnnotationError errorDetails : e.getValue()) {
					if (errorDetails.fieldName != null) {
						analyzer.error(
							"The DS component %s uses standard annotations to declare it as a component, but also uses the bnd DS annotation: %s on field %s. It is an error to mix these two types of annotations",
							componentName, e.getKey(), errorDetails.fieldName)
							.details(errorDetails);
					} else if (errorDetails.methodName != null) {
						analyzer.error(
							"The DS component %s uses standard annotations to declare it as a component, but also uses the bnd DS annotation: %s on method %s with signature %s. It is an error to mix these two types of annotations",
							componentName, e.getKey(), errorDetails.methodName, errorDetails.methodSignature)
							.details(errorDetails);
					} else {
						analyzer.error(
							"The DS component %s uses standard annotations to declare it as a component, but also uses the bnd DS annotation: %s. It is an error to mix these two types of annotations",
							componentName, e.getKey())
							.details(errorDetails);
					}
				}
			}
			return;
		}

		// Check if we are doing a super class
		if (component.implementation != null)
			return;

		component.implementation = clazz.getClassName();
		component.name = componentName;
		if (annotation.get("factory") != null)
			component.factory = comp.factory();
		if (annotation.get("configurationPolicy") != null)
			component.configurationPolicy = comp.configurationPolicy();
		if (annotation.get("enabled") != null)
			component.enabled = comp.enabled();
		if (annotation.get("factory") != null)
			component.factory = comp.factory();
		if (annotation.get("immediate") != null)
			component.immediate = comp.immediate();
		if (annotation.get("servicefactory") != null)
			component.scope = comp.servicefactory() ? ServiceScope.BUNDLE : ServiceScope.SINGLETON;
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

		component.property.setTypedProperty(className, comp.property());
		component.factoryProperty.setTypedProperty(className, comp.factoryProperty());

		component.properties.addProperties(comp.properties());
		component.factoryProperties.addProperties(comp.factoryProperties());

		Object[] declaredServices = annotation.get("service");
		if (declaredServices == null) {
			// Use the found interfaces, but convert from internal to
			// fqn.
			if (interfaces != null) {
				TypeRef scalaObject = analyzer.getTypeRef("scala/ScalaObject");
				component.service = Stream.of(interfaces)
					.filter(i -> !Objects.equals(i, scalaObject))
					.toArray(TypeRef[]::new);
			}
		} else {
			// We have explicit interfaces set
			component.service = Stream.of(declaredServices)
				.map(TypeRef.class::cast)
				.peek(typeRef -> {
					try {
						Clazz service = analyzer.findClass(typeRef);
						if (!analyzer.assignable(clazz, service)) {
							analyzer
								.error("Class %s is not assignable to specified service %s", clazz.getFQN(),
									typeRef.getFQN())
								.details(new DeclarativeServicesAnnotationError(className.getFQN(), null, null,
									ErrorType.INCOMPATIBLE_SERVICE));
						}
					} catch (Exception e) {
						analyzer
							.exception(e,
								"An error occurred when attempting to process service %s, applied to component %s",
								typeRef.getFQN(), className.getFQN())
							.details(new DeclarativeServicesAnnotationError(className.getFQN(), null, null,
								ErrorType.INCOMPATIBLE_SERVICE));
					}
				})
				.toArray(TypeRef[]::new);
		}

		// make sure reference processing knows this is a Reference in Component
		member = null;
		Object[] refAnnotations = annotation.get("reference");
		if (refAnnotations != null) {
			for (Object o : refAnnotations) {
				Annotation refAnnotation = (Annotation) o;
				Reference ref = refAnnotation.getAnnotation();
				doReference(ref, refAnnotation);
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
	public void method(MethodDef method) {
		if (method.isAbstract() || method.isStatic() || method.isBridge())
			return;

		if (!baseclass && method.isPrivate())
			return;

		this.member = method;
		methods.add(method.getName(), method);
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
