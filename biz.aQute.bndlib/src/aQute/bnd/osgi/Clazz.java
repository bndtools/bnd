package aQute.bnd.osgi;

import static aQute.bnd.classfile.ClassFile.ACC_ANNOTATION;
import static aQute.bnd.classfile.ClassFile.ACC_ENUM;
import static aQute.bnd.classfile.ClassFile.ACC_MODULE;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Class;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Fieldref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_InterfaceMethodref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_MethodType;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Methodref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_NameAndType;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_String;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.classfile.AnnotationDefaultAttribute;
import aQute.bnd.classfile.AnnotationInfo;
import aQute.bnd.classfile.AnnotationsAttribute;
import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.BootstrapMethodsAttribute;
import aQute.bnd.classfile.BootstrapMethodsAttribute.BootstrapMethod;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.CodeAttribute;
import aQute.bnd.classfile.CodeAttribute.ExceptionHandler;
import aQute.bnd.classfile.ConstantPool;
import aQute.bnd.classfile.ConstantPool.AbstractRefInfo;
import aQute.bnd.classfile.ConstantPool.MethodTypeInfo;
import aQute.bnd.classfile.ConstantPool.NameAndTypeInfo;
import aQute.bnd.classfile.ConstantValueAttribute;
import aQute.bnd.classfile.DeprecatedAttribute;
import aQute.bnd.classfile.ElementValueInfo;
import aQute.bnd.classfile.ElementValueInfo.EnumConst;
import aQute.bnd.classfile.ElementValueInfo.ResultConst;
import aQute.bnd.classfile.EnclosingMethodAttribute;
import aQute.bnd.classfile.ExceptionsAttribute;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.InnerClassesAttribute;
import aQute.bnd.classfile.InnerClassesAttribute.InnerClass;
import aQute.bnd.classfile.MemberInfo;
import aQute.bnd.classfile.MethodInfo;
import aQute.bnd.classfile.MethodParametersAttribute;
import aQute.bnd.classfile.ParameterAnnotationInfo;
import aQute.bnd.classfile.ParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleTypeAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleTypeAnnotationsAttribute;
import aQute.bnd.classfile.SignatureAttribute;
import aQute.bnd.classfile.SourceFileAttribute;
import aQute.bnd.classfile.StackMapTableAttribute;
import aQute.bnd.classfile.StackMapTableAttribute.AppendFrame;
import aQute.bnd.classfile.StackMapTableAttribute.FullFrame;
import aQute.bnd.classfile.StackMapTableAttribute.ObjectVariableInfo;
import aQute.bnd.classfile.StackMapTableAttribute.SameLocals1StackItemFrame;
import aQute.bnd.classfile.StackMapTableAttribute.SameLocals1StackItemFrameExtended;
import aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame;
import aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo;
import aQute.bnd.classfile.TypeAnnotationInfo;
import aQute.bnd.classfile.TypeAnnotationsAttribute;
import aQute.bnd.osgi.Annotation.ElementType;
import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.Signature;
import aQute.bnd.stream.MapStream;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.generics.Create;
import aQute.libg.glob.Glob;

public class Clazz {
	private final static Logger logger = LoggerFactory.getLogger(Clazz.class);

	@Deprecated
	public class ClassConstant {
		final int		cname;
		public boolean	referred;

		public ClassConstant(int class_index) {
			this.cname = class_index;
		}

		public String getName() {
			return constantPool.utf8(cname);
		}

		@Override
		public String toString() {
			return "ClassConstant[" + getName() + "]";
		}
	}

	public enum JAVA {
		JDK1_1(45, "JRE-1.1", "(&(osgi.ee=JavaSE)(version=1.1))"), //
		JDK1_2(46, "J2SE-1.2", "(&(osgi.ee=JavaSE)(version=1.2))"), //
		JDK1_3(47, "J2SE-1.3", "(&(osgi.ee=JavaSE)(version=1.3))"), //
		JDK1_4(48, "J2SE-1.4", "(&(osgi.ee=JavaSE)(version=1.4))"), //
		J2SE5(49, "J2SE-1.5", "(&(osgi.ee=JavaSE)(version=1.5))"), //
		J2SE6(50, "JavaSE-1.6", "(&(osgi.ee=JavaSE)(version=1.6))"), //
		OpenJDK7(51, "JavaSE-1.7", "(&(osgi.ee=JavaSE)(version=1.7))"), //
		OpenJDK8(52, "JavaSE-1.8", "(&(osgi.ee=JavaSE)(version=1.8))") {

			Map<String, Set<String>> profiles;

			@Override
			public Map<String, Set<String>> getProfiles() throws IOException {
				if (profiles == null) {
					Properties p = new UTF8Properties();
					try (InputStream in = Clazz.class.getResourceAsStream("profiles-" + this + ".properties")) {
						p.load(in);
					}
					profiles = MapStream.of(p)
						.map((k, v) -> MapStream.entry((String) k, Strings.splitAsStream((String) v)
							.collect(toSet())))
						.collect(MapStream.toMap());
				}
				return profiles;
			}
		}, //
		OpenJDK9(53, "JavaSE-9", "(&(osgi.ee=JavaSE)(version=9))"), //
		OpenJDK10(54, "JavaSE-10", "(&(osgi.ee=JavaSE)(version=10))"), //
		OpenJDK11(55, "JavaSE-11", "(&(osgi.ee=JavaSE)(version=11))"), //
		OpenJDK12(56, "JavaSE-12", "(&(osgi.ee=JavaSE)(version=12))"), //
		OpenJDK13(57, "JavaSE-13", "(&(osgi.ee=JavaSE)(version=13))"), //
		OpenJDK14(58, "JavaSE-14", "(&(osgi.ee=JavaSE)(version=14))"), //
		UNKNOWN(Integer.MAX_VALUE, "<UNKNOWN>", "(osgi.ee=UNKNOWN)");

		final int		major;
		final String	ee;
		final String	filter;

		JAVA(int major, String ee, String filter) {
			this.major = major;
			this.ee = ee;
			this.filter = filter;
		}

		static JAVA format(int n) {
			for (JAVA e : JAVA.values())
				if (e.major == n)
					return e;
			return UNKNOWN;
		}

		public int getMajor() {
			return major;
		}

		public boolean hasAnnotations() {
			return major >= J2SE5.major;
		}

		public boolean hasGenerics() {
			return major >= J2SE5.major;
		}

		public boolean hasEnums() {
			return major >= J2SE5.major;
		}

		public static JAVA getJava(int major, @SuppressWarnings("unused") int minor) {
			for (JAVA j : JAVA.values()) {
				if (j.major == major)
					return j;
			}
			return UNKNOWN;
		}

		public String getEE() {
			return ee;
		}

		public String getFilter() {
			return filter;
		}

		public Map<String, Set<String>> getProfiles() throws IOException {
			return null;
		}
	}

	public enum QUERY {
		IMPLEMENTS,
		EXTENDS,
		IMPORTS,
		NAMED,
		ANY,
		VERSION,
		CONCRETE,
		ABSTRACT,
		PUBLIC,
		ANNOTATED,
		INDIRECTLY_ANNOTATED,
		HIERARCHY_ANNOTATED,
		HIERARCHY_INDIRECTLY_ANNOTATED,
		RUNTIMEANNOTATIONS,
		CLASSANNOTATIONS,
		DEFAULT_CONSTRUCTOR;

	}

	public final static EnumSet<QUERY>	HAS_ARGUMENT	= EnumSet.of(QUERY.IMPLEMENTS, QUERY.EXTENDS, QUERY.IMPORTS,
		QUERY.NAMED, QUERY.VERSION, QUERY.ANNOTATED, QUERY.INDIRECTLY_ANNOTATED, QUERY.HIERARCHY_ANNOTATED,
		QUERY.HIERARCHY_INDIRECTLY_ANNOTATED);

	final static int					ACC_SYNTHETIC	= 0x1000;
	final static int					ACC_BRIDGE		= 0x0040;

	@Deprecated
	static protected class Assoc {
		private Assoc() {}
	}

	public abstract class Def {
		private final int access;

		public Def(int access) {
			this.access = access;
		}

		public int getAccess() {
			return access;
		}

		public boolean isEnum() {
			return Clazz.isEnum(getAccess());
		}

		public boolean isPublic() {
			return Modifier.isPublic(getAccess());
		}

