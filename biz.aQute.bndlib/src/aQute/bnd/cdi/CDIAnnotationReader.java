package aQute.bnd.cdi;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Annotation.ElementType;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
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
import aQute.bnd.signatures.VoidDescriptor;
import aQute.bnd.version.Version;

public class CDIAnnotationReader extends ClassDataCollector {
	public static final Version			V1_0					= new Version(1, 0, 0);
	public static final Version			CDI_ARCHIVE_VERSION		= new Version(1, 1, 0);

	private static final Instruction	COMPONENTSCOPED_INSTR	= new Instruction(
		"org.osgi.service.cdi.annotations.ComponentScoped");
	private static final Instruction	DEPENDENT_INSTR			= new Instruction("javax.enterprise.context.Dependent");
	private static final Instruction	EXTENSION_INSTR			= new Instruction(
		"javax.enterprise.inject.spi.Extension");
	private static final Instruction	INTERCEPTOR_INSTR		= new Instruction("javax.interceptor.Interceptor");
	private static final Instruction	NORMALSCOPE_INSTR		= new Instruction(
		"javax.enterprise.context.NormalScope");
	private static final Instruction	STEREOTYPE_INSTR		= new Instruction("javax.enterprise.inject.Stereotype");
	private static final Instruction	VETOED_INSTR			= new Instruction("javax.enterprise.inject.Vetoed");

	final Analyzer						analyzer;
	final Clazz							clazz;
	final ClassSignature				classSig;
	final EnumSet<Discover>				options;
	final Map<PackageRef, PackageDef>	packageInfos			= new HashMap<>();
	final List<BeanDef>					definitions				= new ArrayList<>();
	boolean								baseclass				= true;
	TypeRef								extendsClass;
	TypeRef								interfaces[];
	FieldDef							member;
	int									parameter				= -1;
	ReferenceDef						referenceDef;
	int									targetIndex				= Clazz.TYPEUSE_INDEX_NONE;

	CDIAnnotationReader(Analyzer analyzer, Clazz clazz, EnumSet<Discover> options) {
		this.analyzer = requireNonNull(analyzer);
		this.clazz = clazz;
		this.options = options;
		this.definitions.add(new BeanDef());
		String signature = clazz.getClassSignature();
		this.classSig = analyzer.getClassSignature((signature != null) ? signature : "Ljava/lang/Object;");
	}

	public static List<BeanDef> getDefinition(Clazz c, Analyzer analyzer, EnumSet<Discover> options) throws Exception {
		CDIAnnotationReader r = new CDIAnnotationReader(analyzer, c, options);
		return r.getDefs();
	}

	private List<BeanDef> getDefs() throws Exception {
		// if discovery mode is 'annotated', only classes annotated with a bean
		// defining annotation are considered. See
		// http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#bean_defining_annotations
		if (options.contains(Discover.annotated)) {
			if (clazz.is(QUERY.ANNOTATED, VETOED_INSTR, analyzer)
				|| (!clazz.is(QUERY.INDIRECTLY_ANNOTATED, COMPONENTSCOPED_INSTR, analyzer)
					&& !clazz.is(QUERY.INDIRECTLY_ANNOTATED, NORMALSCOPE_INSTR, analyzer)
					&& !clazz.is(QUERY.INDIRECTLY_ANNOTATED, STEREOTYPE_INSTR, analyzer)
					&& !clazz.is(QUERY.ANNOTATED, DEPENDENT_INSTR, analyzer)
					&& !clazz.is(QUERY.ANNOTATED, INTERCEPTOR_INSTR, analyzer))
				|| clazz.is(QUERY.IMPLEMENTS, EXTENSION_INSTR, analyzer)) {

				return null;
			}

			// check for @Vitoed package
			Clazz packageClazz = analyzer.getPackageInfo(clazz.getClassName()
				.getPackageRef());
			if (packageClazz != null && packageClazz.is(QUERY.ANNOTATED, VETOED_INSTR, analyzer)) {
				return null;
			}
		}

		clazz.parseClassFileWithCollector(this);

		// the default discovery mode is 'annotated_by_bean' to indicate that
		// classes annotated with @Bean or in packages annotated with @Beans are
		// considered
		if (options.contains(Discover.annotated_by_bean) && !definitions.get(0).marked) {
			return null;
		}

		return definitions;
	}

