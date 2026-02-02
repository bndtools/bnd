package aQute.bnd.component;

import static aQute.bnd.osgi.Clazz.QUERY.ANNOTATED;
import static aQute.bnd.osgi.Descriptors.binaryToFQN;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.component.DSAnnotations.Options;
import aQute.bnd.component.annotations.CollectionType;
import aQute.bnd.component.annotations.Component;
import aQute.bnd.component.annotations.ConfigurationPolicy;
import aQute.bnd.component.annotations.FieldOption;
import aQute.bnd.component.annotations.Reference;
import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.component.annotations.ReferencePolicy;
import aQute.bnd.component.annotations.ServiceScope;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError.ErrorType;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MemberDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.MethodParameter;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.signatures.BaseType;
import aQute.bnd.signatures.ClassResolver;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldResolver;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodResolver;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.VoidDescriptor;
import aQute.bnd.unmodifiable.Maps;
import aQute.bnd.version.Version;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.collections.MultiMap;

/**
 * Processes spec DS annotations into xml.
 */
public class DSAnnotationReader extends ClassDataCollector {
	private static final Logger								logger						= LoggerFactory
		.getLogger(DSAnnotationReader.class);

	public static final Version								V1_0						= new Version("1.0.0");
	public static final Version								V1_1						= new Version("1.1.0");
	public static final Version								V1_2						= new Version("1.2.0");
	public static final Version								V1_3						= new Version("1.3.0");
	public static final Version								V1_4						= new Version("1.4.0");
	public static final Version								V1_5						= new Version("1.5.0");
	public static final Version								VMAX						= new Version("2.0.0");

	private static final Pattern							BINDNAME					= Pattern
		.compile("(?:set|add|bind)?(?<name>.*)");

	static final Pattern									IDENTIFIERTOPROPERTY		= Pattern
		.compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");

	// We avoid using the XXX.class.getName() because it breaks the launcher
	// tests when run in gradle. This is because gradle uses the bin/
	// folder, not the bndlib jar which packages the ds annotations
	private static final Instruction						COMPONENT_INSTR				= new Instruction(
		"org.osgi.service.component.annotations.Component");
	private static final Instruction						COMPONENT_PROPERTY_INSTR	= new Instruction(
		"org.osgi.service.component.annotations.ComponentPropertyType");

	final static Map<String, Class<?>>						wrappers					= Maps.of("boolean",
		Boolean.class, "byte", Byte.class, "short", Short.class, "char", Character.class, "int", Integer.class, "long",
		Long.class, "float", Float.class, "double", Double.class);

	private static final Entry<Pattern, String>				unbind1						= Maps
		.entry(Pattern.compile("add(.*)"), "remove$1");
	private static final Entry<Pattern, String>				unbind2						= Maps
		.entry(Pattern.compile("(.*)"), "un$1");
	private static final Entry<Pattern, String>				updated1					= Maps
		.entry(Pattern.compile("(?:add|set|bind)(.*)"), "updated$1");
	private static final Entry<Pattern, String>				updated2					= Maps
		.entry(Pattern.compile("(.*)"), "updated$1");

	private static final String								constructorArgFormat		= "$%03d";

	ComponentDef											component;

	final Clazz												clazz;
	final ClassSignature									classSig;
	TypeRef[]												interfaces;
	MemberDef												member;
	MethodSignature											methodSig;
	FieldSignature											fieldSig;
	MethodSignature											constructorSig;
	int														parameter;
	int														constructorArg;
	TypeRef													className;
	Analyzer												analyzer;
	MultiMap<String, MethodDef>								methods						= new MultiMap<>();
	TypeRef													extendsClass;
	boolean													baseclass					= true;
	final Set<Options>										options;

	final Map<Object, ReferenceDef>							referencesByTarget			= new HashMap<>();

	final XMLAttributeFinder								finder;

	Map<String, List<DeclarativeServicesAnnotationError>>	mismatchedAnnotations		= new HashMap<>();
	private int												componentPropertyTypeCount	= 0;

	DSAnnotationReader(Analyzer analyzer, Clazz clazz, Set<Options> options, XMLAttributeFinder finder,
		Version minVersion) {
		this.analyzer = requireNonNull(analyzer);
		this.clazz = clazz;
		this.options = options;
		this.finder = finder;
		this.component = new ComponentDef(analyzer, finder, minVersion);
		String signature = clazz.getClassSignature();
		classSig = analyzer.getClassSignature((signature != null) ? signature : "Ljava/lang/Object;");
	}

	public static ComponentDef getDefinition(Clazz c, Analyzer analyzer, Set<Options> options,
		XMLAttributeFinder finder, Version minVersion) throws Exception {
		DSAnnotationReader r = new DSAnnotationReader(analyzer, c, options, finder, minVersion);
		return r.getDef();
	}