		public boolean isAbstract() {
			return Modifier.isAbstract(getAccess());
		}

		public boolean isProtected() {
			return Modifier.isProtected(getAccess());
		}

		public boolean isFinal() {
			return Modifier.isFinal(getAccess());
		}

		public boolean isStatic() {
			return Modifier.isStatic(getAccess());
		}

		public boolean isPrivate() {
			return Modifier.isPrivate(getAccess());
		}

		public boolean isNative() {
			return Modifier.isNative(getAccess());
		}

		public boolean isTransient() {
			return Modifier.isTransient(getAccess());
		}

		public boolean isVolatile() {
			return Modifier.isVolatile(getAccess());
		}

		public boolean isInterface() {
			return Modifier.isInterface(getAccess());
		}

		public boolean isSynthetic() {
			return Clazz.isSynthetic(getAccess());
		}

		public boolean isModule() {
			return Clazz.isModule(getAccess());
		}

		public boolean isAnnotation() {
			return Clazz.isAnnotation(getAccess());
		}

		@Deprecated
		public Collection<TypeRef> getAnnotations() {
			return null;
		}

		public TypeRef getOwnerType() {
			return classDef.getType();
		}

		public abstract String getName();

		public abstract TypeRef getType();

		public abstract TypeRef[] getPrototype();

		public Object getClazz() {
			return Clazz.this;
		}
	}

	abstract class ElementDef extends Def {
		private final Attribute[] attributes;

		ElementDef(int access, Attribute[] attributes) {
			super(access);
			this.attributes = attributes;
		}

		Attribute[] attributes() {
			return attributes;
		}

		public boolean isDeprecated() {
			return attribute(DeprecatedAttribute.class).isPresent()
				|| annotationInfos(RuntimeVisibleAnnotationsAttribute.class)
					.anyMatch(a -> a.type.equals("Ljava/lang/Deprecated;"));
		}

		public String getSignature() {
			return attribute(SignatureAttribute.class).map(a -> a.signature)
				.orElse(null);
		}

		<A extends Attribute> Stream<A> attributes(Class<A> attributeType) {
			@SuppressWarnings("unchecked")
			Stream<A> stream = (Stream<A>) Arrays.stream(attributes())
				.filter(attributeType::isInstance);
			return stream;
		}

		<A extends Attribute> Optional<A> attribute(Class<A> attributeType) {
			return attributes(attributeType).findFirst();
		}

		<A extends AnnotationsAttribute> Stream<AnnotationInfo> annotationInfos(Class<A> attributeType) {
			return attributes(attributeType).flatMap(a -> Arrays.stream(a.annotations));
		}

		public Stream<Annotation> annotations(String binaryNameFilter) {
			Predicate<AnnotationInfo> matches = matches(binaryNameFilter);
			ElementType elementType = elementType();
			Stream<Annotation> runtimeAnnotations = annotationInfos(RuntimeVisibleAnnotationsAttribute.class)
				.filter(matches)
				.map(a -> newAnnotation(a, elementType, RetentionPolicy.RUNTIME, getAccess()));
			Stream<Annotation> classAnnotations = annotationInfos(RuntimeInvisibleAnnotationsAttribute.class)
				.filter(matches)
				.map(a -> newAnnotation(a, elementType, RetentionPolicy.CLASS, getAccess()));
			return Stream.concat(runtimeAnnotations, classAnnotations);
		}

		Predicate<AnnotationInfo> matches(String binaryNameFilter) {
			if ((binaryNameFilter == null) || binaryNameFilter.equals("*")) {
				return annotationInfo -> true;
			}
			Glob glob = new Glob("L{" + binaryNameFilter + "};");
			return annotationInfo -> glob.matches(annotationInfo.type);
		}

		<A extends TypeAnnotationsAttribute> Stream<TypeAnnotationInfo> typeAnnotationInfos(Class<A> attributeType) {
			return attributes(attributeType).flatMap(a -> Arrays.stream(a.type_annotations));
		}

		public Stream<TypeAnnotation> typeAnnotations(String binaryNameFilter) {
			Predicate<AnnotationInfo> matches = matches(binaryNameFilter);
			ElementType elementType = elementType();
			Stream<TypeAnnotation> runtimeTypeAnnotations = typeAnnotationInfos(
				RuntimeVisibleTypeAnnotationsAttribute.class).filter(matches)
					.map(a -> newTypeAnnotation(a, elementType, RetentionPolicy.RUNTIME, getAccess()));
			Stream<TypeAnnotation> classTypeAnnotations = typeAnnotationInfos(
				RuntimeInvisibleTypeAnnotationsAttribute.class).filter(matches)
					.map(a -> newTypeAnnotation(a, elementType, RetentionPolicy.CLASS, getAccess()));
			return Stream.concat(runtimeTypeAnnotations, classTypeAnnotations);
		}

		@Override
		public String getName() {
			return super.toString();
		}

		@Override
		public TypeRef getType() {
			return null;
		}

		@Override
		public TypeRef[] getPrototype() {
			return null;
		}

		@Override
		public String toString() {
			return getName();
		}

		abstract ElementType elementType();
	}

	class CodeDef extends ElementDef {
		private final ElementType elementType;

		CodeDef(CodeAttribute code, ElementType elementType) {
			super(0, code.attributes);
			this.elementType = elementType;
		}

		@Override
		ElementType elementType() {
			return elementType;
		}

		@Override
		public boolean isDeprecated() {
			return false;
		}
	}

	class ClassDef extends ElementDef {
		private final TypeRef type;

		ClassDef(ClassFile classFile) {
			super(classFile.access, classFile.attributes);
			type = analyzer.getTypeRef(classFile.this_class);
		}

		String getSourceFile() {
			return attribute(SourceFileAttribute.class).map(a -> a.sourcefile)
				.orElse(null);
		}

		boolean isInnerClass() {
			String binary = type.getBinary();
			return attributes(InnerClassesAttribute.class).flatMap(a -> Arrays.stream(a.classes))
				.anyMatch(inner -> inner.inner_class.equals(binary) && !Modifier.isStatic(inner.inner_access));
		}

		@Override
		public String getName() {
			return type.getFQN();
		}

		@Override
		public TypeRef getType() {
			return type;
		}

		@Override
		ElementType elementType() {
			if (isAnnotation()) {
				return ElementType.ANNOTATION_TYPE;
			}
			if (isModule()) {
				return ElementType.MODULE;
			}
			return type.getBinary()
				.endsWith("/package-info") ? ElementType.PACKAGE : ElementType.TYPE;
		}
	}

	public class FieldDef extends ElementDef {
		private final String	name;
		private final String	descriptor;

		@Deprecated
		public FieldDef(int access, String name, String descriptor) {
			super(access, new Attribute[0]);
			this.name = name;
			this.descriptor = descriptor;
		}

		FieldDef(MemberInfo memberInfo) {
			super(memberInfo.access, memberInfo.attributes);
			this.name = memberInfo.name;
			this.descriptor = memberInfo.descriptor;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public TypeRef getType() {
			return getDescriptor().getType();
		}

		@Deprecated
		public void setDeprecated(boolean deprecated) {}

		public TypeRef getContainingClass() {
			return getClassName();
		}

		public String descriptor() {
			return descriptor;
		}

		public Descriptor getDescriptor() {
			return analyzer.getDescriptor(descriptor());
		}

		@Deprecated
		public void setConstant(Object o) {}

		public Object getConstant() {
			return attribute(ConstantValueAttribute.class).map(a -> a.value)
				.orElse(null);
		}

		public String getGenericReturnType() {
			String signature = getSignature();
			FieldSignature sig = analyzer.getFieldSignature((signature != null) ? signature : descriptor());
			return sig.type.toString();
		}

		@Override
		public TypeRef[] getPrototype() {
			return null;
		}

		@Override
		ElementType elementType() {
			return ElementType.FIELD;
		}
	}

	public static class MethodParameter {
		private final MethodParametersAttribute.MethodParameter methodParameter;

		MethodParameter(MethodParametersAttribute.MethodParameter methodParameter) {
			this.methodParameter = methodParameter;
		}

		public String getName() {
			return methodParameter.name;
		}

		public int getAccess() {
			return methodParameter.access_flags;
		}

		@Override
		public String toString() {
			return getName();
		}