	@Override
	public void annotation(Annotation annotation) {
		try {
			switch (annotation.getName()
				.getFQN()) {
				case "org.osgi.service.cdi.annotations.Bean" :
					definitions.get(0).marked = true;
					break;
				case "org.osgi.service.cdi.annotations.Service" :
					doService(annotation);
					break;
				case "org.osgi.service.cdi.annotations.MinimumCardinality" :
					int minimumCardinality = (int) annotation.get("value");
					if (minimumCardinality > 0) {
						if (referenceDef == null) {
							referenceDef = new ReferenceDef();
						}
						referenceDef.cardinality = ReferenceCardinality.AT_LEAST_ONE;
					}
					break;
				case "org.osgi.service.cdi.annotations.Reference" :
					doReference(annotation, parameter);
					break;
			}
		} catch (Exception e) {
			analyzer.exception(e, "During bean processing on class %s, exception %s", clazz, e);
		}
	}

	@Override
	public void classBegin(int access, TypeRef name) {
		definitions.get(0).implementation = name;

		PackageDef packageDef = packageInfos.computeIfAbsent(name.getPackageRef(),
			k -> new PackageDef(analyzer.getPackageInfo(k)));

		if (packageDef.marked != null) {
			definitions.get(0).marked = packageDef.marked.matches(name.getFQN());
		}
	}

	@Override
	public void classEnd() throws Exception {
		member = null;
		referenceDef = null;
	}

	@Override
	public void extendsClass(TypeRef name) {
		this.extendsClass = name;
	}

	@Override
	public void field(FieldDef field) {
		this.member = field;
	}

	@Override
	public void implementsInterfaces(TypeRef[] interfaces) {
		this.interfaces = interfaces;
	}

	@Override
	public void memberEnd() {
		member = null;
		referenceDef = null;
		parameter = -1;
	}

	@Override
	public void method(MethodDef method) {
		if (method.isAbstract() || method.isBridge() || method.isSynthetic()) {
			this.member = null;
			return;
		}

		if (!baseclass && method.isPrivate()) {
			this.member = null;
			return;
		}

		this.member = method;

		String signature = (member.getSignature() != null) ? member.getSignature()
			: member.descriptor();
		MethodSignature methodSig = analyzer.getMethodSignature(signature);
		MethodResolver resolver = new MethodResolver(classSig, methodSig);
		if (methodSig.parameterTypes.length != 1)
			return;
		JavaTypeSignature parameterSig = resolver.resolveParameter(0);
		if (!(parameterSig instanceof ClassTypeSignature)) {
			return;
		}
		ClassTypeSignature param = (ClassTypeSignature) parameterSig;
		switch (param.binary) {
			case "org/osgi/service/cdi/reference/BindService" :
			case "org/osgi/service/cdi/reference/BindBeanServiceObjects" :
			case "org/osgi/service/cdi/reference/BindServiceReference" : {
				if (param.classType.typeArguments.length != 1) {
					analyzer.error("In bean %s, Bind parameter has wrong type arguments: %s", clazz, param);
					return;
				}
				ReferenceTypeSignature inferred = resolver.resolveType(param.classType.typeArguments[0]);
				if (!(inferred instanceof ClassTypeSignature)) {
					analyzer.error("In bean %s, Bind parameter has unresolvable type argument: %s", clazz, inferred);
					return;
				}
				ClassTypeSignature classSig = (ClassTypeSignature) inferred;
				TypeRef typeRef = analyzer.getTypeRef(classSig.binary);
				ReferenceDef referenceDef = new ReferenceDef();
				referenceDef.service = typeRef.getFQN();
				referenceDef.cardinality = ReferenceCardinality.MULTIPLE;
				definitions.get(0).references.add(referenceDef);
				break;
			}
			default :
				// skip
				break;
		}
	}

	@Override
	public void parameter(int p) {
		parameter = p;
	}

	@Override
	public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
		if (target_type != 0x10) {
			targetIndex = Clazz.TYPEUSE_INDEX_NONE;
			return;
		}

