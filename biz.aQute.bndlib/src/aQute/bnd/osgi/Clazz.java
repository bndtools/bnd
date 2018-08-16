package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.generics.Create;

public class Clazz {
	private final static Logger	logger				= LoggerFactory.getLogger(Clazz.class);

	static Pattern				METHOD_DESCRIPTOR	= Pattern.compile("(.*)\\)(.+)");

	public class ClassConstant {
		final int		cname;
		public boolean	referred;

		public ClassConstant(int class_index) {
			this.cname = class_index;
		}

		public String getName() {
			return (String) pool[cname];
		}

		@Override
		public String toString() {
			return "ClassConstant[" + getName() + "]";
		}
	}

	public static enum JAVA {
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
					profiles = new HashMap<>();
					for (Map.Entry<Object, Object> prop : p.entrySet()) {
						String list = (String) prop.getValue();
						Set<String> set = new HashSet<>();
						Collections.addAll(set, list.split("\\s*,\\s*"));
						profiles.put((String) prop.getKey(), set);
					}
				}
				return profiles;
			}
		}, //
		OpenJDK9(53, "JavaSE-9", "(&(osgi.ee=JavaSE)(version=9))"), //
		OpenJDK10(54, "JavaSE-10", "(&(osgi.ee=JavaSE)(version=10))"), //
		OpenJDK11(55, "JavaSE-11", "(&(osgi.ee=JavaSE)(version=11))"), //
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

	public static enum QUERY {
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
	
	interface ConstantInfo {
		void accept(Clazz c, CONSTANT tag, DataInput in, int poolIndex) throws IOException;
	}

	static enum CONSTANT {
		Zero(CONSTANT::invalid),
		Utf8(Clazz::doUtf8_info),
		Two(CONSTANT::invalid),
		Integer(Clazz::doInteger_info),
		Float(Clazz::doFloat_info),
		Long(Clazz::doLong_info),
		Double(Clazz::doDouble_info),
		Class(Clazz::doClass_info),
		String(Clazz::doString_info),
		Fieldref(Clazz::doFieldref_info),
		Methodref(Clazz::doMethodref_info),
		InterfaceMethodref(Clazz::doInterfaceMethodref_info),
		NameAndType(Clazz::doNameAndType_info),
		Thirteen(CONSTANT::invalid),
		Fourteen(CONSTANT::invalid),
		MethodHandle(Clazz::doMethodHandle_info),
		MethodType(Clazz::doMethodType_info),
		Dynamic(Clazz::doDynamic_info),
		InvokeDynamic(Clazz::doInvokeDynamic_info),
		Module(Clazz::doModule_info),
		Package(Clazz::doPackage_info);

		private final ConstantInfo info;
		private final int			width;

		CONSTANT(ConstantInfo info) {
			this.info = requireNonNull(info);
			// For some insane optimization reason,
			// the Long(5) and Double(6) entries take two slots in the
			// constant pool. See 4.4.5
			int value = ordinal();
			width = ((value == 5) || (value == 6)) ? 2 : 1;
		}

		int parse(Clazz c, DataInput in, int poolIndex) throws IOException {
			info.accept(c, this, in, poolIndex);
			return width;
		}

		private static void invalid(Clazz c, CONSTANT tag, DataInput in, int poolIndex) throws IOException {
			throw new IOException("Invalid constant pool tag " + tag.ordinal());
		}
	}

	public final static EnumSet<QUERY>	HAS_ARGUMENT	= EnumSet.of(QUERY.IMPLEMENTS, QUERY.EXTENDS, QUERY.IMPORTS,
		QUERY.NAMED, QUERY.VERSION, QUERY.ANNOTATED, QUERY.INDIRECTLY_ANNOTATED, QUERY.HIERARCHY_ANNOTATED,
		QUERY.HIERARCHY_INDIRECTLY_ANNOTATED);

	// Declared public; may be accessed from outside its package.
	final static int					ACC_PUBLIC		= 0x0001;
	// Declared final; no subclasses allowed.
	final static int					ACC_FINAL		= 0x0010;
	// Treat superclass methods specially when invoked by the invokespecial
	// instruction.
	final static int					ACC_SUPER		= 0x0020;
	// Is an interface, not a class
	final static int					ACC_INTERFACE	= 0x0200;
	// Declared a thing not in the source code
	final static int					ACC_ABSTRACT	= 0x0400;
	final static int					ACC_SYNTHETIC	= 0x1000;
	final static int					ACC_BRIDGE		= 0x0040;
	final static int					ACC_ANNOTATION	= 0x2000;
	final static int					ACC_ENUM		= 0x4000;
	final static int					ACC_MODULE		= 0x8000;

	static protected class Assoc {
		final CONSTANT	tag;
		final int		a;
		final int		b;

		Assoc(CONSTANT tag, int a, int b) {
			this.tag = tag;
			this.a = a;
			this.b = b;
		}

		@Override
		public String toString() {
			return "Assoc[" + tag + ", " + a + "," + b + "]";
		}
	}

	public abstract class Def {

		final int		access;
		Set<TypeRef>	annotations;

		public Def(int access) {
			this.access = access;
		}

		public int getAccess() {
			return access;
		}

		public boolean isEnum() {
			return (access & ACC_ENUM) != 0;
		}

		public boolean isPublic() {
			return Modifier.isPublic(access);
		}

		public boolean isAbstract() {
			return Modifier.isAbstract(access);
		}

		public boolean isProtected() {
			return Modifier.isProtected(access);
		}

		public boolean isFinal() {
			return Modifier.isFinal(access) || Clazz.this.isFinal();
		}

		public boolean isStatic() {
			return Modifier.isStatic(access);
		}

		public boolean isPrivate() {
			return Modifier.isPrivate(access);
		}

		public boolean isNative() {
			return Modifier.isNative(access);
		}

		public boolean isTransient() {
			return Modifier.isTransient(access);
		}

		public boolean isVolatile() {
			return Modifier.isVolatile(access);
		}

		public boolean isInterface() {
			return Modifier.isInterface(access);
		}

		public boolean isSynthetic() {
			return (access & ACC_SYNTHETIC) != 0;
		}

		void addAnnotation(Annotation a) {
			if (annotations == null)
				annotations = Create.set();
			annotations.add(analyzer.getTypeRef(a.getName()
				.getBinary()));
		}

		public Collection<TypeRef> getAnnotations() {
			return annotations;
		}

		public TypeRef getOwnerType() {
			return className;
		}

		public abstract String getName();

		public abstract TypeRef getType();

		public abstract TypeRef[] getPrototype();

		public Object getClazz() {
			return Clazz.this;
		}
	}

	public class FieldDef extends Def {
		final String		name;
		final Descriptor	descriptor;
		String				signature;
		Object				constant;
		boolean				deprecated;

		public boolean isDeprecated() {
			return deprecated;
		}

		public void setDeprecated(boolean deprecated) {
			this.deprecated = deprecated;
		}

		public FieldDef(int access, String name, String descriptor) {
			super(access);
			this.name = name;
			this.descriptor = analyzer.getDescriptor(descriptor);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public TypeRef getType() {
			return descriptor.getType();
		}

		public TypeRef getContainingClass() {
			return getClassName();
		}

		public Descriptor getDescriptor() {
			return descriptor;
		}

		public void setConstant(Object o) {
			this.constant = o;
		}

		public Object getConstant() {
			return this.constant;
		}

		// TODO change to use proper generics
		public String getGenericReturnType() {
			String use = descriptor.toString();
			if (signature != null)
				use = signature;

			Matcher m = METHOD_DESCRIPTOR.matcher(use);
			if (!m.matches())
				throw new IllegalArgumentException("Not a valid method descriptor: " + use);

			String returnType = m.group(2);
			return objectDescriptorToFQN(returnType);
		}

		@Override
		public TypeRef[] getPrototype() {
			return null;
		}

		public String getSignature() {
			return signature;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public class MethodDef extends FieldDef {
		public MethodDef(int access, String method, String descriptor) {
			super(access, method, descriptor);
		}

		public boolean isConstructor() {
			return name.equals("<init>") || name.equals("<clinit>");
		}

		@Override
		public TypeRef[] getPrototype() {
			return descriptor.getPrototype();
		}

		public boolean isBridge() {
			return (access & ACC_BRIDGE) != 0;
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

	public static final Comparator<Clazz>	NAME_COMPARATOR	= (Clazz a, Clazz b) -> a.className.compareTo(b.className);

	boolean									hasRuntimeAnnotations;
	boolean									hasClassAnnotations;
	boolean									hasDefaultConstructor;

	int										depth			= 0;
	Deque<ClassDataCollector>				cds				= new LinkedList<>();

	TypeRef									className;
	Object[]								pool;
	int[]									intPool;
	Set<PackageRef>							imports			= Create.set();
	String									path;
	int										minor			= 0;
	int										major			= 0;
	int										innerAccess		= -1;
	int										accessx			= 0;
	String									sourceFile;
	Set<TypeRef>							xref;
	Set<TypeRef>							annotations;
	int										forName			= 0;
	int										class$			= 0;
	TypeRef[]								interfaces;
	TypeRef									zuper;
	ClassDataCollector						cd				= null;
	Resource								resource;
	FieldDef								last			= null;
	boolean									deprecated;
	Set<PackageRef>							api;
	final Analyzer							analyzer;
	String									classSignature;

	private Map<String, Object>				defaults;

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

	Set<TypeRef> parseClassFileData(DataInput in, ClassDataCollector cd) throws Exception {
		cds.push(this.cd);
		this.cd = cd;
		try {
			return parseClassFileData(in);
		} finally {
			this.cd = cds.pop();
		}
	}

	Set<TypeRef> parseClassFileData(DataInput in) throws Exception {
		logger.debug("parseClassFile(): path={} resource={}", path, resource);

		++depth;
		xref = new HashSet<>();

		boolean crawl = cd != null; // Crawl the byte code if we have a
		// collector
		int magic = in.readInt();
		if (magic != 0xCAFEBABE)
			throw new IOException("Not a valid class file (no CAFEBABE header)");

		minor = in.readUnsignedShort(); // minor version
		major = in.readUnsignedShort(); // major version
		if (cd != null)
			cd.version(minor, major);
		int count = in.readUnsignedShort();
		pool = new Object[count];
		intPool = new int[count];

		CONSTANT[] tags = CONSTANT.values();
		for (int poolIndex = 1; poolIndex < count;) {
			int tagValue = in.readUnsignedByte();
			if (tagValue >= tags.length) {
				throw new IOException("Unrecognized constant pool tag value " + tagValue);
			}
			CONSTANT tag = tags[tagValue];
			poolIndex += tag.parse(this, in, poolIndex);
		}

		pool(pool, intPool);

		// All name& type and class constant records contain descriptors we must
		// treat as references, though not API
		for (Object o : pool) {
			if (o instanceof Assoc) {
				Assoc assoc = (Assoc) o;
				switch (assoc.tag) {
					case Fieldref :
					case Methodref :
					case InterfaceMethodref :
						classConstRef(assoc.a);
						break;

					case NameAndType :
					case MethodType :
						referTo(assoc.b, 0); // Descriptor
						break;
					default :
						break;
				}
			}
		}

		/*
		 * Parse after the constant pool, code thanks to Hans Christian
		 * Falkenberg
		 */

		accessx = in.readUnsignedShort(); // access
		if (Modifier.isPublic(accessx))
			api = new HashSet<>();

		int this_class = in.readUnsignedShort();
		className = analyzer.getTypeRef((String) pool[intPool[this_class]]);
		if (!isModule()) {
			referTo(className, Modifier.PUBLIC);
		}

		try {

			if (cd != null) {
				if (!cd.classStart(this))
					return null;
			}

			int super_class = in.readUnsignedShort();
			String superName = (String) pool[intPool[super_class]];
			if (superName != null) {
				zuper = analyzer.getTypeRef(superName);
			}

			if (zuper != null) {
				referTo(zuper, accessx);
				if (cd != null)
					cd.extendsClass(zuper);
			}

			int interfacesCount = in.readUnsignedShort();
			if (interfacesCount > 0) {
				interfaces = new TypeRef[interfacesCount];
				for (int i = 0; i < interfacesCount; i++) {
					interfaces[i] = analyzer.getTypeRef((String) pool[intPool[in.readUnsignedShort()]]);
					referTo(interfaces[i], accessx);
				}
				if (cd != null)
					cd.implementsInterfaces(interfaces);
			}

			int fieldsCount = in.readUnsignedShort();
			for (int i = 0; i < fieldsCount; i++) {
				int access_flags = in.readUnsignedShort(); // skip access flags
				int name_index = in.readUnsignedShort();
				int descriptor_index = in.readUnsignedShort();

				// Java prior to 1.5 used a weird
				// static variable to hold the com.X.class
				// result construct. If it did not find it
				// it would create a variable class$com$X
				// that would be used to hold the class
				// object gotten with Class.forName ...
				// Stupidly, they did not actively use the
				// class name for the field type, so bnd
				// would not see a reference. We detect
				// this case and add an artificial descriptor
				String name = pool[name_index].toString(); // name_index
				if (name.startsWith("class$") || name.startsWith("$class$")) {
					crawl = true;
				}
				if (cd != null) {
					FieldDef fdef = new FieldDef(access_flags, name, pool[descriptor_index].toString());
					last = fdef;
					cd.field(fdef);
				}

				referTo(descriptor_index, access_flags);
				doAttributes(in, ElementType.FIELD, false, access_flags);
			}

			//
			// Check if we have to crawl the code to find
			// the ldc(_w) <string constant> invokestatic Class.forName
			// if so, calculate the method ref index so we
			// can do this efficiently
			//
			if (crawl) {
				forName = findMethodReference("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
				class$ = findMethodReference(className.getBinary(), "class$", "(Ljava/lang/String;)Ljava/lang/Class;");
			} else if (major == JAVA.JDK1_4.major) {
				forName = findMethodReference("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
				if (forName > 0) {
					crawl = true;
					class$ = findMethodReference(className.getBinary(), "class$",
						"(Ljava/lang/String;)Ljava/lang/Class;");
				}
			}

			// There are some serious changes in the
			// class file format. So we do not do any crawling
			// it has also become less important
			// however, jDK8 has a bug that leaves an orphan ClassConstnat
			// so if we have those, we need to also crawl the byte codes.
			if (!crawl) {
				// This loop is overeager since we have not processed exceptions
				// and bootstrap method arguments, so we may crawl when we do
				// not need to.
				for (Object o : pool) {
					if (o instanceof ClassConstant) {
						ClassConstant cc = (ClassConstant) o;
						if (cc.referred == false) {
							crawl = true;
							break;
						}
					}
				}
			}

			//
			// Handle the methods
			//
			int methodCount = in.readUnsignedShort();
			for (int i = 0; i < methodCount; i++) {
				int access_flags = in.readUnsignedShort();
				int name_index = in.readUnsignedShort();
				int descriptor_index = in.readUnsignedShort();
				String name = pool[name_index].toString();
				String descriptor = pool[descriptor_index].toString();
				if (cd != null) {
					MethodDef mdef = new MethodDef(access_flags, name, descriptor);
					last = mdef;
					cd.method(mdef);
				}
				referTo(descriptor_index, access_flags);

				if ("<init>".equals(name)) {
					if (Modifier.isPublic(access_flags) && "()V".equals(descriptor)) {
						hasDefaultConstructor = true;
					}
					doAttributes(in, ElementType.CONSTRUCTOR, crawl, access_flags);
				} else {
					doAttributes(in, ElementType.METHOD, crawl, access_flags);
				}
			}
			if (cd != null)
				cd.memberEnd();
			last = null;

			doAttributes(in, ElementType.TYPE, false, accessx);

			//
			// Parse all the descriptors we found
			//

			Set<TypeRef> xref = this.xref;
			reset();
			return xref;
		} finally {
			if (cd != null)
				cd.classEnd();
		}
	}

	private void pool(@SuppressWarnings("unused") Object[] pool, @SuppressWarnings("unused") int[] intPool) {}

	void doUtf8_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		String name = in.readUTF();
		pool[poolIndex] = name;
	}

	void doInteger_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		intPool[poolIndex] = in.readInt();
		if (cd != null)
			pool[poolIndex] = intPool[poolIndex];
	}

	void doFloat_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		if (cd != null)
			pool[poolIndex] = in.readFloat(); // ALU
		else
			in.skipBytes(4);
	}

	void doLong_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		if (cd != null) {
			pool[poolIndex] = in.readLong();
		} else
			in.skipBytes(8);
	}

	void doDouble_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		if (cd != null)
			pool[poolIndex] = in.readDouble();
		else
			in.skipBytes(8);
	}

	void doClass_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		intPool[poolIndex] = class_index;
		ClassConstant c = new ClassConstant(class_index);
		pool[poolIndex] = c;
	}

	void doString_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int string_index = in.readUnsignedShort();
		intPool[poolIndex] = string_index;
	}

	void doFieldref_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, class_index, name_and_type_index);
	}

	void doMethodref_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, class_index, name_and_type_index);
	}

	void doInterfaceMethodref_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, class_index, name_and_type_index);
	}

	void doNameAndType_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, name_index, descriptor_index);
	}

	void doMethodHandle_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int reference_kind = in.readUnsignedByte();
		int reference_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, reference_kind, reference_index);
	}

	void doMethodType_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int descriptor_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, 0, descriptor_index);
	}

	void doDynamic_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int bootstrap_method_attr_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, bootstrap_method_attr_index, name_and_type_index);
	}

	void doInvokeDynamic_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		int bootstrap_method_attr_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc(tag, bootstrap_method_attr_index, name_and_type_index);
	}

	void doModule_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		in.skipBytes(2);
	}

	void doPackage_info(CONSTANT tag, DataInput in, int poolIndex) throws IOException {
		in.skipBytes(2);
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
		for (int i = 1; i < pool.length; i++) {
			if (pool[i] instanceof Assoc) {
				Assoc methodref = (Assoc) pool[i];
				switch (methodref.tag) {
					case Methodref :
					case InterfaceMethodref : {
						// Method ref
						int class_index = methodref.a;
						int class_name_index = intPool[class_index];
						if (clazz.equals(pool[class_name_index])) {
							int name_and_type_index = methodref.b;
							Assoc name_and_type = (Assoc) pool[name_and_type_index];
							if (name_and_type.tag == CONSTANT.NameAndType) {
								// Name and Type
								int name_index = name_and_type.a;
								int type_index = name_and_type.b;
								if (methodname.equals(pool[name_index])) {
									if (descriptor.equals(pool[type_index])) {
										return i;
									}
								}
							}
						}
						break;
					}
					default :
						break;
				}
			}
		}
		return -1;
	}

	/**
	 * Called for each attribute in the class, field, or method.
	 * 
	 * @param in The stream
	 * @param access_flags
	 * @throws Exception
	 */
	private void doAttributes(DataInput in, ElementType member, boolean crawl, int access_flags) throws Exception {
		int attributesCount = in.readUnsignedShort();
		for (int j = 0; j < attributesCount; j++) {
			doAttribute(in, member, crawl, access_flags);
		}
	}

	/**
	 * Process a single attribute, if not recognized, skip it.
	 * 
	 * @param in the data stream
	 * @param access_flags
	 * @throws Exception
	 */
	private void doAttribute(DataInput in, ElementType member, boolean crawl, int access_flags) throws Exception {
		final int attribute_name_index = in.readUnsignedShort();
		final String attributeName = (String) pool[attribute_name_index];
		final int attribute_length = in.readInt();
		switch (attributeName) {
			case "Deprecated" :
				if (cd != null)
					cd.deprecated();
				break;
			case "RuntimeVisibleAnnotations" :
				doAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);
				break;
			case "RuntimeInvisibleAnnotations" :
				doAnnotations(in, member, RetentionPolicy.CLASS, access_flags);
				break;
			case "RuntimeVisibleParameterAnnotations" :
				doParameterAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);
				break;
			case "RuntimeInvisibleParameterAnnotations" :
				doParameterAnnotations(in, member, RetentionPolicy.CLASS, access_flags);
				break;
			case "RuntimeVisibleTypeAnnotations" :
				doTypeAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);
				break;
			case "RuntimeInvisibleTypeAnnotations" :
				doTypeAnnotations(in, member, RetentionPolicy.CLASS, access_flags);
				break;
			case "InnerClasses" :
				doInnerClasses(in);
				break;
			case "EnclosingMethod" :
				doEnclosingMethod(in);
				break;
			case "SourceFile" :
				doSourceFile(in);
				break;
			case "Code" :
				doCode(in, crawl);
				break;
			case "Signature" :
				doSignature(in, member, access_flags);
				break;
			case "ConstantValue" :
				doConstantValue(in);
				break;
			case "AnnotationDefault" :
				Object value = doElementValue(in, member, RetentionPolicy.RUNTIME, cd != null, access_flags);
				if (last instanceof MethodDef) {
					((MethodDef) last).constant = value;
					cd.annotationDefault((MethodDef) last, value);
				}
				break;
			case "Exceptions" :
				doExceptions(in, access_flags);
				break;
			case "BootstrapMethods" :
				doBootstrapMethods(in);
				break;
			case "StackMapTable" :
				doStackMapTable(in);
				break;
			case "NestHost" :
				doNestHost(in);
				break;
			case "NestMembers" :
				doNestMembers(in);
				break;
			default :
				if (attribute_length < 0) {
					throw new IllegalArgumentException("Attribute > 2Gb");
				}
				in.skipBytes(attribute_length);
				break;
		}
	}

	/**
	 * <pre>
	 *  EnclosingMethod_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 class_index u2 method_index; }
	 * </pre>
	 * 
	 * @param in
	 * @throws IOException
	 */
	private void doEnclosingMethod(DataInput in) throws IOException {
		int cIndex = in.readUnsignedShort();
		int mIndex = in.readUnsignedShort();
		classConstRef(cIndex);

		if (cd != null) {
			int nameIndex = intPool[cIndex];
			TypeRef cName = analyzer.getTypeRef((String) pool[nameIndex]);

			String mName = null;
			String mDescriptor = null;

			if (mIndex != 0) {
				Assoc nameAndType = (Assoc) pool[mIndex];
				mName = (String) pool[nameAndType.a];
				mDescriptor = (String) pool[nameAndType.b];
			}
			cd.enclosingMethod(cName, mName, mDescriptor);
		}
	}

	/**
	 * <pre>
	 *  InnerClasses_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 number_of_classes; { u2 inner_class_info_index; u2
	 * outer_class_info_index; u2 inner_name_index; u2 inner_class_access_flags;
	 * } classes[number_of_classes]; }
	 * </pre>
	 * 
	 * @param in
	 * @throws Exception
	 */
	private void doInnerClasses(DataInput in) throws Exception {
		int number_of_classes = in.readUnsignedShort();
		for (int i = 0; i < number_of_classes; i++) {
			int inner_class_info_index = in.readUnsignedShort();
			int outer_class_info_index = in.readUnsignedShort();
			int inner_name_index = in.readUnsignedShort();
			int inner_class_access_flags = in.readUnsignedShort();

			if (cd != null) {
				TypeRef innerClass = null;
				TypeRef outerClass = null;
				String innerName = null;

				if (inner_class_info_index != 0) {
					int nameIndex = intPool[inner_class_info_index];
					innerClass = analyzer.getTypeRef((String) pool[nameIndex]);
				}

				if (outer_class_info_index != 0) {
					int nameIndex = intPool[outer_class_info_index];
					outerClass = analyzer.getTypeRef((String) pool[nameIndex]);
				}

				if (inner_name_index != 0)
					innerName = (String) pool[inner_name_index];

				cd.innerClass(innerClass, outerClass, innerName, inner_class_access_flags);
			}
		}
	}

	/**
	 * Handle a signature
	 * 
	 * <pre>
	 *  Signature_attribute { u2 attribute_name_index;
	 * u4 attribute_length; u2 signature_index; }
	 * </pre>
	 * 
	 * @param member
	 * @param access_flags
	 */

	void doSignature(DataInput in, ElementType member, int access_flags) throws IOException {
		int signature_index = in.readUnsignedShort();
		String signature = (String) pool[signature_index];
		try {

			parseDescriptor(signature, access_flags);
			if (last != null)
				last.signature = signature;

			if (cd != null)
				cd.signature(signature);

			if (member == ElementType.TYPE)
				classSignature = signature;

		} catch (Exception e) {
			throw new RuntimeException("Signature failed for " + signature, e);
		}
	}

	/**
	 * Handle a constant value call the data collector with it
	 */
	void doConstantValue(DataInput in) throws IOException {
		int constantValue_index = in.readUnsignedShort();
		if (cd == null)
			return;

		Object object = pool[constantValue_index];
		if (object == null)
			object = pool[intPool[constantValue_index]];

		last.constant = object;
		cd.constant(object);
	}

	void doExceptions(DataInput in, int access_flags) throws IOException {
		int exception_count = in.readUnsignedShort();
		for (int i = 0; i < exception_count; i++) {
			int index = in.readUnsignedShort();
			ClassConstant cc = (ClassConstant) pool[index];
			TypeRef clazz = analyzer.getTypeRef(cc.getName());
			referTo(clazz, access_flags);
		}
	}

	/**
	 * <pre>
	 *  Code_attribute { u2 attribute_name_index; u4 attribute_length; u2
	 * max_stack; u2 max_locals; u4 code_length; u1 code[code_length]; u2
	 * exception_table_length; { u2 start_pc; u2 end_pc; u2 handler_pc; u2
	 * catch_type; } exception_table[exception_table_length]; u2
	 * attributes_count; attribute_info attributes[attributes_count]; }
	 * </pre>
	 * 
	 * @param in
	 * @param pool
	 * @throws Exception
	 */
	private void doCode(DataInput in, boolean crawl) throws Exception {
		/* int max_stack = */in.readUnsignedShort();
		/* int max_locals = */in.readUnsignedShort();
		int code_length = in.readInt();
		if (crawl) {
			ByteBuffer code;
			if (in instanceof ByteBufferDataInput) {
				ByteBufferDataInput bbin = (ByteBufferDataInput) in;
				code = bbin.slice(code_length);
			} else {
				byte array[] = new byte[code_length];
				in.readFully(array, 0, code_length);
				code = ByteBuffer.wrap(array, 0, code_length);
			}
			crawl(code);
		} else {
			in.skipBytes(code_length);
		}
		int exception_table_length = in.readUnsignedShort();
		for (int i = 0; i < exception_table_length; i++) {
			int start_pc = in.readUnsignedShort();
			int end_pc = in.readUnsignedShort();
			int handler_pc = in.readUnsignedShort();
			int catch_type = in.readUnsignedShort();
			classConstRef(catch_type);
		}
		doAttributes(in, ElementType.METHOD, false, 0);
	}

	/**
	 * We must find Class.forName references ...
	 * 
	 * @param code
	 */
	private void crawl(ByteBuffer bb) {
		int lastReference = -1;

		while (bb.hasRemaining()) {
			int instruction = Byte.toUnsignedInt(bb.get());
			switch (instruction) {
				case OpCodes.ldc : {
					lastReference = Byte.toUnsignedInt(bb.get());
					classConstRef(lastReference);
					break;
				}
				case OpCodes.ldc_w : {
					lastReference = Short.toUnsignedInt(bb.getShort());
					classConstRef(lastReference);
					break;
				}
				case OpCodes.anewarray :
				case OpCodes.checkcast :
				case OpCodes.instanceof_ :
				case OpCodes.new_ : {
					int cref = Short.toUnsignedInt(bb.getShort());
					classConstRef(cref);
					lastReference = -1;
					break;
				}

				case OpCodes.multianewarray : {
					int cref = Short.toUnsignedInt(bb.getShort());
					classConstRef(cref);
					bb.get();
					lastReference = -1;
					break;
				}

				case OpCodes.invokespecial : {
					int mref = Short.toUnsignedInt(bb.getShort());
					if (cd != null)
						referenceMethod(0, mref);
					break;
				}

				case OpCodes.invokevirtual : {
					int mref = Short.toUnsignedInt(bb.getShort());
					if (cd != null)
						referenceMethod(0, mref);
					break;
				}

				case OpCodes.invokeinterface : {
					int mref = Short.toUnsignedInt(bb.getShort());
					if (cd != null)
						referenceMethod(0, mref);
					bb.get(); // read past the 'count' operand
					bb.get(); // read past the reserved space for future operand
					break;
				}

				case OpCodes.invokestatic : {
					int methodref = Short.toUnsignedInt(bb.getShort());
					if (cd != null)
						referenceMethod(0, methodref);

					if ((methodref == forName || methodref == class$) && lastReference != -1
						&& pool[intPool[lastReference]] instanceof String) {
						String fqn = (String) pool[intPool[lastReference]];
						if (!fqn.equals("class") && fqn.indexOf('.') > 0) {
							TypeRef clazz = analyzer.getTypeRefFromFQN(fqn);
							referTo(clazz, 0);
						}
						lastReference = -1;
					}
					break;
				}

				/*
				 * 3/5: opcode, indexbyte1, indexbyte2 or iinc, indexbyte1,
				 * indexbyte2, countbyte1, countbyte2
				 */
				case OpCodes.wide : {
					int opcode = Byte.toUnsignedInt(bb.get());
					bb.position(bb.position() + (opcode == OpCodes.iinc ? 4 : 2));
					break;
				}
				case OpCodes.tableswitch : {
					// Skip to place divisible by 4
					int rem = bb.position() % 4;
					if (rem != 0) {
						bb.position(bb.position() + 4 - rem);
					}
					int deflt = bb.getInt();
					int low = bb.getInt();
					int high = bb.getInt();
					bb.position(bb.position() + (high - low + 1) * 4);
					lastReference = -1;
					break;
				}
				case OpCodes.lookupswitch : {
					// Skip to place divisible by 4
					int rem = bb.position() % 4;
					if (rem != 0) {
						bb.position(bb.position() + 4 - rem);
					}
					int deflt = bb.getInt();
					int npairs = bb.getInt();
					bb.position(bb.position() + npairs * 8);
					lastReference = -1;
					break;
				}
				default : {
					lastReference = -1;
					bb.position(bb.position() + OpCodes.OFFSETS[instruction]);
				}
			}
		}
	}

	private void doSourceFile(DataInput in) throws IOException {
		int sourcefile_index = in.readUnsignedShort();
		this.sourceFile = pool[sourcefile_index].toString();
	}

	private void doParameterAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
		throws Exception {
		int num_parameters = in.readUnsignedByte();
		for (int p = 0; p < num_parameters; p++) {
			if (cd != null)
				cd.parameter(p);
			doAnnotations(in, member, policy, access_flags);
		}
	}

	private void doTypeAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
		throws Exception {
		int num_annotations = in.readUnsignedShort();
		for (int p = 0; p < num_annotations; p++) {

			// type_annotation {
			// u1 target_type;
			// union {
			// type_parameter_target;
			// supertype_target;
			// type_parameter_bound_target;
			// empty_target;
			// method_formal_parameter_target;
			// throws_target;
			// localvar_target;
			// catch_target;
			// offset_target;
			// type_argument_target;
			// } target_info;
			// type_path target_path;
			// u2 type_index;
			// u2 num_element_value_pairs;
			// { u2 element_name_index;
			// element_value value;
			// } element_value_pairs[num_element_value_pairs];
			// }

			// Table 4.7.20-A. Interpretation of target_type values (Part 1)

			int target_type = in.readUnsignedByte();
			switch (target_type) {
				case 0x00 : // type parameter declaration of generic class or
							// interface
				case 0x01 : // type parameter declaration of generic method or
							// constructor
					//
					// type_parameter_target {
					// u1 type_parameter_index;
					// }
					in.skipBytes(1);
					break;

				case 0x10 : // type in extends clause of class or interface
							// declaration (including the direct superclass of
							// an anonymous class declaration), or in implements
							// clause of interface declaration
					// supertype_target {
					// u2 supertype_index;
					// }

					in.skipBytes(2);
					break;

				case 0x11 : // type in bound of type parameter declaration of
							// generic class or interface
				case 0x12 : // type in bound of type parameter declaration of
							// generic method or constructor
					// type_parameter_bound_target {
					// u1 type_parameter_index;
					// u1 bound_index;
					// }
					in.skipBytes(2);
					break;

				case 0x13 : // type in field declaration
				case 0x14 : // return type of method, or type of newly
							// constructed object
				case 0x15 : // receiver type of method or constructor
					break;

				case 0x16 : // type in formal parameter declaration of method,
							// constructor, or lambda expression
					// formal_parameter_target {
					// u1 formal_parameter_index;
					// }
					in.skipBytes(1);
					break;

				case 0x17 : // type in throws clause of method or constructor
					// throws_target {
					// u2 throws_type_index;
					// }
					in.skipBytes(2);
					break;

				case 0x40 : // type in local variable declaration
				case 0x41 : // type in resource variable declaration
					// localvar_target {
					// u2 table_length;
					// { u2 start_pc;
					// u2 length;
					// u2 index;
					// } table[table_length];
					// }
					int table_length = in.readUnsignedShort();
					in.skipBytes(table_length * 6);
					break;

				case 0x42 : // type in exception parameter declaration
					// catch_target {
					// u2 exception_table_index;
					// }
					in.skipBytes(2);
					break;

				case 0x43 : // type in instanceof expression
				case 0x44 : // type in new expression
				case 0x45 : // type in method reference expression using ::new
				case 0x46 : // type in method reference expression using
							// ::Identifier
					// offset_target {
					// u2 offset;
					// }
					in.skipBytes(2);
					break;

				case 0x47 : // type in cast expression
				case 0x48 : // type argument for generic constructor in new
							// expression or explicit constructor invocation
							// statement

				case 0x49 : // type argument for generic method in method
							// invocation expression
				case 0x4A : // type argument for generic constructor in method
							// reference expression using ::new
				case 0x4B : // type argument for generic method in method
							// reference expression using ::Identifier
					// type_argument_target {
					// u2 offset;
					// u1 type_argument_index;
					// }
					in.skipBytes(3);
					break;

			}

			// The value of the target_path item denotes precisely which part of
			// the type indicated by target_info is annotated. The format of the
			// type_path structure is specified in §4.7.20.2.
			//
			// type_path {
			// u1 path_length;
			// { u1 type_path_kind;
			// u1 type_argument_index;
			// } path[path_length];
			// }

			int path_length = in.readUnsignedByte();
			in.skipBytes(path_length * 2);

			//
			// Rest is identical to the normal annotations
			doAnnotation(in, member, policy, false, access_flags);
		}
	}

	private void doAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
		throws Exception {
		int num_annotations = in.readUnsignedShort(); // # of annotations
		for (int a = 0; a < num_annotations; a++) {
			if (cd == null)
				doAnnotation(in, member, policy, false, access_flags);
			else {
				Annotation annotion = doAnnotation(in, member, policy, true, access_flags);
				cd.annotation(annotion);
			}
		}
	}

	// annotation {
	// u2 type_index;
	// u2 num_element_value_pairs; {
	// u2 element_name_index;
	// element_value value;
	// }
	// element_value_pairs[num_element_value_pairs];
	// }

	private Annotation doAnnotation(DataInput in, ElementType member, RetentionPolicy policy, boolean collect,
		int access_flags) throws IOException {
		int type_index = in.readUnsignedShort();
		if (annotations == null)
			annotations = new HashSet<>();

		String typeName = (String) pool[type_index];
		TypeRef typeRef = null;
		if (typeName != null) {
			typeRef = analyzer.getTypeRef(typeName);
			annotations.add(typeRef);

			if (policy == RetentionPolicy.RUNTIME) {
				referTo(type_index, 0);
				hasRuntimeAnnotations = true;
				if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags)))
					api.add(typeRef.getPackageRef());
			} else {
				hasClassAnnotations = true;
			}
		}
		int num_element_value_pairs = in.readUnsignedShort();
		Map<String, Object> elements = null;
		for (int v = 0; v < num_element_value_pairs; v++) {
			int element_name_index = in.readUnsignedShort();
			String element = (String) pool[element_name_index];
			Object value = doElementValue(in, member, policy, collect, access_flags);
			if (collect) {
				if (elements == null)
					elements = new LinkedHashMap<>();
				elements.put(element, value);
			}
		}
		if (collect)
			return new Annotation(typeRef, elements, member, policy);
		return null;
	}

	private Object doElementValue(DataInput in, ElementType member, RetentionPolicy policy, boolean collect,
		int access_flags) throws IOException {
		char tag = (char) in.readUnsignedByte();
		switch (tag) {
			case 'B' : // Byte
			case 'C' : // Character
			case 'I' : // Integer
			case 'S' : // Short
				int const_value_index = in.readUnsignedShort();
				return intPool[const_value_index];

			case 'D' : // Double
			case 'F' : // Float
			case 's' : // String
			case 'J' : // Long
				const_value_index = in.readUnsignedShort();
				return pool[const_value_index];

			case 'Z' : // Boolean
				const_value_index = in.readUnsignedShort();
				return pool[const_value_index] == null || pool[const_value_index].equals(0) ? false : true;

			case 'e' : // enum constant
				int type_name_index = in.readUnsignedShort();
				if (policy == RetentionPolicy.RUNTIME) {
					referTo(type_name_index, 0);
					if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags))) {
						TypeRef name = analyzer.getTypeRef((String) pool[type_name_index]);
						api.add(name.getPackageRef());
					}
				}
				int const_name_index = in.readUnsignedShort();
				return pool[const_name_index];

			case 'c' : // Class
				int class_info_index = in.readUnsignedShort();
				TypeRef name = analyzer.getTypeRef((String) pool[class_info_index]);
				if (policy == RetentionPolicy.RUNTIME) {
					referTo(class_info_index, 0);
					if (api != null && (Modifier.isPublic(access_flags) || Modifier.isProtected(access_flags))) {
						api.add(name.getPackageRef());
					}
				}
				return name;

			case '@' : // Annotation type
				return doAnnotation(in, member, policy, collect, access_flags);

			case '[' : // Array
				int num_values = in.readUnsignedShort();
				Object[] result = new Object[num_values];
				for (int i = 0; i < num_values; i++) {
					result[i] = doElementValue(in, member, policy, collect, access_flags);
				}
				return result;

			default :
				throw new IllegalArgumentException("Invalid value for Annotation ElementValue tag " + tag);
		}
	}

	/*
	 * Bootstrap method arguments can be class constants.
	 */
	private void doBootstrapMethods(DataInput in) throws IOException {
		final int num_bootstrap_methods = in.readUnsignedShort();
		for (int v = 0; v < num_bootstrap_methods; v++) {
			final int bootstrap_method_ref = in.readUnsignedShort();
			final int num_bootstrap_arguments = in.readUnsignedShort();
			for (int a = 0; a < num_bootstrap_arguments; a++) {
				final int bootstrap_argument = in.readUnsignedShort();
				classConstRef(bootstrap_argument);
			}
		}
	}

	/*
	 * The verifier can require access to types only referenced in StackMapTable
	 * attributes.
	 */
	private void doStackMapTable(DataInput in) throws IOException {
		final int number_of_entries = in.readUnsignedShort();
		for (int v = 0; v < number_of_entries; v++) {
			final int frame_type = in.readUnsignedByte();
			if (frame_type <= 63) { // same_frame
				// nothing else to do
			} else if (frame_type <= 127) { // same_locals_1_stack_item_frame
				verification_type_info(in);
			} else if (frame_type <= 246) { // RESERVED
				// nothing else to do
			} else if (frame_type <= 247) { // same_locals_1_stack_item_frame_extended
				final int offset_delta = in.readUnsignedShort();
				verification_type_info(in);
			} else if (frame_type <= 250) { // chop_frame
				final int offset_delta = in.readUnsignedShort();
			} else if (frame_type <= 251) { // same_frame_extended
				final int offset_delta = in.readUnsignedShort();
			} else if (frame_type <= 254) { // append_frame
				final int offset_delta = in.readUnsignedShort();
				final int number_of_locals = frame_type - 251;
				for (int n = 0; n < number_of_locals; n++) {
					verification_type_info(in);
				}
			} else if (frame_type <= 255) { // full_frame
				final int offset_delta = in.readUnsignedShort();
				final int number_of_locals = in.readUnsignedShort();
				for (int n = 0; n < number_of_locals; n++) {
					verification_type_info(in);
				}
				final int number_of_stack_items = in.readUnsignedShort();
				for (int n = 0; n < number_of_stack_items; n++) {
					verification_type_info(in);
				}
			}
		}
	}

	private void verification_type_info(DataInput in) throws IOException {
		final int tag = in.readUnsignedByte();
		switch (tag) {
			case 7 :// Object_variable_info
				final int cpool_index = in.readUnsignedShort();
				classConstRef(cpool_index);
				break;
			case 8 :// ITEM_Uninitialized
				final int offset = in.readUnsignedShort();
				break;
		}
	}

	/*
	 * Nest class references are only used during access checks. So we do not
	 * need to record them as class references here.
	 */
	private void doNestHost(DataInput in) throws IOException {
		final int host_class_index = in.readUnsignedShort();
	}

	/*
	 * Nest class references are only used during access checks. So we do not
	 * need to record them as class references here.
	 */
	private void doNestMembers(DataInput in) throws IOException {
		final int number_of_classes = in.readUnsignedShort();
		for (int v = 0; v < number_of_classes; v++) {
			final int member_class_index = in.readUnsignedShort();
		}
	}

	/**
	 * Add a new package reference.
	 * 
	 * @param packageRef A '.' delimited package name
	 */
	void referTo(TypeRef typeRef, int modifiers) {
		if (xref != null)
			xref.add(typeRef);
		if (typeRef.isPrimitive())
			return;

		PackageRef packageRef = typeRef.getPackageRef();
		if (packageRef.isPrimitivePackage())
			return;

		imports.add(packageRef);

		if (api != null && (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)))
			api.add(packageRef);

		if (cd != null)
			cd.referTo(typeRef, modifiers);

	}

	void referTo(int index, int modifiers) {
		String descriptor = (String) pool[index];
		parseDescriptor(descriptor, modifiers);
	}

	/**
	 * This method parses a descriptor and adds the package of the descriptor to
	 * the referenced packages. The syntax of the descriptor is:
	 * 
	 * <pre>
	 * descriptor ::= ( '(' reference * ')' )? reference reference ::= 'L'
	 * classname ( '&lt;' references '&gt;' )? ';' | 'B' | 'Z' | ... | '+' | '-'
	 * | '['
	 * </pre>
	 * 
	 * This methods uses heavy recursion to parse the descriptor and a roving
	 * pointer to limit the creation of string objects.
	 * 
	 * @param descriptor The to be parsed descriptor
	 * @param modifiers
	 */

	public void parseDescriptor(String descriptor, int modifiers) {
		// Some descriptors are weird, they start with a generic
		// declaration that contains ':', not sure what they mean ...
		int rover = 0;
		if (descriptor.charAt(0) == '<') {
			rover = parseFormalTypeParameters(descriptor, rover, modifiers);
		}

		if (descriptor.charAt(rover) == '(') {
			rover = parseReferences(descriptor, rover + 1, ')', modifiers);
			rover++;
		}
		parseReferences(descriptor, rover, (char) 0, modifiers);
	}

	/**
	 * Parse a sequence of references. A sequence ends with a given character or
	 * when the string ends.
	 * 
	 * @param descriptor The whole descriptor.
	 * @param rover The index in the descriptor
	 * @param delimiter The end character or 0
	 * @return the last index processed, one character after the delimeter
	 */
	int parseReferences(String descriptor, int rover, char delimiter, int modifiers) {
		int r = rover;
		while (r < descriptor.length() && descriptor.charAt(r) != delimiter) {
			r = parseReference(descriptor, r, modifiers);
		}
		return r;
	}

	/**
	 * Parse a single reference. This can be a single character or an object
	 * reference when it starts with 'L'.
	 * 
	 * @param descriptor The descriptor
	 * @param rover The place to start
	 * @return The return index after the reference
	 */
	int parseReference(String descriptor, int rover, int modifiers) {
		int r = rover;
		char c = descriptor.charAt(r);
		while (c == '[')
			c = descriptor.charAt(++r);

		if (c == '<') {
			r = parseReferences(descriptor, r + 1, '>', modifiers);
		} else if (c == 'T') {
			// Type variable name
			r++;
			while (descriptor.charAt(r) != ';')
				r++;
		} else if (c == 'L') {
			StringBuilder sb = new StringBuilder();
			r++;
			while ((c = descriptor.charAt(r)) != ';') {
				if (c == '<') {
					r = parseReferences(descriptor, r + 1, '>', modifiers);
				} else
					sb.append(c);
				r++;
			}
			TypeRef ref = analyzer.getTypeRef(sb.toString());
			if (cd != null)
				cd.addReference(ref);

			referTo(ref, modifiers);
		} else {
			if ("+-*BCDFIJSZV".indexOf(c) < 0)
				;// System.err.println("Should not skip: " + c);
		}

		// this skips a lot of characters
		// [, *, +, -, B, etc.

		return r + 1;
	}

	/**
	 * FormalTypeParameters
	 * 
	 * @param descriptor
	 * @param index
	 */
	private int parseFormalTypeParameters(String descriptor, int index, int modifiers) {
		index++;
		while (descriptor.charAt(index) != '>') {
			// Skip IDENTIFIER
			index = descriptor.indexOf(':', index) + 1;
			if (index == 0)
				throw new IllegalArgumentException("Expected ClassBound or InterfaceBounds: " + descriptor);

			// ClassBound? InterfaceBounds
			char c = descriptor.charAt(index);

			if (c != ':') {
				// ClassBound?
				index = parseReference(descriptor, index, modifiers);
				c = descriptor.charAt(index);
			}

			// InterfaceBounds*
			while (c == ':') {
				index++;
				index = parseReference(descriptor, index, modifiers);
				c = descriptor.charAt(index);
			} // for each interface

		} // for each formal parameter
		return index + 1; // skip >
	}

	public Set<PackageRef> getReferred() {
		return imports;
	}

	public String getAbsolutePath() {
		return path;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * .class construct for different compilers sun 1.1 Detect static variable
	 * class$com$acme$MyClass 1.2 " 1.3 " 1.4 " 1.5 ldc_w (class) 1.6 " eclipse
	 * 1.1 class$0, ldc (string), invokestatic Class.forName 1.2 " 1.3 " 1.5 ldc
	 * (class) 1.6 " 1.5 and later is not an issue, sun pre 1.5 is easy to
	 * detect the static variable that decodes the class name. For eclipse, the
	 * class$0 gives away we have a reference encoded in a string.
	 * compilerversions/compilerversions.jar contains test versions of all
	 * versions/compilers.
	 */

	public void reset() {
		if (--depth == 0) {
			pool = null;
			intPool = null;
			xref = null;
		}
	}

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
				TypeRef type = clazz.zuper;
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

	private Stream<TypeRef> typeStream(Analyzer analyzer,
		Function<? super Clazz, Collection<? extends TypeRef>> func,
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
				String v = major + "." + minor;
				return instr.matches(v) ^ instr.isNegated();
			}

			case IMPLEMENTS : {
				Set<TypeRef> visited = new HashSet<>();
				return hierarchyStream(analyzer)
					.flatMap(c -> c.typeStream(analyzer, Clazz::interfaces, visited))
					.map(TypeRef::getDottedOnly)
					.anyMatch(instr::matches) ^ instr.isNegated();
			}

			case EXTENDS :
				return hierarchyStream(analyzer).skip(1) // skip this class
					.map(Clazz::getClassName)
					.map(TypeRef::getDottedOnly)
					.anyMatch(instr::matches) ^ instr.isNegated();

			case PUBLIC :
				return Modifier.isPublic(accessx);

			case CONCRETE :
				return !Modifier.isAbstract(accessx);

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
				return Modifier.isAbstract(accessx);

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
		if (className != null) {
			return className.getFQN();
		}
		return resource.toString();
	}

	/**
	 * Called when crawling the byte code and a method reference is found
	 */
	private void referenceMethod(int access, int methodRefPoolIndex) {
		if (methodRefPoolIndex == 0)
			return;

		Object o = pool[methodRefPoolIndex];
		if (o instanceof Assoc) {
			Assoc assoc = (Assoc) o;
			switch (assoc.tag) {
				case Methodref :
				case InterfaceMethodref : {
					int string_index = intPool[assoc.a];
					TypeRef class_name = analyzer.getTypeRef((String) pool[string_index]);
					int name_and_type_index = assoc.b;
					Assoc name_and_type = (Assoc) pool[name_and_type_index];
					if (name_and_type.tag == CONSTANT.NameAndType) {
						// Name and Type
						int name_index = name_and_type.a;
						int type_index = name_and_type.b;
						String method = (String) pool[name_index];
						String descriptor = (String) pool[type_index];
						cd.referenceMethod(access, class_name, method, descriptor);
					} else {
						throw new IllegalArgumentException(
							"Invalid class file (or parsing is wrong), assoc is not type + name (12)");
					}
					break;
				}
				default :
					throw new IllegalArgumentException(
						"Invalid class file (or parsing is wrong), Assoc is not method ref! (10)");
			}
		} else
			throw new IllegalArgumentException(
				"Invalid class file (or parsing is wrong), Not an assoc at a method ref");
	}

	public boolean isPublic() {
		return Modifier.isPublic(accessx);
	}

	public boolean isProtected() {
		return Modifier.isProtected(accessx);
	}

	public boolean isEnum() {
		return zuper != null && zuper.getBinary()
			.equals("java/lang/Enum");
	}

	public boolean isSynthetic() {
		return (ACC_SYNTHETIC & accessx) != 0;
	}

	public boolean isModule() {
		return (ACC_MODULE & accessx) != 0;
	}

	public JAVA getFormat() {
		return JAVA.format(major);

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
		return Modifier.isInterface(accessx);
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(accessx);
	}

	public boolean hasPublicNoArgsConstructor() {
		return hasDefaultConstructor;
	}

	public int getAccess() {
		if (innerAccess == -1)
			return accessx;
		return innerAccess;
	}

	public TypeRef getClassName() {
		return className;
	}

	/**
	 * To provide an enclosing instance
	 * 
	 * @param access
	 * @param name
	 * @param descriptor
	 */
	public MethodDef getMethodDef(int access, String name, String descriptor) {
		return new MethodDef(access, name, descriptor);
	}

	public TypeRef getSuper() {
		return zuper;
	}

	public String getFQN() {
		return className.getFQN();
	}

	public TypeRef[] getInterfaces() {
		return interfaces;
	}

	public List<TypeRef> interfaces() {
		return (interfaces != null) ? Arrays.asList(interfaces) : Collections.emptyList();
	}

	public Set<TypeRef> annotations() {
		return (annotations != null) ? annotations : Collections.emptySet();
	}

	public void setInnerAccess(int access) {
		innerAccess = access;
	}

	public boolean isFinal() {
		return Modifier.isFinal(accessx);
	}

	public void setDeprecated(boolean b) {
		deprecated = b;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public boolean isAnnotation() {
		return (accessx & ACC_ANNOTATION) != 0;
	}

	public Set<PackageRef> getAPIUses() {
		if (api == null)
			return Collections.emptySet();
		return api;
	}

	public Clazz.TypeDef getExtends(TypeRef type) {
		return new TypeDef(type, false);
	}

	public Clazz.TypeDef getImplements(TypeRef type) {
		return new TypeDef(type, true);
	}

	private void classConstRef(int lastReference) {
		Object o = pool[lastReference];
		if (o == null)
			return;

		if (o instanceof ClassConstant) {
			ClassConstant cc = (ClassConstant) o;
			if (cc.referred)
				return;
			cc.referred = true;
			String name = cc.getName();
			if (name != null) {
				TypeRef tr = analyzer.getTypeRef(name);
				referTo(tr, 0);
			}
		}

	}

	public String getClassSignature() {
		return classSignature;
	}

	public Map<String, Object> getDefaults() throws Exception {
		if (defaults == null) {
			Map<String, Object> map = defaults = new HashMap<>();
			parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void annotationDefault(MethodDef last, Object value) {
					map.put(last.name, value);
				}
			});
		}
		return defaults;
	}

}