		static MethodParameter[] parameters(MethodParametersAttribute attribute) {
			int parameters_count = attribute.parameters.length;
			MethodParameter[] parameters = new MethodParameter[parameters_count];
			for (int i = 0; i < parameters_count; i++) {
				parameters[i] = new MethodParameter(attribute.parameters[i]);
			}
			return parameters;
		}
	}

	public class MethodDef extends FieldDef {
		@Deprecated
		public MethodDef(int access, String method, String descriptor) {
			super(access, method, descriptor);
		}

		public MethodDef(MethodInfo methodInfo) {
			super(methodInfo);
		}

		public boolean isConstructor() {
			String name = getName();
			return name.equals("<init>") || name.equals("<clinit>");
		}

		@Override
		public boolean isFinal() {
			return super.isFinal() || Modifier.isFinal(classDef.getAccess());
		}

		@Override
		public TypeRef[] getPrototype() {
			return getDescriptor().getPrototype();
		}

		public boolean isBridge() {
			return (getAccess() & ACC_BRIDGE) != 0;
		}

		@Override
		public String getGenericReturnType() {
			String signature = getSignature();
			MethodSignature sig = analyzer.getMethodSignature((signature != null) ? signature : descriptor());
			return sig.resultType.toString();
		}

		public MethodParameter[] getParameters() {
			return attribute(MethodParametersAttribute.class).map(MethodParameter::parameters)
				.orElseGet(() -> new MethodParameter[0]);
		}

		@Override
		public Object getConstant() {
			return attribute(AnnotationDefaultAttribute.class).map(a -> annotationDefault(a, getAccess()))
				.orElse(null);
		}

		<A extends ParameterAnnotationsAttribute> Stream<ParameterAnnotationInfo> parameterAnnotationInfos(
			Class<A> attributeType) {
			return attributes(attributeType).flatMap(a -> Arrays.stream(a.parameter_annotations));
		}

		public Stream<ParameterAnnotation> parameterAnnotations(String binaryNameFilter) {
			Predicate<AnnotationInfo> matches = matches(binaryNameFilter);
			ElementType elementType = elementType();
			Stream<ParameterAnnotation> runtimeParameterAnnotations = parameterAnnotationInfos(
				RuntimeVisibleParameterAnnotationsAttribute.class)
					.flatMap(a -> parameterAnnotations(a, matches, elementType, RetentionPolicy.RUNTIME));
			Stream<ParameterAnnotation> classParameterAnnotations = parameterAnnotationInfos(
				RuntimeInvisibleParameterAnnotationsAttribute.class)
					.flatMap(a -> parameterAnnotations(a, matches, elementType, RetentionPolicy.CLASS));
			return Stream.concat(runtimeParameterAnnotations, classParameterAnnotations);
		}

		private Stream<ParameterAnnotation> parameterAnnotations(ParameterAnnotationInfo parameterAnnotationInfo,
			Predicate<AnnotationInfo> matches, ElementType elementType, RetentionPolicy policy) {
			int parameter = parameterAnnotationInfo.parameter;
			return Arrays.stream(parameterAnnotationInfo.annotations)
				.filter(matches)
				.map(a -> newParameterAnnotation(parameter, a, elementType, policy, getAccess()));
		}

		/**
		 * We must also look in the method's Code attribute for type
		 * annotations.
		 */
		@Override
		<A extends TypeAnnotationsAttribute> Stream<TypeAnnotationInfo> typeAnnotationInfos(Class<A> attributeType) {
			ElementType elementType = elementType();
			Stream<A> methodAttributes = attributes(attributeType);
			Stream<A> codeAttributes = attribute(CodeAttribute.class)
				.map(code -> new CodeDef(code, elementType).attributes(attributeType))
				.orElseGet(Stream::empty);
			return Stream.concat(methodAttributes, codeAttributes)
				.flatMap(a -> Arrays.stream(a.type_annotations));
		}

		@Override
		ElementType elementType() {
			return getName().equals("<init>") ? ElementType.CONSTRUCTOR : ElementType.METHOD;
		}
	}

	public class TypeDef extends Def {
		final TypeRef	type;
		final boolean	interf;

		public TypeDef(TypeRef type, boolean interf) {
			super(Modifier.PUBLIC);
			this.type = type;
			this.interf = interf;
		}

		public TypeRef getReference() {
			return type;
		}

		public boolean getImplements() {
			return interf;
		}

		@Override
		public String getName() {
			if (interf)
				return "<implements>";
			return "<extends>";
		}

		@Override
		public TypeRef getType() {
			return type;
		}

		@Override
		public TypeRef[] getPrototype() {
			return null;
		}
	}

	public static final Comparator<Clazz>	NAME_COMPARATOR					= (Clazz a,
		Clazz b) -> a.classFile.this_class.compareTo(b.classFile.this_class);

	private boolean							hasRuntimeAnnotations;
	private boolean							hasClassAnnotations;
	private boolean							hasDefaultConstructor;

	private Set<PackageRef>					imports							= Create.set();
	private Set<TypeRef>					xref							= new HashSet<>();
	private Set<TypeRef>					annotations;
	private int								forName							= 0;
	private int								class$							= 0;
	private Set<PackageRef>					api;

	private ClassFile						classFile						= null;
	private ConstantPool					constantPool					= null;
	TypeRef									superClass;
	private TypeRef[]						interfaces;
	ClassDef								classDef;

	private Map<TypeRef, Integer>			referred						= null;

	final Analyzer							analyzer;
	final String							path;
	final Resource							resource;

	public static final int					TYPEUSE_INDEX_NONE				= TypeAnnotationInfo.TYPEUSE_INDEX_NONE;
	public static final int					TYPEUSE_TARGET_INDEX_EXTENDS	= TypeAnnotationInfo.TYPEUSE_TARGET_INDEX_EXTENDS;

	public Clazz(Analyzer analyzer, String path, Resource resource) {
		this.path = path;
		this.resource = resource;
		this.analyzer = analyzer;
	}

	public Set<TypeRef> parseClassFile() throws Exception {
		return parseClassFileWithCollector(null);
	}

	public Set<TypeRef> parseClassFile(InputStream in) throws Exception {
		return parseClassFile(in, null);
	}

	public Set<TypeRef> parseClassFileWithCollector(ClassDataCollector cd) throws Exception {
		ByteBuffer bb = resource.buffer();
		if (bb != null) {
			return parseClassFileData(ByteBufferDataInput.wrap(bb), cd);
		}
		return parseClassFile(resource.openInputStream(), cd);
	}

	public Set<TypeRef> parseClassFile(InputStream in, ClassDataCollector cd) throws Exception {
		try (DataInputStream din = new DataInputStream(in)) {
			return parseClassFileData(din, cd);
		}
	}

	private Set<TypeRef> parseClassFileData(DataInput in, ClassDataCollector cd) throws Exception {
		Set<TypeRef> xref = parseClassFileData(in);
		visitClassFile(cd);
		return xref;
	}