		targetIndex = target_index;
	}

	private void doReference(Annotation reference, int targetIndex) {
		Object value = reference.get("value");

		if (value != null) {
			TypeRef typeRef = (TypeRef) value;

			referenceDef = new ReferenceDef();
			referenceDef.service = typeRef.getFQN();
			referenceDef.cardinality = ReferenceCardinality.MANDATORY;
			definitions.get(0).references.add(referenceDef);

			return;
		}

		ClassTypeSignature type;
		ClassResolver resolver;
		switch (reference.elementType()) {
			case PARAMETER : {
				String signature = (member.getSignature() != null) ? member.getSignature()
					: member.descriptor();
				MethodSignature methodSig = analyzer.getMethodSignature(signature);
				resolver = new MethodResolver(classSig, methodSig);
				JavaTypeSignature parameterType = ((MethodResolver) resolver).resolveParameter(parameter);
				if (!(parameterType instanceof ClassTypeSignature)) {
					analyzer.error("In bean %s, method %s, parameter %s with @Reference has unresolved type %s",
						classSig, member.getName(), targetIndex, parameterType);
					return;
				}
				type = (ClassTypeSignature) parameterType;
				break;
			}
			default : { // FIELD
				FieldSignature fieldSig;
				String signature = member.getSignature();
				if (signature == null) {
					try {
						fieldSig = analyzer.getFieldSignature(member.descriptor());
					} catch (IllegalArgumentException iae) {
						fieldSig = null;
					}
				} else {
					fieldSig = analyzer.getFieldSignature(signature);
				}
				if (fieldSig == null) {
					analyzer.error("In bean %s, field %s has an incompatible type for @Reference: %s", clazz,
						member.getName(), member.descriptor());
					return;
				}
				resolver = new FieldResolver(classSig, fieldSig);
				ReferenceTypeSignature inferred = ((FieldResolver) resolver).resolveField();
				if (!(inferred instanceof ClassTypeSignature)) {
					analyzer.error("In bean %s, field %s with @Reference has unresolved type: %s", classSig,
						member.getName(), inferred);
					return;
				}
				type = (ClassTypeSignature) inferred;
				break;
			}
		}

		ReferenceCardinality cardinality = ReferenceCardinality.MANDATORY;
		TypeRef typeRef = analyzer.getTypeRef(type.binary);
		String fqn = typeRef.getFQN();

		// unwrap Provider
		if (fqn.equals("javax.inject.Provider")) {
			ReferenceTypeSignature inferred = resolver.resolveType(type.classType.typeArguments[0]);
			if (!(inferred instanceof ClassTypeSignature)) {
				analyzer.error(
					"In bean %s, in member %s with @Reference the type argument of Provider can't be resolved: %s",
					clazz, member.getName(), inferred);
				return;
			}
			type = (ClassTypeSignature) inferred;
			fqn = Descriptors.binaryToFQN(type.binary);
		}

		// unwrap Collection, List, Optional
		if (fqn.equals("java.util.Collection") || fqn.equals("java.util.List")) {
			ReferenceTypeSignature inferred = resolver.resolveType(type.classType.typeArguments[0]);
			if (!(inferred instanceof ClassTypeSignature)) {
				analyzer.error(
					"In bean %s, in member %s with @Reference the type argument of Collection or List can't be resolved: %s",
					clazz, member.getName(), inferred);
				return;
			}
			type = (ClassTypeSignature) inferred;
			fqn = Descriptors.binaryToFQN(type.binary);
			cardinality = ReferenceCardinality.MULTIPLE;
		} else if (fqn.equals("java.util.Optional")) {
			ReferenceTypeSignature inferred = resolver.resolveType(type.classType.typeArguments[0]);
			if (!(inferred instanceof ClassTypeSignature)) {
				analyzer.error(
					"In bean %s, in member %s with @Reference the type argument of Optional can't be resolved: %s",
					clazz, member.getName(), inferred);
				return;
			}
			type = (ClassTypeSignature) inferred;
			fqn = Descriptors.binaryToFQN(type.binary);
			cardinality = ReferenceCardinality.OPTIONAL;
		}

		if (fqn.equals("org.osgi.service.cdi.reference.BeanServiceObjects")
			|| fqn.equals("org.osgi.framework.ServiceReference")) {

			ReferenceTypeSignature inferred = resolver.resolveType(type.classType.typeArguments[0]);
			if (!(inferred instanceof ClassTypeSignature)) {
				analyzer.error(
					"In bean %s, in member %s with @Reference the type argument of BeanServiceObjects or ServiceReference can't be resolved: %s",
					clazz, member.getName(), inferred);
				return;
			}
			type = (ClassTypeSignature) inferred;
			fqn = Descriptors.binaryToFQN(type.binary);
		} else if (fqn.equals("java.util.Map$Entry")) {
			ReferenceTypeSignature inferred = resolver.resolveType(type.innerTypes[0].typeArguments[1]);
			if (!(inferred instanceof ClassTypeSignature)) {
				analyzer.error(
					"In bean %s, in member %s with @Reference the second type argument of Map.Entry can't be resolved: %s",
					clazz, member.getName(), inferred);
				return;
			}
			type = (ClassTypeSignature) inferred;
			fqn = Descriptors.binaryToFQN(type.binary);
		}

		referenceDef = new ReferenceDef();
		referenceDef.service = fqn;
		referenceDef.cardinality = cardinality;
		definitions.get(0).references.add(referenceDef);
	}

	private void doService(Annotation annotation) {
		switch (annotation.elementType()) {
			case FIELD : {
				Clazz.FieldDef fieldDef = member;
				FieldSignature fieldSig;
				String signature = member.getSignature();
				if (signature == null) {
					try {
						fieldSig = analyzer.getFieldSignature(member.descriptor());
					} catch (IllegalArgumentException iae) {
						fieldSig = null;
					}
				} else {
					fieldSig = analyzer.getFieldSignature(signature);
				}
				if (fieldSig == null) {
					analyzer.error("In bean %s, field %s has an incompatible type for @Service: %s", clazz,
						fieldDef.getName(), member.descriptor());
					return;
				}
				FieldResolver resolver = new FieldResolver(classSig, fieldSig);
				ReferenceTypeSignature type = resolver.resolveField();
				if (!(type instanceof ClassTypeSignature)) {
					analyzer.error("In bean %s, field %s has an incompatible type for @Service: %s", clazz,
						fieldDef.getName(), member.descriptor());
					return;
				}

				ClassTypeSignature returnType = (ClassTypeSignature) type;
				TypeRef typeRef = analyzer.getTypeRef(returnType.binary);
				BeanDef beanDef = new BeanDef();
				beanDef.service.add(typeRef);
				definitions.add(beanDef);
				break;
			}
			case METHOD : {
				Clazz.MethodDef methodDef = (Clazz.MethodDef) member;
				String signature = (member.getSignature() != null) ? member.getSignature()
					: member.descriptor();
				MethodSignature methodSig = analyzer.getMethodSignature(signature);
				MethodResolver resolver = new MethodResolver(classSig, methodSig);
				Result result = resolver.resolveResult();
				if (result instanceof VoidDescriptor) {
					analyzer.error("In bean %s, method %s has @Service and returns void: %s", clazz,
						methodDef.getName(), member.descriptor());
					return;
				}
				if (!(result instanceof ClassTypeSignature)) {
					analyzer.error("In bean %s, method %s has an incompatible return type for @Service: %s", clazz,
						methodDef.getName(), member.descriptor());
					return;
				}
				ClassTypeSignature returnType = (ClassTypeSignature) result;
				TypeRef typeRef = analyzer.getTypeRef(returnType.binary);
				BeanDef producer = new BeanDef();
				producer.service.add(typeRef);
				definitions.add(producer);
				break;
			}
			case TYPE_USE : {
				if (definitions.get(0).serviceOrigin != null && definitions.get(0).serviceOrigin == ElementType.TYPE) {
					analyzer.error("In bean %s, @Service cannot be used both on TYPE and TYPE_USE: %s", clazz,
						classSig);
					break;
				}
				definitions.get(0).serviceOrigin = ElementType.TYPE_USE;
				if (targetIndex == Clazz.TYPEUSE_TARGET_INDEX_EXTENDS) {
					definitions.get(0).service.add(extendsClass);
				} else if (targetIndex != Clazz.TYPEUSE_INDEX_NONE) {
					definitions.get(0).service.add(interfaces[targetIndex]);
				}
				break;
			}
			case TYPE : {
				if (definitions.get(0).serviceOrigin != null
					&& definitions.get(0).serviceOrigin == ElementType.TYPE_USE) {
					analyzer.error("In bean %s, @Service cannot be used both on TYPE and TYPE_USE: %s", clazz,
						classSig);
					break;
				}
				definitions.get(0).serviceOrigin = ElementType.TYPE;
				Object[] serviceClasses = annotation.get("value");

				if (serviceClasses != null && serviceClasses.length > 0) {
					for (Object serviceClass : serviceClasses) {
						definitions.get(0).service.add((TypeRef) serviceClass);
					}
				} else if (interfaces != null && interfaces.length > 0) {
					for (TypeRef inter : interfaces) {
						definitions.get(0).service.add(inter);
					}
				} else {
					definitions.get(0).service.add(clazz.getClassName());
				}
				break;
			}
			default :
				// this can't happen, @Service is limited to the above targets
				break;
		}
	}

}