	private ComponentDef getDef() throws Exception {
		if (clazz.isEnum() || clazz.isInterface() || clazz.isAnnotation()) {
			// These types cannot be components so don't bother scanning them

			if (clazz.is(ANNOTATED, COMPONENT_INSTR, analyzer)) {
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(clazz.getFQN(),
					null, ErrorType.INVALID_COMPONENT_TYPE);
				details.addError(analyzer,
					"[%s] The type is not a class and therefore not suitable for the @Component annotation",
					details.location());
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
					DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(
						className.getFQN(), null, null, ErrorType.UNABLE_TO_LOCATE_SUPER_CLASS);
					details.addError(analyzer, "[%s] Missing super class for DS annotations %s", details.location(),
						extendsClass);
					break;
				} else {
					ec.parseClassFileWithCollector(this);
				}
			}
		}
		for (ReferenceDef rdef : component.references.values()) {
			if (rdef.bind != null) {
				rdef.unbind = referredMethod(analyzer, rdef, rdef.unbind, unbind1, unbind2);
				rdef.updated = referredMethod(analyzer, rdef, rdef.updated, updated1, updated2);

				if (rdef.policy == ReferencePolicy.DYNAMIC && rdef.unbind == null) {
					addError(rdef, ErrorType.DYNAMIC_REFERENCE_WITHOUT_UNBIND,
						"In component class %s, reference %s is dynamic but has no unbind method.",
					className.getFQN(), rdef.name);
				}
			}
		}
		return component;
	}

	/**
	 * @param analyzer
	 * @param rdef
	 */
	@SafeVarargs
	private final String referredMethod(Analyzer analyzer, ReferenceDef rdef, String value,
		Entry<Pattern, String>... matches) {
		if (value == null) {
			String bind = rdef.bind;
			for (Entry<Pattern, String> match : matches) {
				Matcher m = match.getKey()
					.matcher(bind);
				if (m.matches()) {
					value = m.replaceFirst(match.getValue());
					break;
				}
			}
		} else if (value.equals("-")) {
			return null;
		}

		if (methods.containsKey(value)) {
			for (MethodDef method : methods.get(value)) {
				String service = determineMethodReferenceType(rdef, method, getMethodSignature(method), rdef.service);
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
			for (MethodDef method : methods.get(value)) {
				analyzer.warning("  methodname: %s descriptor: %s", value, method.descriptor())
					.details(getDetails(rdef, ErrorType.UNSET_OR_MODIFY_WITH_WRONG_SIGNATURE));
			}
		}
		return null;
	}

	@Override
	public void annotation(Annotation annotation) {
		if ((member != null) && ignoreMember()) {
			return; // ignore annotations on ignored members
		}

		try {
			switch (annotation.getName()
				.getFQN()) {
				case "org.osgi.service.component.annotations.Component" :
					doComponent(annotation.getAnnotation(Component.class), annotation);
					break;
				case "org.osgi.service.component.annotations.Reference" :
					doReference(annotation.getAnnotation(Reference.class), annotation);
					break;
				case "org.osgi.service.component.annotations.Activate" :
					doActivate(annotation);
					break;
				case "org.osgi.service.component.annotations.Deactivate" :
					doDeactivate(annotation);
					break;
				case "org.osgi.service.component.annotations.Modified" :
					doModified(annotation);
					break;
				case "org.osgi.service.metatype.annotations.Designate" :
					doDesignate(annotation);
					break;
				case "aQute.bnd.annotation.component.Activate" :
				case "aQute.bnd.annotation.component.Component" :
				case "aQute.bnd.annotation.component.Deactivate" :
				case "aQute.bnd.annotation.component.Modified" :
				case "aQute.bnd.annotation.component.Reference" :
					handleMixedUsageError(annotation);
					break;
				default :
					handlePossibleComponentPropertyAnnotation(annotation);

					XMLAttribute xmlAttr = finder.getXMLAttribute(annotation);
					if (xmlAttr != null) {
						doXmlAttribute(annotation, xmlAttr);
					}
					break;
			}
		} catch (Exception e) {
			analyzer.exception(e, "During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	private void handleMixedUsageError(Annotation annotation) throws Exception {
		DeclarativeServicesAnnotationError errorDetails = switch (annotation.elementType()) {
			case METHOD -> new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(),
				member.descriptor(), ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
			case FIELD -> new DeclarativeServicesAnnotationError(className.getFQN(), member.getName(),
				ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
			default -> new DeclarativeServicesAnnotationError(className.getFQN(), null,
				ErrorType.MIXED_USE_OF_DS_ANNOTATIONS_STD);
		};
		String fqn = annotation.getName()
			.getFQN();
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
					"[%s] Unable to determine whether the annotation %s is a component property type as it is not on the project build path. If this annotation is a component property type then it must be present on the build path in order to be processed",
					details.location(), annotation.getName()
						.getFQN())
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
					"The annotation {} on component {} will not be used for properties as the annotation is not annotated with @ComponentPropertyType",
					clazz.getFQN(), details.location());
				return;
			}
		} catch (Exception e) {
			analyzer
				.exception(e, "[%s] An error occurred when attempting to process annotation %s", details.location(),
					annotation.getName()
						.getFQN())
				.details(details);
		}
	}

	private void doXmlAttribute(Annotation annotation, XMLAttribute xmlAttr) {
		// make sure doc is namespace aware, since we are adding namespaced
		// attributes.
		ExtensionDef def;
		switch (annotation.elementType()) {
			case METHOD :
			case FIELD :
				def = referencesByTarget.computeIfAbsent(member, m -> new ReferenceDef(finder));
				break;
			case PARAMETER : // constructor parameter
				def = referencesByTarget.computeIfAbsent(String.format(constructorArgFormat, parameter),
					m -> new ReferenceDef(finder));
				break;
			case TYPE :
				def = component;
				break;
			default :
				return;
		}
		component.updateVersion(V1_1, "xml attribute");
		def.addExtensionAttribute(xmlAttr, annotation);
	}

	private void doDesignate(Annotation annotation) {
		if (TRUE.equals(annotation.get("factory")) && (component.configurationPolicy == null)) {
			component.configurationPolicy = ConfigurationPolicy.REQUIRE;
		}
	}

	/**
	 *
	 */
	private void doActivate(Annotation annotation) {
		String memberDescriptor = member.descriptor();
		switch (annotation.elementType()) {
			case METHOD : {
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
					member.getName(), memberDescriptor, ErrorType.ACTIVATE_SIGNATURE_ERROR);
				component.activate = member.getName();
				if (!member.isProtected()) {
					component.updateVersion(V1_1, "activate method not protected");
				}
				if (!"activate".equals(member.getName())) {
					component.updateVersion(V1_1, "activate method named not activate");
				}
				processMethodActivationArgs(ComponentDef.PROPERTYDEF_ACTIVATEFORMAT, memberDescriptor, details, false);
				break;
			}
			case FIELD : {
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
					member.getName(), ErrorType.ACTIVATE_SIGNATURE_ERROR);
				if (fieldSig != null) {
					// field type is ReferenceTypeSignature
					FieldResolver resolver = new FieldResolver(classSig, fieldSig);
					ReferenceTypeSignature type = resolver.resolveField();
					if (type instanceof ClassTypeSignature param) {
						component.activation_fields.add(member.getName());
						component.updateVersion(V1_4, "field type is ReferenceTypeSignature???");
						String propertyDefKey = String.format(ComponentDef.PROPERTYDEF_FIELDFORMAT, member.getName());
						processActivationObject(propertyDefKey, param, memberDescriptor, details, false);
						break;
					}
				}
				details.addError(analyzer, "[%s] Invalid activation object type %s", details.location(),
					member.descriptor());
				break;
			}
			case CONSTRUCTOR : {
				if (!((MethodDef)member).getContainingClass().getFQN().equals(clazz.getFQN())) {
					// Ignore constructors from super classes as cannot be called directly
					break;
				}
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
					member.getName(), memberDescriptor, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR);
				if (component.init != null) {
					details.addError(analyzer, "[%s] Multiple constructors are annotated @Activate.",
						details.location());
					break;
				}
				if (!member.isPublic()) {
					details.addError(analyzer, "[%s] Constructors must be public access.", details.location());
					break;
				}
				constructorSig = methodSig;
				component.init = constructorSig.parameterTypes.length;
				component.updateVersion(V1_4, "constructor injection");
				constructorArg = 0;
				break;
			}
			default :
				break;
		}
	}

	/**
	 *
	 */
	private void doDeactivate(Annotation annotation) {
		switch (annotation.elementType()) {
			case METHOD : {
				String memberDescriptor = member.descriptor();
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
					member.getName(), memberDescriptor, ErrorType.DEACTIVATE_SIGNATURE_ERROR);
				component.deactivate = member.getName();
				if (!member.isProtected()) {
					component.updateVersion(V1_1, "deactivate method not protected");
				}
				if (!"deactivate".equals(member.getName())) {
					component.updateVersion(V1_1, "deactivate not named deactivate");
				}
				processMethodActivationArgs(ComponentDef.PROPERTYDEF_DEACTIVATEFORMAT, memberDescriptor, details, true);
				break;
			}
			default :
				break;
		}
	}

	/**
	 *
	 */
	private void doModified(Annotation annotation) {
		switch (annotation.elementType()) {
			case METHOD : {
				String memberDescriptor = member.descriptor();
				DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
					member.getName(), memberDescriptor, ErrorType.MODIFIED_SIGNATURE_ERROR);
				component.modified = member.getName();
				component.updateVersion(V1_1, "modified");
				processMethodActivationArgs(ComponentDef.PROPERTYDEF_MODIFIEDFORMAT, memberDescriptor, details, false);
				break;
			}
			default :
				break;
		}
	}

	/**
	 * look for annotation activation objects and extract properties from them
	 */
	private void processMethodActivationArgs(String propertyDefKeyFormat, String memberDescriptor,
		DeclarativeServicesAnnotationError details, boolean deactivate) {
		MethodResolver resolver = new MethodResolver(classSig, methodSig);
		for (int arg = 0; arg < methodSig.parameterTypes.length; arg++) {
			JavaTypeSignature type = resolver.resolveParameter(arg);
			if (type instanceof ClassTypeSignature param) {
				String propertyDefKey = String.format(propertyDefKeyFormat, arg);
				processActivationObject(propertyDefKey, param, memberDescriptor, details, deactivate);
			} else if (deactivate && (type == BaseType.I)) {
				component.updateVersion(V1_1, "deactivate(int)");
			} else {
				details.addError(analyzer, "[%s] Invalid activation object type %s for parameter %s",
					details.location(), type, arg);
			}
		}
		Result resultType = resolver.resolveResult();
		if (resultType instanceof VoidDescriptor) {
			return;
		}
		/**
		 * Felix SCR allows Map return type.
		 */
		if ((resultType instanceof ClassTypeSignature param) && param.binary.equals("java/util/Map")) {
			checkMapReturnType(details);
		} else {
			details.addError(analyzer, "[%s] Invalid return type type %s", details.location(), resultType);
		}
	}

	/**
	 * extract properties from an activation object
	 */
	private void processConstructorActivationArgs(int toArg) {
		String memberDescriptor = member.descriptor();
		DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(className.getFQN(),
			member.getName(), memberDescriptor, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR);
		MethodResolver resolver = new MethodResolver(classSig, constructorSig);
		for (int arg = constructorArg; arg < toArg; arg++) {
			// validate activation object as methodSig.
			JavaTypeSignature type = resolver.resolveParameter(arg);
			if (type instanceof ClassTypeSignature param) {
				String propertyDefKey = String.format(ComponentDef.PROPERTYDEF_CONSTRUCTORFORMAT, arg);
				processActivationObject(propertyDefKey, param, memberDescriptor, details, false);
			} else {
				details.addError(analyzer, "[%s] Invalid activation object type %s for constructor parameter %s",
					details.location(), type, arg);
			}
		}
	}

	private void processActivationObject(String propertyDefKey, ClassTypeSignature param, String memberDescriptor,
		DeclarativeServicesAnnotationError details, boolean deactivate) {
		switch (param.binary) {
			case "org/osgi/service/component/ComponentContext" :
				break;
			case "org/osgi/framework/BundleContext" :
			case "java/util/Map" :
				component.updateVersion(V1_1, "use of Map/BundleContext");
				break;
			case "java/lang/Integer" :
				if (deactivate) {
					component.updateVersion(V1_1, "deactivate(int)");
					break;
				}
				// FALL-THROUGH
			default :
				TypeRef typeRef = analyzer.getTypeRef(param.binary);
				try {
					Clazz clazz = analyzer.findClass(typeRef);
					if (clazz.isAnnotation()) {
						component.updateVersion(V1_3, "Annotation type config??");
						clazz.parseClassFileWithCollector(
							new ComponentPropertyTypeDataCollector(propertyDefKey, memberDescriptor, details));
					} else if (clazz.isInterface() && options.contains(Options.felixExtensions)) {
						component.updateVersion(V1_3, "Felix interface type??");
					} else {
						details.addError(analyzer,
							"[%s] Non annotation type for activation object with descriptor %s, type %s",
							details.location(), memberDescriptor, param.binary);
					}
				} catch (Exception e) {
					analyzer.exception(e,
						"[%s] Exception looking at annotation type for activation object with descriptor %s, type %s",
						details.location(), memberDescriptor, param.binary)
						.details(details);
				}
				break;
		}
	}

	final class ComponentPropertyTypeDataCollector extends ClassDataCollector {
		private final String								propertyDefKey;
		private final String								memberDescriptor;
		private final DeclarativeServicesAnnotationError	details;
		private final PropertyDef							propertyDef		= new PropertyDef(analyzer);
		private int											hasNoDefault	= 0;
		private boolean										hasValue		= false;
		private boolean										hasMethods		= false;
		private FieldDef									prefixField		= null;
		private TypeRef										typeRef			= null;

		ComponentPropertyTypeDataCollector(String propertyDefKey, String memberDescriptor,
			DeclarativeServicesAnnotationError details) {
			this.propertyDefKey = requireNonNull(propertyDefKey);
			this.memberDescriptor = memberDescriptor;
			this.details = details;
		}

		ComponentPropertyTypeDataCollector(String propertyDefKey, Annotation componentPropertyAnnotation,
			DeclarativeServicesAnnotationError details) {
			this.propertyDefKey = requireNonNull(propertyDefKey);
			// Component Property annotations added in 1.4, but they just map to
			// normal DS properties, so there's not really a need to require DS
			// 1.4. Therefore we just leave the required version as is

			this.memberDescriptor = null;
			this.details = details;

			// Add in the defined attributes
			for (Entry<String, Object> entry : componentPropertyAnnotation.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				boolean isClass;
				if (value instanceof TypeRef) {
					isClass = true;
				} else if (value.getClass()
					.isArray()) {
					Object[] valueArray = (Object[]) value;
					if (valueArray.length == 0) {
						// If there are no values then it doesn't matter whether
						// this
						// is a class or not as we pass null for the type and it
						// will
						// eventually get interpreted as a string
						isClass = false;
					} else {
						isClass = valueArray[0] instanceof TypeRef;
					}
				} else {
					isClass = false;
				}
				handleValue(key, value, isClass, null);
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
								.warning("[%s] Nested annotation type found in member %s, %s", details.location(), name,
									type.getFQN())
								.details(details);
							return;
						}
					} catch (Exception e) {
						if (memberDescriptor != null) {
							analyzer
								.exception(e,
									"[%s] Exception looking at annotation type on member with descriptor %s, type %s",
									details.location(), memberDescriptor, type)
								.details(details);
						} else {
							analyzer
								.exception(e, "[%s] Exception looking at annotation %s", details.location(),
									typeRef.getFQN())
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
					&& (c instanceof String p)) {
					prefix = p;

					// If we have a member descriptor then this is an injected
					// component property type and the prefix must be processed
					// and understood by DS 1.4. Otherwise bnd handles the
					// property prefix mapping transparently here and DS doesn't
					// need to care
					if (memberDescriptor != null) {
						component.updateVersion(V1_4, "injected component property");
					}
				} else {
					prefix = null;
					analyzer.warning(
						"[%s] Field PREFIX_ in %s is not a static final String field with a compile-time constant value: %s",
						details.location(), typeRef.getFQN(), c)
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
				// If we have a member descriptor then this is an injected
				// component property type and the single elementness must
				// be processed and understood by DS 1.4. Otherwise bnd handles
				// the property mapping transparently here and DS doesn't
				// need to care
				if (memberDescriptor != null) {
					component.updateVersion(V1_4, "member descriptor injected component property");
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
						key = prefix.concat(key);
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
			StringBuilder sb = new StringBuilder();
			int start = 0;
			for (; m.find(); start = m.end()) {
				sb.append(name, start, m.start());
				switch (m.group()) {
					case "__" : // __ to _
						sb.append('_');
						break;
					case "_" : // _ to .
						sb.append('.');
						break;
					case "$_$" : // $_$ to -
						sb.append('-');
						break;
					case "$$" : // $$ to $
						sb.append('$');
						break;
					case "$" : // $ removed
						break;
					default : // unknown!
						sb.append(m.group());
						analyzer.error("[%s] unknown mapping %s in property name %s", details.location(), m.group(),
							name);
						break;
				}
			}
			return (start == 0) ? name
				: sb.append(name, start, name.length())
					.toString();
		}
	}

	/**
	 * @param reference @Reference proxy backed by annotation.
	 * @param annotation @Reference contents
	 * @throws Exception
	 */
	private void doReference(Reference reference, Annotation annotation) throws Exception {
		ReferenceDef def;
		String paramName = null;
		switch (annotation.elementType()) {
			case METHOD :
			case FIELD :
				def = referencesByTarget.computeIfAbsent(member, m -> new ReferenceDef(finder));
				break;
			case PARAMETER : // reference element on constructor parameter
				paramName = String.format(constructorArgFormat, parameter);
				def = referencesByTarget.computeIfAbsent(paramName, m -> new ReferenceDef(finder));
				break;
			case TYPE : // reference element of Component annotation
			default :
				def = new ReferenceDef(finder);
				break;
		}

		def.className = className.getFQN();
		if (annotation.get("name") != null) {
			def.name = reference.name();
		}
		if (annotation.get("bind") != null) {
			def.bind = reference.bind();
		}
		if (annotation.get("unbind") != null) {
			def.unbind = reference.unbind();
		}
		if (annotation.get("updated") != null) {
			def.updated = reference.updated();
		}
		if (annotation.get("field") != null) {
			def.field = reference.field();
		}
		if (annotation.get("fieldOption") != null) {
			def.fieldOption = reference.fieldOption();
		}
		if (annotation.get("cardinality") != null) {
			def.cardinality = reference.cardinality();
		}
		if (annotation.get("policy") != null) {
			def.policy = reference.policy();
		}
		if (annotation.get("policyOption") != null) {
			def.policyOption = reference.policyOption();
		}
		if (annotation.get("scope") != null) {
			def.scope = reference.scope();
		}
		if (annotation.get("target") != null) {
			def.target = reference.target();
		}

		TypeRef annoServiceTR = annotation.get("service");
		String annoService = (annoServiceTR != null) ? annoServiceTR.getFQN() : null;

		switch (annotation.elementType()) {
			case METHOD : {
				def.bindDescriptor = member.descriptor();
				def.bind = member.getName();
				if (def.name == null) {
					Matcher m = BINDNAME.matcher(member.getName());
					if (m.matches()) {
						def.name = m.group("name");
					} else {
						addError(def, ErrorType.INVALID_REFERENCE_BIND_METHOD_NAME,
							"In component '%s', invalid name for bind method '%s'", className, def.bind);
					}
				}

				def.service = determineMethodReferenceType(def, (MethodDef) member, methodSig, annoService);

				if (def.service == null) {
					addError(def, ErrorType.REFERENCE,
						"In component '%s', cannot recognize the signature of method '%s': %s", className, def.bind,
						methodSig);
				}
				break;
			}
			case FIELD : {
				def.updateVersion(V1_3, "field reference");
				def.field = member.getName();
				if (def.name == null) {
					def.name = def.field;
				}
				// field type is ReferenceTypeSignature
				ReferenceTypeSignature type = null;
				if (fieldSig != null) {
					FieldResolver resolver = new FieldResolver(classSig, fieldSig);
					type = resolver.resolveField();
					def.service = determineReferenceType(def, type, resolver, annoService);
				}

				if (def.service != null) {
					if ((def.policy == null) && member.isVolatile()) {
						def.policy = ReferencePolicy.DYNAMIC;
					}
					if (def.isCollection) {
						if (def.cardinality == null) {
							def.cardinality = def.isOptional ? ReferenceCardinality.OPTIONAL
								: ReferenceCardinality.MULTIPLE;
						}
						if (annotation.get("collectionType") != null) {
							def.collectionType = reference.collectionType();
						}
						if (def.isOptional && (def.cardinality != ReferenceCardinality.OPTIONAL)
							&& (def.cardinality != ReferenceCardinality.MANDATORY)) {
							addError(def, ErrorType.OPTIONAL_FIELD_WITH_MULTIPLE,
								"In component '%s', field '%s' is 'Optional' but cardinality is not '0..1' or '1..1'.",
								className, def.field);
						}
					}
					if ((def.fieldOption == null) && (def.policy == ReferencePolicy.DYNAMIC)
						&& ((def.cardinality == ReferenceCardinality.MULTIPLE)
							|| (def.cardinality == ReferenceCardinality.AT_LEAST_ONE))
						&& member.isFinal()) {
						def.fieldOption = FieldOption.UPDATE;
					}
					if (def.fieldOption == FieldOption.UPDATE) {
						if (def.policy != ReferencePolicy.DYNAMIC) {
							addError(def, ErrorType.UPDATE_FIELD_WITH_STATIC,
									"In component '%s', field '%s' fieldOption is 'update' but policy is not 'dynamic'.",
								className, def.field);
						}
						if ((def.cardinality != ReferenceCardinality.MULTIPLE)
							&& (def.cardinality != ReferenceCardinality.AT_LEAST_ONE)) {
							addError(def, ErrorType.UPDATE_FIELD_WITH_UNARY,
								"In component '%s', field '%s' fieldOption is 'update' but cardinality is not '0..n' or '1..n'.",
								className, def.field);
						}
					} else { // def.fieldOption == FieldOption.REPLACE
						if (member.isFinal()) {
							addError(def, ErrorType.FINAL_FIELD_WITH_REPLACE,
									"In component '%s', field '%s' has fieldOption 'replace' but the field is final.",
									className,
								def.field);
						}
						if ((def.policy == ReferencePolicy.DYNAMIC) && !member.isVolatile()) {
							addError(def, ErrorType.DYNAMIC_FIELD_NOT_VOLATILE,
								"In component '%s', field '%s' policy is 'dynamic' and field is not volatile.",
								className, def.field);
						}
						if (def.isNotDirectlyListOrCollection) {
							addError(def, ErrorType.COLLECTION_SUBCLASS_FIELD_WITH_REPLACE,
								"In component '%s', field '%s' is a collection type with fieldOption 'replace' but the type of the field is not declared as 'List' or 'Collection'.",
								className, def.field);
						}
					}
				} else {
					addError(def, ErrorType.REFERENCE,
						"In component '%s', cannot recognize the signature of field '%s': %s", className, def.field,
						(type != null) ? type : member.descriptor());
				}
				break;
			}
			case PARAMETER : {
				if (!"<init>".equals(member.getName())) {
					addError(def, ErrorType.REFERENCE,
						"In component '%s', @Reference can be used on parameters only for constructors; method '%s' is not a constructor",
						className, member);
					return;
				}
				if (constructorSig == null) {
					addError(def, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR,
						"In component '%s', @Reference can only be used for parameters on the constructor annotated @Activate",
						className);
					return;
				}

				processConstructorActivationArgs(parameter);
				constructorArg = parameter + 1;

				def.parameter = parameter;
				if (def.name == null) {
					MethodParameter[] parameters = ((MethodDef) member).getParameters();
					if ((parameters != null) && (parameter < parameters.length)) {
						def.name = parameters[parameter].getName();
					} else {
						// The DS specification says we are supposed to use the
						// parameter name, but not all class files will have the
						// MethodParameters attribute.
						def.name = paramName;
					}
				}

				MethodResolver resolver = new MethodResolver(classSig, constructorSig);
				JavaTypeSignature type = resolver.resolveParameter(parameter);
				def.service = determineReferenceType(def, type, resolver, annoService);
				if (def.service != null) {
					if (def.policy == ReferencePolicy.DYNAMIC) {
						addError(def, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR,
							"In component '%s', constructor parameters may not be dynamic", className);
						def.policy = ReferencePolicy.STATIC;
					}
					if (def.isCollection) {
						if (def.cardinality == null) {
							def.cardinality = def.isOptional ? ReferenceCardinality.OPTIONAL
								: ReferenceCardinality.MULTIPLE;
						}
						if (annotation.get("collectionType") != null) {
							def.collectionType = reference.collectionType();
						}
						if (def.isOptional && (def.cardinality != ReferenceCardinality.OPTIONAL)
							&& (def.cardinality != ReferenceCardinality.MANDATORY)) {
							addError(def, ErrorType.OPTIONAL_FIELD_WITH_MULTIPLE,
								"In component '%s', constructor argument %s is 'Optional' but cardinality is not '0..1' or '1..1'.",
								className, def.parameter);
						}
						if (def.isNotDirectlyListOrCollection) {
							addError(def, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR,
								"In component '%s', constructor argument %s is a subclass of Collection but it must be declared as either List or Collection",
								className, def.parameter);
						}
					}
					if (def.fieldOption == FieldOption.UPDATE) {
						addError(def, ErrorType.CONSTRUCTOR_SIGNATURE_ERROR,
							"In component '%s', constructor argument %s is marked with 'update' fieldOption. Changing this to 'replace'.",
							className, def.parameter);
						def.fieldOption = null;
					}
				} else {
					addError(def, ErrorType.REFERENCE,
						"In component '%s', cannot recognize the signature of constructor argument %s: %s", className,
						def.parameter, type);
				}
				break;
			}
			case TYPE : { // reference element of Component annotation
				def.service = annoService;
				if (def.name == null) {
					addError(def, ErrorType.MISSING_REFERENCE_NAME,
						"In component '%s', 'name' must be specified for a @Reference specified in the @Component annotation. Service: %s",
						className, def.service);
					return;
				}
				break;
			}
			default :
				break;
		}

		// If we have a target, this must be a filter
		if (def.target != null) {
			String error = Verifier.validateFilter(def.target);
			if (error != null) {
				addError(def, ErrorType.INVALID_TARGET_FILTER, "In component '%s', invalid target filter %s for %s: %s",
					className, def.target, def.name, error);
			}
		}

		if ("org.osgi.service.component.AnyService".equals(def.service)) {
			def.updateVersion(V1_5, "use of AnyService");
			if (def.target == null) {
				addError(def, ErrorType.ANYSERVICE_NO_TARGET,
					"In component '%s', @Reference name '%s' uses 'AnyService' without specifying 'target'.", className,
					def.name);
			}
		}

		if (component.references.containsKey(def.name)) {
			addError(def, ErrorType.MULTIPLE_REFERENCES_SAME_NAME, "In component '%s', multiple references with the same name: %s. Previous def: %s, this def: %s",
				className, component.references.get(def.name), def.service, "");
		} else {
			component.references.put(def.name, def);
		}
	}

	private DeclarativeServicesAnnotationError getDetails(ReferenceDef def, ErrorType type) {
		if (def == null) {

			return null;
		}

		if (def.bindDescriptor != null) {
			return new DeclarativeServicesAnnotationError(className.getFQN(), def.bind, def.bindDescriptor, type);
		}
		return new DeclarativeServicesAnnotationError(className.getFQN(), def.field, type);
	}

	private String determineReferenceType(ReferenceDef def, JavaTypeSignature type, ClassResolver resolver,
		String annoService) {
		String paramType = null;
		String inferredService = null;
		if (type instanceof ClassTypeSignature param) {
			paramType = binaryToFQN(param.binary);
			// Check for collection
			switch (paramType) {
				case "java.util.Collection" :
				case "java.util.List" :
					def.isCollection = true;
					break;
				default :
					// check for subtype of Collection
					if (analyzer.assignable(paramType, "java.util.Collection", false)) {
						def.isCollection = true;
						def.isNotDirectlyListOrCollection = true;
					}
					break;
				case "java.util.Optional" :
					// We think of Optional as a collection
					def.isCollection = true;
					def.isOptional = true;
					def.updateVersion(V1_5, "use of Optional");
					break;
			}
			if (def.isCollection) {
				TypeArgument[] typeArguments = param.classType.typeArguments;
				if (typeArguments.length != 0) {
					ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[0]);
					if (inferred instanceof ClassTypeSignature inferredParam) {
						def.collectionType = CollectionType.SERVICE;
						param = inferredParam;
						paramType = binaryToFQN(param.binary);
					}
				}
			}
			// compute inferred service
			boolean tryInfer = (annoService == null) || !annoService.equals(paramType);
			switch (paramType) {
				case "org.osgi.service.component.ComponentServiceObjects" :
					if (tryInfer) {
						if (def.isCollection) {
							def.collectionType = CollectionType.SERVICEOBJECTS;
						}
						TypeArgument[] typeArguments = param.classType.typeArguments;
						if (typeArguments.length != 0) {
							ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[0]);
							if (inferred instanceof ClassTypeSignature inferredParam) {
								inferredService = binaryToFQN(inferredParam.binary);
							}
						}
					}
					break;
				case "org.osgi.framework.ServiceReference" :
					if (tryInfer) {
						if (def.isCollection) {
							def.collectionType = CollectionType.REFERENCE;
						}
						TypeArgument[] typeArguments = param.classType.typeArguments;
						if (typeArguments.length != 0) {
							ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[0]);
							if (inferred instanceof ClassTypeSignature inferredParam) {
								inferredService = binaryToFQN(inferredParam.binary);
							}
						}
					}
					break;
				case "java.util.Map" :
					if (tryInfer) {
						if (def.isCollection) {
							def.collectionType = CollectionType.PROPERTIES;
						}
					}
					break;
				case "java.util.Map$Entry" :
					if (tryInfer) {
						if (def.isCollection) {
							def.collectionType = CollectionType.TUPLE;
						}
						TypeArgument[] typeArguments = param.innerTypes[0].typeArguments;
						if (typeArguments.length != 0) {
							ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[1]);
							if (inferred instanceof ClassTypeSignature inferredParam) {
								inferredService = binaryToFQN(inferredParam.binary);
							}
						}
					}
					break;
				default :
					if (def.isCollection && (def.collectionType == null)) {
						// no inferred type for collection
						def.collectionType = CollectionType.SERVICE;
					} else {
						inferredService = paramType;
					}
					break;
			}
		}

		if (!analyzer.assignable(annoService, inferredService)) {
			if (!def.isCollection && "org.osgi.service.log.LoggerFactory".equals(annoService)
				&& ("org.osgi.service.log.Logger".equals(paramType)
					|| "org.osgi.service.log.FormatterLogger".equals(paramType))) {
				def.updateVersion(V1_4, "use of logger");
			} else {
				return null;
			}
		}
		return annoService != null ? annoService : inferredService;
	}

	private String determineMethodReferenceType(ReferenceDef def, MethodDef method, MethodSignature signature,
		String annoService) {
		// We have to find the type of the current method to
		// link it to the referenced service.
		int parameterCount = signature.parameterTypes.length;
		if (parameterCount == 0) {
			return null;
		}
		MethodResolver resolver = new MethodResolver(classSig, signature);
		boolean hasMapResultType = false;
		Result resultType = resolver.resolveResult();
		if (!(resultType instanceof VoidDescriptor)) {
			if ((resultType instanceof ClassTypeSignature param) && param.binary.equals("java/util/Map")) {
				hasMapResultType = true;
			} else {
				return null;
			}
		}
		JavaTypeSignature first = resolver.resolveParameter(0);
		if (!(first instanceof ClassTypeSignature param)) {
			return null;
		}
		Version minVersion = null;
		String minReason = null;
		switch (parameterCount) {
			case 1 :
				if (!method.isProtected()) {
					minVersion = V1_1;
					minReason = "protected method";
				}
				break;
			case 2 :
				JavaTypeSignature type = resolver.resolveParameter(1);
				if ((type instanceof ClassTypeSignature typeParam) && typeParam.binary.equals("java/util/Map")) {
					minVersion = V1_1;
					minReason = "use of map in method reference in second position";
				} else {
					minVersion = V1_3;
					minReason = "use of map in method reference not in second position";
				}
				break;
			default :
				minVersion = V1_3;
				minReason = "default determineMethodReferenceType 0 or > 2 parameters";
				break;
		}
		String paramType = binaryToFQN(param.binary);
		boolean tryInfer = (annoService == null) || !annoService.equals(paramType);
		String inferredService = null;
		switch (paramType) {
			case "org.osgi.service.component.ComponentServiceObjects" :
				if (tryInfer) {
					// first argument using type supported starting in 1.3
					minVersion = V1_3;
					minReason = "first argument using type supported starting in 1.3";
					TypeArgument[] typeArguments = param.classType.typeArguments;
					if (typeArguments.length != 0) {
						ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[0]);
						if (inferred instanceof ClassTypeSignature inferredParam) {
							inferredService = binaryToFQN(inferredParam.binary);
						}
					}
				}
				break;
			case "org.osgi.framework.ServiceReference" :
				// infer service type
				if (tryInfer) {
					TypeArgument[] typeArguments = param.classType.typeArguments;
					if (typeArguments.length != 0) {
						ReferenceTypeSignature inferred = resolver.resolveType(typeArguments[0]);
						if (inferred instanceof ClassTypeSignature inferredParam) {
							inferredService = binaryToFQN(inferredParam.binary);
						}
					}
				}
				break;
			case "java.util.Map" :
				if (tryInfer) {
					// first argument using type supported starting in 1.3
					minVersion = V1_3;
					minReason = "first argument using more types supported starting in 1.3";
				}
				break;
			default :
				// inferred service
				inferredService = paramType;
				break;
		}

		if (!analyzer.assignable(annoService, inferredService)) {
			if ("org.osgi.service.log.LoggerFactory".equals(annoService)
				&& ("org.osgi.service.log.Logger".equals(paramType)
					|| "org.osgi.service.log.FormatterLogger".equals(paramType))) {
				minVersion = V1_4;
				minReason = "Use of logger";
			} else {
				return null;
			}
		}
		if (hasMapResultType) {
			checkMapReturnType(getDetails(def, ErrorType.REFERENCE));
		}
		if (minVersion != null) {
			def.updateVersion(minVersion, minReason);
		}
		return annoService != null ? annoService : inferredService;
	}

	private void checkMapReturnType(DeclarativeServicesAnnotationError details) {
		if (!options.contains(Options.felixExtensions)) {
			details.addError(analyzer,
				"[%s] To use a return type of Map you must specify the -dsannotations-options felixExtensions flag "
					+ " and use a felix extension attribute or explicitly specify the appropriate xmlns.",
				details.location());
		}
	}

	/**
	 * @param annotation
	 * @throws Exception
	 */
	private void doComponent(Component comp, Annotation annotation) throws Exception {

		String componentName = annotation.containsKey("name") ? comp.name() : className.getFQN();

		if (!mismatchedAnnotations.isEmpty()) {
			for (Entry<String, List<DeclarativeServicesAnnotationError>> e : mismatchedAnnotations.entrySet()) {
				for (DeclarativeServicesAnnotationError errorDetails : e.getValue()) {
					errorDetails.addError(analyzer,
						"[%s] The DS component %s uses standard annotations to declare it as a component, but also uses the bnd DS annotation: %s. It is an error to mix these two types of annotations",
						errorDetails.location(), componentName, e.getKey());
				}
			}
			return;
		}

		// Check if we are doing a super class
		if (component.implementation != null) {
			return;
		}

		component.implementation = clazz.getClassName();
		component.name = componentName;
		if (annotation.get("factory") != null) {
			component.factory = comp.factory();
		}
		if (annotation.get("configurationPolicy") != null) {
			component.configurationPolicy = comp.configurationPolicy();
		}
		if (annotation.get("enabled") != null) {
			component.enabled = comp.enabled();
		}
		if (annotation.get("immediate") != null) {
			component.immediate = comp.immediate();
		}
		if (annotation.get("servicefactory") != null) {
			@SuppressWarnings("deprecation")
			boolean servicefactory = comp.servicefactory();
			component.scope = servicefactory ? ServiceScope.BUNDLE : ServiceScope.SINGLETON;
		}
		if (annotation.get("scope") != null && comp.scope() != ServiceScope.DEFAULT) {
			component.scope = comp.scope();
			if (comp.scope() == ServiceScope.PROTOTYPE) {
				component.updateVersion(V1_3, "prototype scope");
			}
		}

		if (annotation.get("configurationPid") != null) {
			component.configurationPid = comp.configurationPid();
			if (component.configurationPid.length > 1) {
				component.updateVersion(V1_3, "multiple configurationPid");
			} else {
				component.updateVersion(V1_2, "single configurationPid");
			}
		}

		if (annotation.get("xmlns") != null) {
			component.xmlns = comp.xmlns();
		}

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
					DeclarativeServicesAnnotationError details = new DeclarativeServicesAnnotationError(
						className.getFQN(), null, null, ErrorType.INCOMPATIBLE_SERVICE);
					try {
						Clazz service = analyzer.findClass(typeRef);
						if (!analyzer.assignable(clazz, service)) {
							details.addError(analyzer, "[%s] The component is not assignable to specified service %s",
								details.location(), typeRef.getFQN());
						}
					} catch (Exception e) {
						analyzer
							.exception(e, "[%s] An error occurred when attempting to process service %s",
								details.location(), typeRef.getFQN())
							.details(details);
					}
				})
				.toArray(TypeRef[]::new);
		}

		Object[] refAnnotations = annotation.get("reference");
		if (refAnnotations != null) {
			for (Object o : refAnnotations) {
				Annotation refAnnotation = (Annotation) o;
				Reference ref = refAnnotation.getAnnotation(Reference.class);
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
	public void extendsClass(TypeRef name) {
		this.extendsClass = name;
	}

	@Override
	public void implementsInterfaces(TypeRef[] interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * Check whether the current member should be ignored.
	 *
	 * @return {@code true} if the current member should be ignored.
	 *         {@code false} otherwise.
	 */
	private boolean ignoreMember() {
		if (member.isStatic()) {
			return true; // ignore static members
		}
		if (member instanceof MethodDef method) {
			if (method.isAbstract() // ignore abstract methods
				|| method.isBridge() // ignore bridge methods
				|| method.isSynthetic() // ignore synthetic methods
				|| (!baseclass && method.isPrivate())) {
				// ignore private methods on subclasses
				return true;
			}
		}
		return false;
	}

	@Override
	public void field(FieldDef field) {
		member = field;
		if (ignoreMember()) {
			return;
		}

		fieldSig = getFieldSignature(field);
	}

	private FieldSignature getFieldSignature(FieldDef field) {
		String signature = field.getSignature();
		if (signature != null) {
			return analyzer.getFieldSignature(signature);
		}
		signature = field.descriptor();
		return switch (signature.charAt(0)) {
			case 'L', // ClassTypeSignature
				'T', // TypeVariableSignature
				'[' -> // ArrayTypeSignature
				analyzer.getFieldSignature(signature);
			default -> // BaseType
				null;
		};
	}

	@Override
	public void method(MethodDef method) {
		// handle before setting member field
		if (constructorSig != null) {
			processConstructorActivationArgs(constructorSig.parameterTypes.length);
			constructorSig = null;
		}

		member = method;
		if (ignoreMember()) {
			return;
		}

		methods.add(method.getName(), method);
		methodSig = getMethodSignature(method);
	}

	private MethodSignature getMethodSignature(MethodDef method) {
		String signature = method.getSignature();
		return analyzer.getMethodSignature((signature != null) ? signature : method.descriptor());
	}

	@Override
	public void memberEnd() {
		// handle before setting member field
		if (constructorSig != null) {
			processConstructorActivationArgs(constructorSig.parameterTypes.length);
			constructorSig = null;
		}

		member = null;
	}

	@Override
	public void parameter(int p) {
		parameter = p;
	}

	@Override
	public String toString() {
		if (member == null) {
			return clazz.toString();
		}
		return clazz + "#" + member;
	}

	private void addError(ReferenceDef def, ErrorType type, String message, Object... params) {
		DeclarativeServicesAnnotationError details = getDetails(def, type);
		if (details == null) {
			analyzer.error(message, params);
		} else {
			details.addError(analyzer, message, params);
		}
	}
}