	private synchronized Set<TypeRef> parseClassFileData(DataInput in) throws Exception {
		if (classFile != null) {
			return xref;
		}

		logger.debug("parseClassFile(): path={} resource={}", path, resource);

		classFile = ClassFile.parseClassFile(in);
		classDef = new ClassDef(classFile);
		constantPool = classFile.constant_pool;
		referred = new HashMap<>(constantPool.size());

		if (classDef.isPublic()) {
			api = new HashSet<>();
		}
		if (!classDef.isModule()) {
			referTo(classDef.getType(), Modifier.PUBLIC);
		}

		String superName = classFile.super_class;
		if (superName == null) {
			if (!(classDef.getType()
				.isObject() || classDef.isModule())) {
				throw new IOException("Class does not have a super class and is not java.lang.Object or module-info");
			}
		} else {
			superClass = analyzer.getTypeRef(superName);
			referTo(superClass, classFile.access);
		}

		int interfaces_count = classFile.interfaces.length;
		if (interfaces_count > 0) {
			interfaces = new TypeRef[interfaces_count];
			for (int i = 0; i < interfaces_count; i++) {
				interfaces[i] = analyzer.getTypeRef(classFile.interfaces[i]);
				referTo(interfaces[i], classFile.access);
			}
		}

		// All name&type and class constant records contain descriptors we
		// must treat as references, though not API
		int constant_pool_count = constantPool.size();
		for (int i = 1; i < constant_pool_count; i++) {
			switch (constantPool.tag(i)) {
				case CONSTANT_Fieldref :
				case CONSTANT_Methodref :
				case CONSTANT_InterfaceMethodref : {
					AbstractRefInfo info = constantPool.entry(i);
					classConstRef(constantPool.className(info.class_index));
					break;
				}
				case CONSTANT_NameAndType : {
					NameAndTypeInfo info = constantPool.entry(i);
					referTo(constantPool.utf8(info.descriptor_index), 0);
					break;
				}
				case CONSTANT_MethodType : {
					MethodTypeInfo info = constantPool.entry(i);
					referTo(constantPool.utf8(info.descriptor_index), 0);
					break;
				}
				default :
					break;
			}
		}

		for (FieldInfo fieldInfo : classFile.fields) {
			referTo(fieldInfo.descriptor, fieldInfo.access);
			processAttributes(fieldInfo.attributes, elementType(fieldInfo), fieldInfo.access);
		}

		// We crawl the code to find the instruction sequence:
		//
		// ldc(_w) <string constant>
		// invokestatic Class.forName(String)
		//
		// We calculate the method reference index so we can do this
		// efficiently during code inspection.
		forName = analyzer.is(Constants.NOCLASSFORNAME) ? -1
			: findMethodReference("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
		class$ = findMethodReference(classFile.this_class, "class$", "(Ljava/lang/String;)Ljava/lang/Class;");

		for (MethodInfo methodInfo : classFile.methods) {
			referTo(methodInfo.descriptor, methodInfo.access);
			ElementType elementType = elementType(methodInfo);
			if ((elementType == ElementType.CONSTRUCTOR) && Modifier.isPublic(methodInfo.access)
				&& methodInfo.descriptor.equals("()V")) {
				hasDefaultConstructor = true;
			}
			processAttributes(methodInfo.attributes, elementType, methodInfo.access);
		}

		processAttributes(classFile.attributes, elementType(classFile), classFile.access);

		return xref;
	}

	private void visitClassFile(ClassDataCollector cd) throws Exception {
		if (cd == null) {
			return;
		}
		logger.debug("visitClassFile(): path={} resource={}", path, resource);

		if (!cd.classStart(this)) {
			return;
		}
		try {
			cd.version(classFile.minor_version, classFile.major_version);
			if (superClass != null) {
				cd.extendsClass(superClass);
			}
			if (interfaces != null) {
				cd.implementsInterfaces(interfaces);
			}

			referred.forEach((typeRef, access) -> {
				cd.addReference(typeRef);
				cd.referTo(typeRef, access.intValue());
			});

			for (FieldInfo fieldInfo : classFile.fields) {
				FieldDef fieldDef = new FieldDef(fieldInfo);
				cd.field(fieldDef);
				visitAttributes(cd, fieldDef);
			}

			for (MethodInfo methodInfo : classFile.methods) {
				MethodDef methodDef = new MethodDef(methodInfo);
				cd.method(methodDef);
				visitAttributes(cd, methodDef);
			}

			cd.memberEnd();

			visitAttributes(cd, classDef);
		} finally {
			cd.classEnd();
		}
	}

	public Stream<FieldDef> fields() {
		return Arrays.stream(classFile.fields)
			.map(FieldDef::new);
	}

	public Stream<MethodDef> methods() {
		return Arrays.stream(classFile.methods)
			.map(MethodDef::new);
	}

	/**
	 * Find a method reference in the pool that points to the given class,
	 * methodname and descriptor.
	 *
	 * @param clazz
	 * @param methodname
	 * @param descriptor
	 * @return index in constant pool
	 */
	private int findMethodReference(String clazz, String methodname, String descriptor) {
		int constant_pool_count = constantPool.size();
		for (int i = 1; i < constant_pool_count; i++) {
			switch (constantPool.tag(i)) {
				case CONSTANT_Methodref :
				case CONSTANT_InterfaceMethodref :
					AbstractRefInfo refInfo = constantPool.entry(i);
					if (clazz.equals(constantPool.className(refInfo.class_index))) {
						NameAndTypeInfo nameAndTypeInfo = constantPool.entry(refInfo.name_and_type_index);
						if (methodname.equals(constantPool.utf8(nameAndTypeInfo.name_index))
							&& descriptor.equals(constantPool.utf8(nameAndTypeInfo.descriptor_index))) {
							return i;
						}
					}
			}
		}
		return -1;
	}

	/**
	 * Called for the attributes in the class, field, method or Code attribute.
	 */
	private void processAttributes(Attribute[] attributes, ElementType elementType, int access_flags) {
		for (Attribute attribute : attributes) {
			switch (attribute.name()) {
				case RuntimeVisibleAnnotationsAttribute.NAME :
					processAnnotations((AnnotationsAttribute) attribute, elementType, RetentionPolicy.RUNTIME,
						access_flags);
					break;
				case RuntimeInvisibleAnnotationsAttribute.NAME :
					processAnnotations((AnnotationsAttribute) attribute, elementType, RetentionPolicy.CLASS,
						access_flags);
					break;
				case RuntimeVisibleParameterAnnotationsAttribute.NAME :
					processParameterAnnotations((ParameterAnnotationsAttribute) attribute, ElementType.PARAMETER,
						RetentionPolicy.RUNTIME, access_flags);
					break;
				case RuntimeInvisibleParameterAnnotationsAttribute.NAME :
					processParameterAnnotations((ParameterAnnotationsAttribute) attribute, ElementType.PARAMETER,
						RetentionPolicy.CLASS, access_flags);
					break;
				case RuntimeVisibleTypeAnnotationsAttribute.NAME :
					processTypeAnnotations((TypeAnnotationsAttribute) attribute, ElementType.TYPE_USE,
						RetentionPolicy.RUNTIME, access_flags);
					break;
				case RuntimeInvisibleTypeAnnotationsAttribute.NAME :
					processTypeAnnotations((TypeAnnotationsAttribute) attribute, ElementType.TYPE_USE,
						RetentionPolicy.CLASS, access_flags);
					break;
				case EnclosingMethodAttribute.NAME :
					processEnclosingMethod((EnclosingMethodAttribute) attribute);
					break;
				case CodeAttribute.NAME :
					processCode((CodeAttribute) attribute, elementType);
					break;
				case SignatureAttribute.NAME :
					processSignature((SignatureAttribute) attribute, elementType, access_flags);
					break;
				case AnnotationDefaultAttribute.NAME :
					processAnnotationDefault((AnnotationDefaultAttribute) attribute, elementType, access_flags);
					break;
				case ExceptionsAttribute.NAME :
					processExceptions((ExceptionsAttribute) attribute, access_flags);
					break;
				case BootstrapMethodsAttribute.NAME :
					processBootstrapMethods((BootstrapMethodsAttribute) attribute);
					break;
				case StackMapTableAttribute.NAME :
					processStackMapTable((StackMapTableAttribute) attribute);
					break;
				default :
					break;
			}
		}
	}

	/**
	 * Called for the attributes in the class, field, or method.
	 */
	private void visitAttributes(ClassDataCollector cd, ElementDef elementDef) throws Exception {
		int access_flags = elementDef.getAccess();
		ElementType elementType = elementDef.elementType();
		if (elementDef.isDeprecated()) {
			cd.deprecated();
		}
		for (Attribute attribute : elementDef.attributes()) {
			switch (attribute.name()) {
				case RuntimeVisibleAnnotationsAttribute.NAME :
					visitAnnotations(cd, (AnnotationsAttribute) attribute, elementType, RetentionPolicy.RUNTIME,
						access_flags);
					break;
				case RuntimeInvisibleAnnotationsAttribute.NAME :
					visitAnnotations(cd, (AnnotationsAttribute) attribute, elementType, RetentionPolicy.CLASS,
						access_flags);
					break;
				case RuntimeVisibleParameterAnnotationsAttribute.NAME :
					visitParameterAnnotations(cd, (ParameterAnnotationsAttribute) attribute, ElementType.PARAMETER,
						RetentionPolicy.RUNTIME, access_flags);
					break;
				case RuntimeInvisibleParameterAnnotationsAttribute.NAME :
					visitParameterAnnotations(cd, (ParameterAnnotationsAttribute) attribute, ElementType.PARAMETER,
						RetentionPolicy.CLASS, access_flags);
					break;
				case RuntimeVisibleTypeAnnotationsAttribute.NAME :
					visitTypeAnnotations(cd, (TypeAnnotationsAttribute) attribute, ElementType.TYPE_USE,
						RetentionPolicy.RUNTIME, access_flags);
					break;
				case RuntimeInvisibleTypeAnnotationsAttribute.NAME :
					visitTypeAnnotations(cd, (TypeAnnotationsAttribute) attribute, ElementType.TYPE_USE,
						RetentionPolicy.CLASS, access_flags);
					break;
				case InnerClassesAttribute.NAME :
					visitInnerClasses(cd, (InnerClassesAttribute) attribute);
					break;
				case EnclosingMethodAttribute.NAME :
					visitEnclosingMethod(cd, (EnclosingMethodAttribute) attribute);
					break;
				case CodeAttribute.NAME :
					visitCode(cd, (CodeAttribute) attribute, elementType);
					break;
				case SignatureAttribute.NAME :
					visitSignature(cd, (SignatureAttribute) attribute);
					break;
				case ConstantValueAttribute.NAME :
					visitConstantValue(cd, (ConstantValueAttribute) attribute);
					break;
				case AnnotationDefaultAttribute.NAME :
					visitAnnotationDefault(cd, (AnnotationDefaultAttribute) attribute, elementDef);
					break;
				case MethodParametersAttribute.NAME :
					visitMethodParameters(cd, (MethodParametersAttribute) attribute, elementDef);
					break;
				default :
					break;
			}
		}
	}

	private void processEnclosingMethod(EnclosingMethodAttribute attribute) {
		classConstRef(attribute.class_name);
	}

	private void visitEnclosingMethod(ClassDataCollector cd, EnclosingMethodAttribute attribute) {
		TypeRef cName = analyzer.getTypeRef(attribute.class_name);
		cd.enclosingMethod(cName, attribute.method_name, attribute.method_descriptor);
	}

	private void visitInnerClasses(ClassDataCollector cd, InnerClassesAttribute attribute) throws Exception {
		for (InnerClass innerClassInfo : attribute.classes) {
			TypeRef innerClass = analyzer.getTypeRef(innerClassInfo.inner_class);
			TypeRef outerClass;
			String outerClassName = innerClassInfo.outer_class;
			if (outerClassName != null) {
				outerClass = analyzer.getTypeRef(outerClassName);
			} else {
				outerClass = null;
			}

			cd.innerClass(innerClass, outerClass, innerClassInfo.inner_name, innerClassInfo.inner_access);
		}
	}

	private void processSignature(SignatureAttribute attribute, ElementType elementType, int access_flags) {
		if (isSynthetic(access_flags)) {
			return; // Ignore generic signatures on synthetic elements
		}
		String signature = attribute.signature;
		Signature sig;
		switch (elementType) {
			case ANNOTATION_TYPE :
			case TYPE :
			case PACKAGE :
				sig = analyzer.getClassSignature(signature);
				break;
			case FIELD :
				sig = analyzer.getFieldSignature(signature);
				break;
			case CONSTRUCTOR :
			case METHOD :
				sig = analyzer.getMethodSignature(signature);
				break;
			default :
				throw new IllegalArgumentException(
					"Signature \"" + signature + "\" found for unknown element type: " + elementType);
		}
		Set<String> binaryRefs = sig.erasedBinaryReferences();
		for (String binary : binaryRefs) {
			TypeRef ref = analyzer.getTypeRef(binary);
			referTo(ref, access_flags);
		}
	}

	private void visitSignature(ClassDataCollector cd, SignatureAttribute attribute) {
		String signature = attribute.signature;
		cd.signature(signature);
	}

	private void processAnnotationDefault(AnnotationDefaultAttribute attribute, ElementType elementType,
		int access_flags) {
		Object value = attribute.value;
		processElementValue(value, elementType, RetentionPolicy.RUNTIME, access_flags);
	}

	private void visitAnnotationDefault(ClassDataCollector cd, AnnotationDefaultAttribute attribute,
		ElementDef elementDef) {
		MethodDef methodDef = (MethodDef) elementDef;
		Object value = annotationDefault(attribute, methodDef.getAccess());
		cd.annotationDefault(methodDef, value);
	}

	static ElementType elementType(FieldInfo fieldInfo) {
		return ElementType.FIELD;
	}

	static ElementType elementType(MethodInfo methodInfo) {
		return methodInfo.name.equals("<init>") ? ElementType.CONSTRUCTOR : ElementType.METHOD;
	}

	static ElementType elementType(ClassFile classFile) {
		if (isAnnotation(classFile.access)) {
			return ElementType.ANNOTATION_TYPE;
		}
		if (isModule(classFile.access)) {
			return ElementType.MODULE;
		}
		return classFile.this_class.endsWith("/package-info") ? ElementType.PACKAGE : ElementType.TYPE;
	}

	Object annotationDefault(AnnotationDefaultAttribute attribute, int access_flags) {
		try {
			return newElementValue(attribute.value, ElementType.METHOD, RetentionPolicy.RUNTIME, access_flags);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void visitConstantValue(ClassDataCollector cd, ConstantValueAttribute attribute) {
		Object value = attribute.value;
		cd.constant(value);
	}

	private void processExceptions(ExceptionsAttribute attribute, int access_flags) {
		for (String exception : attribute.exceptions) {
			TypeRef clazz = analyzer.getTypeRef(exception);
			referTo(clazz, access_flags);
		}
	}

	private void visitMethodParameters(ClassDataCollector cd, MethodParametersAttribute attribute,
		ElementDef elementDef) {
		MethodDef method = (MethodDef) elementDef;
		cd.methodParameters(method, MethodParameter.parameters(attribute));
	}

	private void processCode(CodeAttribute attribute, ElementType elementType) {
		ByteBuffer code = attribute.code.duplicate();
		code.rewind();
		int lastReference = -1;
		while (code.hasRemaining()) {
			int instruction = Byte.toUnsignedInt(code.get());
			switch (instruction) {
				case OpCodes.ldc : {
					lastReference = Byte.toUnsignedInt(code.get());
					classConstRef(lastReference);
					break;
				}
				case OpCodes.ldc_w : {
					lastReference = Short.toUnsignedInt(code.getShort());
					classConstRef(lastReference);
					break;
				}
				case OpCodes.anewarray :
				case OpCodes.checkcast :
				case OpCodes.instanceof_ :
				case OpCodes.new_ : {
					int class_index = Short.toUnsignedInt(code.getShort());
					classConstRef(class_index);
					lastReference = -1;
					break;
				}
				case OpCodes.multianewarray : {
					int class_index = Short.toUnsignedInt(code.getShort());
					classConstRef(class_index);
					code.get();
					lastReference = -1;
					break;
				}
				case OpCodes.invokestatic : {
					int method_ref_index = Short.toUnsignedInt(code.getShort());
					if ((method_ref_index == forName || method_ref_index == class$) && lastReference != -1) {
						if (constantPool.tag(lastReference) == CONSTANT_String) {
							String fqn = constantPool.string(lastReference);
							if (!fqn.equals("class") && fqn.indexOf('.') > 0) {
								TypeRef typeRef = analyzer.getTypeRefFromFQN(fqn);
								referTo(typeRef, 0);
							}
						}
					}
					lastReference = -1;
					break;
				}
				case OpCodes.wide : {
					int opcode = Byte.toUnsignedInt(code.get());
					code.position(code.position() + (opcode == OpCodes.iinc ? 4 : 2));
					lastReference = -1;
					break;
				}
				case OpCodes.tableswitch : {
					// Skip to place divisible by 4
					int rem = code.position() % 4;
					if (rem != 0) {
						code.position(code.position() + 4 - rem);
					}
					int deflt = code.getInt();
					int low = code.getInt();
					int high = code.getInt();
					code.position(code.position() + (high - low + 1) * 4);
					lastReference = -1;
					break;
				}
				case OpCodes.lookupswitch : {
					// Skip to place divisible by 4
					int rem = code.position() % 4;
					if (rem != 0) {
						code.position(code.position() + 4 - rem);
					}
					int deflt = code.getInt();
					int npairs = code.getInt();
					code.position(code.position() + npairs * 8);
					lastReference = -1;
					break;
				}
				default : {
					code.position(code.position() + OpCodes.OFFSETS[instruction]);
					lastReference = -1;
					break;
				}
			}
		}
		for (ExceptionHandler exceptionHandler : attribute.exception_table) {
			classConstRef(exceptionHandler.catch_type);
		}
		processAttributes(attribute.attributes, elementType, 0);
	}

	private void visitCode(ClassDataCollector cd, CodeAttribute attribute, ElementType elementType) throws Exception {
		ByteBuffer code = attribute.code.duplicate();
		code.rewind();
		while (code.hasRemaining()) {
			int instruction = Byte.toUnsignedInt(code.get());
			switch (instruction) {
				case OpCodes.invokespecial : {
					int method_ref_index = Short.toUnsignedInt(code.getShort());
					visitReferenceMethod(cd, method_ref_index);
					break;
				}
				case OpCodes.invokevirtual : {
					int method_ref_index = Short.toUnsignedInt(code.getShort());
					visitReferenceMethod(cd, method_ref_index);
					break;
				}
				case OpCodes.invokeinterface : {
					int method_ref_index = Short.toUnsignedInt(code.getShort());
					visitReferenceMethod(cd, method_ref_index);
					code.position(code.position() + 2);
					break;
				}
				case OpCodes.invokestatic : {
					int method_ref_index = Short.toUnsignedInt(code.getShort());
					visitReferenceMethod(cd, method_ref_index);
					break;
				}
				case OpCodes.wide : {
					int opcode = Byte.toUnsignedInt(code.get());
					code.position(code.position() + (opcode == OpCodes.iinc ? 4 : 2));
					break;
				}
				case OpCodes.tableswitch : {
					// Skip to place divisible by 4
					int rem = code.position() % 4;
					if (rem != 0) {
						code.position(code.position() + 4 - rem);
					}
					int deflt = code.getInt();
					int low = code.getInt();
					int high = code.getInt();
					code.position(code.position() + (high - low + 1) * 4);
					break;
				}
				case OpCodes.lookupswitch : {
					// Skip to place divisible by 4
					int rem = code.position() % 4;
					if (rem != 0) {
						code.position(code.position() + 4 - rem);
					}
					int deflt = code.getInt();
					int npairs = code.getInt();
					code.position(code.position() + npairs * 8);
					break;
				}
				default : {
					code.position(code.position() + OpCodes.OFFSETS[instruction]);
					break;
				}
			}
		}

		CodeDef codeDef = new CodeDef(attribute, elementType);
		visitAttributes(cd, codeDef);
	}

	/**
	 * Called when crawling the byte code and a method reference is found
	 */
	private void visitReferenceMethod(ClassDataCollector cd, int method_ref_index) {
		AbstractRefInfo refInfo = constantPool.entry(method_ref_index);
		String className = constantPool.className(refInfo.class_index);
		NameAndTypeInfo nameAndTypeInfo = constantPool.entry(refInfo.name_and_type_index);
		String method = constantPool.utf8(nameAndTypeInfo.name_index);
		String descriptor = constantPool.utf8(nameAndTypeInfo.descriptor_index);
		TypeRef type = analyzer.getTypeRef(className);
		cd.referenceMethod(0, type, method, descriptor);
	}

	private void processParameterAnnotations(ParameterAnnotationsAttribute attribute, ElementType elementType,
		RetentionPolicy policy, int access_flags) {
		for (ParameterAnnotationInfo parameterAnnotationInfo : attribute.parameter_annotations) {
			for (AnnotationInfo annotationInfo : parameterAnnotationInfo.annotations) {
				processAnnotation(annotationInfo, elementType, policy, access_flags);
			}
		}
	}

	private void visitParameterAnnotations(ClassDataCollector cd, ParameterAnnotationsAttribute attribute,
		ElementType elementType, RetentionPolicy policy, int access_flags) throws Exception {
		for (ParameterAnnotationInfo parameterAnnotationInfo : attribute.parameter_annotations) {
			if (parameterAnnotationInfo.annotations.length > 0) {
				cd.parameter(parameterAnnotationInfo.parameter);
				for (AnnotationInfo annotationInfo : parameterAnnotationInfo.annotations) {
					Annotation annotation = newAnnotation(annotationInfo, elementType, policy, access_flags);
					cd.annotation(annotation);
				}
			}
		}
	}

	private void processTypeAnnotations(TypeAnnotationsAttribute attribute, ElementType elementType,
		RetentionPolicy policy, int access_flags) {
		for (TypeAnnotationInfo typeAnnotationInfo : attribute.type_annotations) {
			processAnnotation(typeAnnotationInfo, elementType, policy, access_flags);
		}
	}

	private void visitTypeAnnotations(ClassDataCollector cd, TypeAnnotationsAttribute attribute,
		ElementType elementType, RetentionPolicy policy, int access_flags) throws Exception {
		for (TypeAnnotationInfo typeAnnotationInfo : attribute.type_annotations) {
			cd.typeuse(typeAnnotationInfo.target_type, typeAnnotationInfo.target_index, typeAnnotationInfo.target_info,
				typeAnnotationInfo.type_path);
			Annotation annotation = newAnnotation(typeAnnotationInfo, elementType, policy, access_flags);
			cd.annotation(annotation);
		}
	}

	private void processAnnotations(AnnotationsAttribute attribute, ElementType elementType, RetentionPolicy policy,
		int access_flags) {
		for (AnnotationInfo annotationInfo : attribute.annotations) {
			processAnnotation(annotationInfo, elementType, policy, access_flags);
		}
	}

	private void visitAnnotations(ClassDataCollector cd, AnnotationsAttribute attribute, ElementType elementType,
		RetentionPolicy policy, int access_flags) throws Exception {
		for (AnnotationInfo annotationInfo : attribute.annotations) {
			Annotation annotation = newAnnotation(annotationInfo, elementType, policy, access_flags);
			cd.annotation(annotation);
		}
	}

	private void processAnnotation(AnnotationInfo annotationInfo, ElementType elementType, RetentionPolicy policy,
		int access_flags) {
		if (annotations == null) {
			annotations = new HashSet<>();
		}

		String typeName = annotationInfo.type;
		TypeRef typeRef = analyzer.getTypeRef(typeName);
		annotations.add(typeRef);

		if (policy == RetentionPolicy.RUNTIME) {
			referTo(typeRef, 0);
			hasRuntimeAnnotations = true;
			if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags))) {
				api.add(typeRef.getPackageRef());
			}
		} else {
			hasClassAnnotations = true;
		}
		for (ElementValueInfo elementValueInfo : annotationInfo.values) {
			processElementValue(elementValueInfo.value, elementType, policy, access_flags);
		}
	}

	Annotation newAnnotation(AnnotationInfo annotationInfo, ElementType elementType, RetentionPolicy policy,
		int access_flags) {
		String typeName = annotationInfo.type;
		TypeRef typeRef = analyzer.getTypeRef(typeName);
		Map<String, Object> elements = annotationValues(annotationInfo.values, elementType, policy, access_flags);
		return new Annotation(typeRef, elements, elementType, policy);
	}

	ParameterAnnotation newParameterAnnotation(int parameter, AnnotationInfo annotationInfo, ElementType elementType,
		RetentionPolicy policy, int access_flags) {
		String typeName = annotationInfo.type;
		TypeRef typeRef = analyzer.getTypeRef(typeName);
		Map<String, Object> elements = annotationValues(annotationInfo.values, elementType, policy, access_flags);
		return new ParameterAnnotation(parameter, typeRef, elements, elementType, policy);
	}

	TypeAnnotation newTypeAnnotation(TypeAnnotationInfo annotationInfo, ElementType elementType, RetentionPolicy policy,
		int access_flags) {
		String typeName = annotationInfo.type;
		TypeRef typeRef = analyzer.getTypeRef(typeName);
		Map<String, Object> elements = annotationValues(annotationInfo.values, elementType, policy, access_flags);
		return new TypeAnnotation(annotationInfo.target_type, annotationInfo.target_info, annotationInfo.target_index,
			annotationInfo.type_path, typeRef, elements, elementType, policy);
	}

	private Map<String, Object> annotationValues(ElementValueInfo[] values, ElementType elementType,
		RetentionPolicy policy, int access_flags) {
		Map<String, Object> elements = new LinkedHashMap<>();
		for (ElementValueInfo elementValueInfo : values) {
			String element = elementValueInfo.name;
			Object value = newElementValue(elementValueInfo.value, elementType, policy, access_flags);
			elements.put(element, value);
		}
		return elements;
	}

	private void processElementValue(Object value, ElementType elementType, RetentionPolicy policy, int access_flags) {
		if (value instanceof EnumConst) {
			if (policy == RetentionPolicy.RUNTIME) {
				EnumConst enumConst = (EnumConst) value;
				TypeRef name = analyzer.getTypeRef(enumConst.type);
				referTo(name, 0);
				if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags))) {
					api.add(name.getPackageRef());
				}
			}
		} else if (value instanceof ResultConst) {
			if (policy == RetentionPolicy.RUNTIME) {
				ResultConst resultConst = (ResultConst) value;
				TypeRef name = analyzer.getTypeRef(resultConst.descriptor);
				if (!name.isPrimitive()) {
					PackageRef packageRef = name.getPackageRef();
					if (!packageRef.isPrimitivePackage()) {
						referTo(name, 0);
						if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags))) {
							api.add(packageRef);
						}
					}
				}
			}
		} else if (value instanceof AnnotationInfo) {
			processAnnotation((AnnotationInfo) value, elementType, policy, access_flags);
		} else if (value instanceof Object[]) {
			Object[] array = (Object[]) value;
			int num_values = array.length;
			for (int i = 0; i < num_values; i++) {
				processElementValue(array[i], elementType, policy, access_flags);
			}
		}
	}

	private Object newElementValue(Object value, ElementType elementType, RetentionPolicy policy, int access_flags) {
		if (value instanceof EnumConst) {
			EnumConst enumConst = (EnumConst) value;
			return enumConst.name;
		} else if (value instanceof ResultConst) {
			ResultConst resultConst = (ResultConst) value;
			TypeRef name = analyzer.getTypeRef(resultConst.descriptor);
			return name;
		} else if (value instanceof AnnotationInfo) {
			return newAnnotation((AnnotationInfo) value, elementType, policy, access_flags);
		} else if (value instanceof Object[]) {
			Object[] array = (Object[]) value;
			int num_values = array.length;
			Object[] result = new Object[num_values];
			for (int i = 0; i < num_values; i++) {
				result[i] = newElementValue(array[i], elementType, policy, access_flags);
			}
			return result;
		} else {
			return value;
		}
	}

	private void processBootstrapMethods(BootstrapMethodsAttribute attribute) {
		for (BootstrapMethod bootstrapMethod : attribute.bootstrap_methods) {
			for (int bootstrap_argument : bootstrapMethod.bootstrap_arguments) {
				classConstRef(bootstrap_argument);
			}
		}
	}

	private void processStackMapTable(StackMapTableAttribute attribute) {
		for (StackMapFrame stackMapFrame : attribute.entries) {
			switch (stackMapFrame.type()) {
				case StackMapFrame.SAME_LOCALS_1_STACK_ITEM :
					SameLocals1StackItemFrame sameLocals1StackItemFrame = (SameLocals1StackItemFrame) stackMapFrame;
					verification_type_info(sameLocals1StackItemFrame.stack);
					break;
				case StackMapFrame.SAME_LOCALS_1_STACK_ITEM_EXTENDED :
					SameLocals1StackItemFrameExtended sameLocals1StackItemFrameExtended = (SameLocals1StackItemFrameExtended) stackMapFrame;
					verification_type_info(sameLocals1StackItemFrameExtended.stack);
					break;
				case StackMapFrame.APPEND :
					AppendFrame appendFrame = (AppendFrame) stackMapFrame;
					for (VerificationTypeInfo verificationTypeInfo : appendFrame.locals) {
						verification_type_info(verificationTypeInfo);
					}
					break;
				case StackMapFrame.FULL_FRAME :
					FullFrame fullFrame = (FullFrame) stackMapFrame;
					for (VerificationTypeInfo verificationTypeInfo : fullFrame.locals) {
						verification_type_info(verificationTypeInfo);
					}
					for (VerificationTypeInfo verificationTypeInfo : fullFrame.stack) {
						verification_type_info(verificationTypeInfo);
					}
					break;
			}
		}
	}

	private void verification_type_info(VerificationTypeInfo verificationTypeInfo) {
		switch (verificationTypeInfo.tag) {
			case VerificationTypeInfo.ITEM_Object :// Object_variable_info
				ObjectVariableInfo objectVariableInfo = (ObjectVariableInfo) verificationTypeInfo;
				classConstRef(objectVariableInfo.type);
				break;
		}
	}

	/**
	 * Add a new package reference.
	 *
	 * @param packageRef A '.' delimited package name
	 */
	private void referTo(TypeRef typeRef, int modifiers) {
		xref.add(typeRef);
		if (typeRef.isPrimitive()) {
			return;
		}

		PackageRef packageRef = typeRef.getPackageRef();
		if (packageRef.isPrimitivePackage()) {
			return;
		}

		imports.add(packageRef);

		if (api != null && (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers))) {
			api.add(packageRef);
		}

		referred.merge(typeRef, Integer.valueOf(modifiers), (o, n) -> {
			int old_modifiers = o.intValue();
			int new_modifiers = n.intValue();
			if ((old_modifiers == new_modifiers) || (new_modifiers == 0)) {
				return o;
			} else if (old_modifiers == 0) {
				return n;
			} else {
				return Integer.valueOf(old_modifiers | new_modifiers);
			}
		});
	}

	private void referTo(String descriptor, int modifiers) {
		char c = descriptor.charAt(0);
		if (c != '(' && c != 'L' && c != '[' && c != '<' && c != 'T') {
			return;
		}
		Signature sig = (c == '(' || c == '<') ? analyzer.getMethodSignature(descriptor)
			: analyzer.getFieldSignature(descriptor);
		Set<String> binaryRefs = sig.erasedBinaryReferences();
		for (String binary : binaryRefs) {
			TypeRef ref = analyzer.getTypeRef(binary);
			referTo(ref, modifiers);
		}
	}

	/**
	 * This method parses method or field descriptors and calls
	 * {@link #referTo(TypeRef, int)} for any types found therein.
	 *
	 * @param descriptor The to be parsed descriptor
	 * @param modifiers
	 * @see "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.3"
	 */
	@Deprecated
	public void parseDescriptor(String descriptor, int modifiers) {
		if (referred == null) {
			referred = new HashMap<>();
		}
		referTo(descriptor, modifiers);
	}

	public Set<PackageRef> getReferred() {
		return imports;
	}

	public String getAbsolutePath() {
		return path;
	}

	@Deprecated
	public void reset() {}

	private Stream<Clazz> hierarchyStream(Analyzer analyzer) {
		requireNonNull(analyzer);
		Spliterator<Clazz> spliterator = new AbstractSpliterator<Clazz>(Long.MAX_VALUE,
			Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL) {
			private Clazz clazz = Clazz.this;

			@Override
			public boolean tryAdvance(Consumer<? super Clazz> action) {
				requireNonNull(action);
				if (clazz == null) {
					return false;
				}
				action.accept(clazz);
				TypeRef type = clazz.superClass;
				if (type == null) {
					clazz = null;
				} else {
					try {
						clazz = analyzer.findClass(type);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
					if (clazz == null) {
						analyzer.warning("While traversing the type tree for %s cannot find class %s", Clazz.this,
							type);
					}
				}
				return true;
			}
		};
		return StreamSupport.stream(spliterator, false);
	}

	private Stream<TypeRef> typeStream(Analyzer analyzer, Function<? super Clazz, Collection<? extends TypeRef>> func,
		Set<TypeRef> visited) {
		requireNonNull(analyzer);
		requireNonNull(func);
		Spliterator<TypeRef> spliterator = new AbstractSpliterator<TypeRef>(Long.MAX_VALUE,
			Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL) {
			private final Deque<TypeRef>	queue	= new ArrayDeque<>(func.apply(Clazz.this));
			private final Set<TypeRef>		seen	= (visited != null) ? visited : new HashSet<>();

			@Override
			public boolean tryAdvance(Consumer<? super TypeRef> action) {
				requireNonNull(action);
				TypeRef type;
				do {
					type = queue.poll();
					if (type == null) {
						return false;
					}
				} while (seen.contains(type));
				seen.add(type);
				action.accept(type);
				if (visited != null) {
					Clazz clazz;
					try {
						clazz = analyzer.findClass(type);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
					if (clazz == null) {
						analyzer.warning("While traversing the type tree for %s cannot find class %s", Clazz.this,
							type);
					} else {
						queue.addAll(func.apply(clazz));
					}
				}
				return true;
			}
		};
		return StreamSupport.stream(spliterator, false);
	}

	public boolean is(QUERY query, Instruction instr, Analyzer analyzer) throws Exception {
		switch (query) {
			case ANY :
				return true;

			case NAMED :
				return instr.matches(getClassName().getDottedOnly()) ^ instr.isNegated();

			case VERSION : {
				String v = classFile.major_version + "." + classFile.minor_version;
				return instr.matches(v) ^ instr.isNegated();
			}

			case IMPLEMENTS : {
				Set<TypeRef> visited = new HashSet<>();
				return hierarchyStream(analyzer).flatMap(c -> c.typeStream(analyzer, Clazz::interfaces, visited))
					.map(TypeRef::getDottedOnly)
					.anyMatch(instr::matches) ^ instr.isNegated();
			}

			case EXTENDS :
				return hierarchyStream(analyzer).skip(1) // skip this class
					.map(Clazz::getClassName)
					.map(TypeRef::getDottedOnly)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case PUBLIC :
				return isPublic();

			case CONCRETE :
				return !isAbstract();

			case ANNOTATED :
				return typeStream(analyzer, Clazz::annotations, null) //
					.map(TypeRef::getFQN)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case INDIRECTLY_ANNOTATED :
				return typeStream(analyzer, Clazz::annotations, new HashSet<>()) //
					.map(TypeRef::getFQN)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case HIERARCHY_ANNOTATED :
				return hierarchyStream(analyzer) //
					.flatMap(c -> c.typeStream(analyzer, Clazz::annotations, null))
					.map(TypeRef::getFQN)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case HIERARCHY_INDIRECTLY_ANNOTATED : {
				Set<TypeRef> visited = new HashSet<>();
				return hierarchyStream(analyzer) //
					.flatMap(c -> c.typeStream(analyzer, Clazz::annotations, visited))
					.map(TypeRef::getFQN)
					.anyMatch(instr::matches) ^ instr.isNegated();
			}

			case RUNTIMEANNOTATIONS :
				return hasRuntimeAnnotations;

			case CLASSANNOTATIONS :
				return hasClassAnnotations;

			case ABSTRACT :
				return isAbstract();

			case IMPORTS :
				return hierarchyStream(analyzer) //
					.map(Clazz::getReferred)
					.flatMap(Set::stream)
					.distinct()
					.map(PackageRef::getFQN)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case DEFAULT_CONSTRUCTOR :
				return hasPublicNoArgsConstructor();
		}

		return instr == null ? false : instr.isNegated();
	}

	@Override
	public String toString() {
		return (classDef != null) ? classDef.getName() : resource.toString();
	}

	public boolean isPublic() {
		return classDef.isPublic();
	}

	public boolean isProtected() {
		return classDef.isProtected();
	}

	public boolean isEnum() {
		/**
		 * The additional check for superClass name avoids stating that an
		 * anonymous inner class of an enum is an enum class.
		 */
		return classDef.isEnum() && superClass.getBinary()
			.equals("java/lang/Enum");
	}

	public boolean isSynthetic() {
		return classDef.isSynthetic();
	}

	static boolean isSynthetic(int access) {
		return (access & ACC_SYNTHETIC) != 0;
	}

	public boolean isModule() {
		return classDef.isModule();
	}

	static boolean isModule(int access) {
		return (access & ACC_MODULE) != 0;
	}

	static boolean isEnum(int access) {
		return (access & ACC_ENUM) != 0;
	}

	public JAVA getFormat() {
		return JAVA.format(classFile.major_version);

	}

	public static String objectDescriptorToFQN(String string) {
		if ((string.startsWith("L") || string.startsWith("T")) && string.endsWith(";"))
			return string.substring(1, string.length() - 1)
				.replace('/', '.');

		switch (string.charAt(0)) {
			case 'V' :
				return "void";
			case 'B' :
				return "byte";
			case 'C' :
				return "char";
			case 'I' :
				return "int";
			case 'S' :
				return "short";
			case 'D' :
				return "double";
			case 'F' :
				return "float";
			case 'J' :
				return "long";
			case 'Z' :
				return "boolean";
			case '[' : // Array
				return objectDescriptorToFQN(string.substring(1)) + "[]";
		}
		throw new IllegalArgumentException("Invalid type character in descriptor " + string);
	}

	public static String unCamel(String id) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (c == '_' || c == '$' || c == '-' || c == '.') {
				if (out.length() > 0 && !Character.isWhitespace(out.charAt(out.length() - 1)))
					out.append(' ');
				continue;
			}

			int n = i;
			while (n < id.length() && Character.isUpperCase(id.charAt(n))) {
				n++;
			}
			if (n == i)
				out.append(id.charAt(i));
			else {
				boolean tolower = (n - i) == 1;
				if (i > 0 && !Character.isWhitespace(out.charAt(out.length() - 1)))
					out.append(' ');

				for (; i < n;) {
					if (tolower)
						out.append(Character.toLowerCase(id.charAt(i)));
					else
						out.append(id.charAt(i));
					i++;
				}
				i--;
			}
		}
		if (id.startsWith("."))
			out.append(" *");
		out.replace(0, 1, Character.toUpperCase(out.charAt(0)) + "");
		return out.toString();
	}

	public boolean isInterface() {
		return classDef.isInterface();
	}

	public boolean isAbstract() {
		return classDef.isAbstract();
	}

	public boolean hasPublicNoArgsConstructor() {
		return hasDefaultConstructor;
	}

	public int getAccess() {
		return classDef.getAccess();
	}

	@Deprecated
	public void setInnerAccess(int access) {}

	public Stream<Annotation> annotations(String binaryNameFilter) {
		return classDef.annotations(binaryNameFilter);
	}

	public Stream<TypeAnnotation> typeAnnotations(String binaryNameFilter) {
		return classDef.typeAnnotations(binaryNameFilter);
	}

	public TypeRef getClassName() {
		return classDef.getType();
	}

	public boolean isInnerClass() {
		return classDef.isInnerClass();
	}

	@Deprecated
	public MethodDef getMethodDef(int access, String name, String descriptor) {
		return new MethodDef(access, name, descriptor);
	}

	public TypeRef getSuper() {
		return superClass;
	}

	public String getFQN() {
		return classDef.getName();
	}

	public TypeRef[] getInterfaces() {
		return interfaces;
	}

	public List<TypeRef> interfaces() {
		return (interfaces != null) ? Arrays.asList(interfaces) : emptyList();
	}

	public Set<TypeRef> annotations() {
		return (annotations != null) ? annotations : emptySet();
	}

	public boolean isFinal() {
		return classDef.isFinal();
	}

	@Deprecated
	public void setDeprecated(boolean b) {}

	public boolean isDeprecated() {
		return classDef.isDeprecated();
	}

	public boolean isAnnotation() {
		return classDef.isAnnotation();
	}

	static boolean isAnnotation(int access) {
		return (access & ACC_ANNOTATION) != 0;
	}

	public Set<PackageRef> getAPIUses() {
		return (api != null) ? api : emptySet();
	}

	public Clazz.TypeDef getExtends(TypeRef type) {
		return new TypeDef(type, false);
	}

	public Clazz.TypeDef getImplements(TypeRef type) {
		return new TypeDef(type, true);
	}

	private void classConstRef(int index) {
		if (constantPool.tag(index) == CONSTANT_Class) {
			String name = constantPool.className(index);
			classConstRef(name);
		}
	}

	private void classConstRef(String name) {
		if (name != null) {
			TypeRef typeRef = analyzer.getTypeRef(name);
			referTo(typeRef, 0);
		}
	}

	public String getClassSignature() {
		return classDef.getSignature();
	}

	public String getSourceFile() {
		return classDef.getSourceFile();
	}

	public Map<String, Object> getDefaults() throws Exception {
		parseClassFile();
		if (!classDef.isAnnotation()) {
			return emptyMap();
		}
		Map<String, Object> map = methods().filter(m -> m.attribute(AnnotationDefaultAttribute.class)
			.isPresent())
			.collect(toMap(MethodDef::getName, MethodDef::getConstant));
		return map;
	}

	public Resource getResource() {
		return resource;
	}

}
