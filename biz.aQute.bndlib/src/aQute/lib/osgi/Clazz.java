package aQute.lib.osgi;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.osgi.Descriptors.Descriptor;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.libg.generics.*;

public class Clazz {

	static Pattern	METHOD_DESCRIPTOR	= Pattern.compile("\\((.*)\\)(.+)");

	public class ClassConstant {
		int	cname;

		public ClassConstant(int class_index) {
			this.cname = class_index;
		}

		public String getName() {
			return (String) pool[cname];
		}
	}

	public static enum JAVA {
		JDK1_1(45, "JRE-1.1"), JDK1_2(46, "J2SE-1.2"), //
		JDK1_3(47, "J2SE-1.3"), //
		JDK1_4(48, "J2SE-1.4"), //
		J2SE5(49, "J2SE-1.5"), //
		J2SE6(50, "JavaSE-1.6"), //
		OpenJDK7(51, "JavaSE-1.7"), //
		UNKNOWN(Integer.MAX_VALUE, "<>")//
		;

		final int		major;
		final String	ee;

		JAVA(int major, String ee) {
			this.major = major;
			this.ee = ee;
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
	}

	public static enum QUERY {
		IMPLEMENTS, EXTENDS, IMPORTS, NAMED, ANY, VERSION, CONCRETE, ABSTRACT, PUBLIC, ANNOTATED, RUNTIMEANNOTATIONS, CLASSANNOTATIONS;

	}

	public final static EnumSet<QUERY>	HAS_ARGUMENT	= EnumSet.of(QUERY.IMPLEMENTS, QUERY.EXTENDS, QUERY.IMPORTS,
																QUERY.NAMED, QUERY.VERSION, QUERY.ANNOTATED);

	/**
	 * <pre>
	 * ACC_PUBLIC 0x0001 Declared public; may be accessed from outside its
	 * package. 
	 * ACC_FINAL 0x0010 Declared final; no subclasses allowed.
	 * ACC_SUPER 0x0020 Treat superclass methods specially when invoked by the
	 * invokespecial instruction. 
	 * ACC_INTERFACE 0x0200 Is an interface, not a
	 * class. 
	 * ACC_ABSTRACT 0x0400 Declared abstract; may not be instantiated.
	 * </pre>
	 * 
	 * @param mod
	 */
	final static int					ACC_PUBLIC		= 0x0001;												// Declared
	// public;
	// may
	// be
	// accessed
	// from outside its package.
	final static int					ACC_FINAL		= 0x0010;												// Declared
	// final;
	// no
	// subclasses
	// allowed.
	final static int					ACC_SUPER		= 0x0020;												// Treat
	// superclass
	// methods
	// specially when invoked by the
	// invokespecial instruction.
	final static int					ACC_INTERFACE	= 0x0200;												// Is
	// an
	// interface,
	// not
	// a
	// classs
	final static int					ACC_ABSTRACT	= 0x0400;												// Declared

	// a thing not in the source code
	final static int					ACC_SYNTHETIC	= 0x1000;
	final static int					ACC_ANNOTATION	= 0x2000;
	final static int					ACC_ENUM		= 0x4000;

	static protected class Assoc {
		Assoc(byte tag, int a, int b) {
			this.tag = tag;
			this.a = a;
			this.b = b;
		}

		byte	tag;
		int		a;
		int		b;
	}

	public class Def {
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
			annotations.add(analyzer.getTypeRef(a.name.getBinary()));
		}

		public Collection<TypeRef> getAnnotations() {
			return annotations;
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

		public String getName() {
			return name;
		}

		public String toString() {
			return getName();
		}

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
				throw new IllegalArgumentException("Not a valid method descriptor: " + descriptor);

			String returnType = m.group(2);
			return objectDescriptorToFQN(returnType);
		}

		public String getSignature() {
			return signature;
		}

	}

	public class MethodDef extends FieldDef {
		public MethodDef(int access, String method, String descriptor) {
			super(access, method, descriptor);
		}

		public boolean isConstructor() {
			return name.equals("<init>") || name.equals("<clinit>");
		}

		public TypeRef[] getPrototype() {
			return descriptor.getPrototype();
		}

	}

	final static byte	SkipTable[]	= { //
			0, // 0 non existent
			-1, // 1 CONSTANT_utf8 UTF 8, handled in
			// method
			-1, // 2
			4, // 3 CONSTANT_Integer
			4, // 4 CONSTANT_Float
			8, // 5 CONSTANT_Long (index +=2!)
			8, // 6 CONSTANT_Double (index +=2!)
			-1, // 7 CONSTANT_Class
			2, // 8 CONSTANT_String
			4, // 9 CONSTANT_FieldRef
			4, // 10 CONSTANT_MethodRef
			4, // 11 CONSTANT_InterfaceMethodRef
			4, // 12 CONSTANT_NameAndType
			-1, // 13 Not defined
			-1, // 14 Not defined
			3, // 15 CONSTANT_MethodHandle
			2, // 16 CONSTANT_MethodType
			-1, // 17 Not defined
			4, // 18 CONSTANT_InvokeDynamic
									};

	boolean				hasRuntimeAnnotations;
	boolean				hasClassAnnotations;

	TypeRef				className;
	Object				pool[];
	int					intPool[];
	Set<PackageRef>		imports		= Create.set();
	String				path;
	int					minor		= 0;
	int					major		= 0;
	int					innerAccess	= -1;
	int					accessx		= 0;
	String				sourceFile;
	Set<TypeRef>		xref;
	Set<Integer>		classes;
	Set<Integer>		descriptors;
	Set<TypeRef>		annotations;
	int					forName		= 0;
	int					class$		= 0;
	TypeRef[]			interfaces;
	TypeRef				zuper;
	ClassDataCollector	cd			= null;
	Resource			resource;
	FieldDef			last		= null;
	boolean				deprecated;

	final Analyzer		analyzer;

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
		InputStream in = resource.openInputStream();
		try {
			return parseClassFile(in, cd);
		}
		finally {
			in.close();
		}
	}

	public Set<TypeRef> parseClassFile(InputStream in, ClassDataCollector cd) throws Exception {
		DataInputStream din = new DataInputStream(in);
		try {
			this.cd = cd;
			return parseClassFile(din);
		}
		finally {
			cd = null;
			din.close();
		}
	}

	Set<TypeRef> parseClassFile(DataInputStream in) throws Exception {
		xref = new HashSet<TypeRef>();
		classes = new HashSet<Integer>();
		descriptors = new HashSet<Integer>();

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

		process: for (int poolIndex = 1; poolIndex < count; poolIndex++) {
			byte tag = in.readByte();
			switch (tag) {
				case 0 :
					break process;
				case 1 :
					constantUtf8(in, poolIndex);
					break;

				case 3 :
					constantInteger(in, poolIndex);
					break;

				case 4 :
					constantFloat(in, poolIndex);
					break;

				// For some insane optimization reason are
				// the long and the double two entries in the
				// constant pool. See 4.4.5
				case 5 :
					constantLong(in, poolIndex);
					poolIndex++;
					break;

				case 6 :
					constantDouble(in, poolIndex);
					poolIndex++;
					break;

				case 7 :
					constantClass(in, poolIndex);
					break;

				case 8 :
					constantString(in, poolIndex);
					break;

				case 10 : // Method ref
				case 11 : // Interface Method ref
					methodRef(in, poolIndex);
					break;

				// Name and Type
				case 12 :
					nameAndType(in, poolIndex, tag);
					break;

				// We get the skip count for each record type
				// from the SkipTable. This will also automatically
				// abort when
				default :
					if (tag == 2)
						throw new IOException("Invalid tag " + tag);
					in.skipBytes(SkipTable[tag]);
					break;
			}
		}

		pool(pool, intPool);
		/*
		 * Parse after the constant pool, code thanks to Hans Christian
		 * Falkenberg
		 */

		accessx = in.readUnsignedShort(); // access

		int this_class = in.readUnsignedShort();
		className = analyzer.getTypeRef((String) pool[intPool[this_class]]);

		try {

			if (cd != null) {
				if (!cd.classStart(accessx, className))
					return null;
			}

			int super_class = in.readUnsignedShort();
			String superName = (String) pool[intPool[super_class]];
			if (superName != null) {
				zuper = analyzer.getTypeRef(superName);
			}

			if (zuper != null) {
				referTo(zuper);
				if (cd != null)
					cd.extendsClass(zuper);
			}

			int interfacesCount = in.readUnsignedShort();
			if (interfacesCount > 0) {
				interfaces = new TypeRef[interfacesCount];
				for (int i = 0; i < interfacesCount; i++)
					interfaces[i] = analyzer.getTypeRef((String) pool[intPool[in.readUnsignedShort()]]);
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
				if (name.startsWith("class$")) {
					crawl = true;
				}
				if (cd != null)
					cd.field(last = new FieldDef(access_flags, name, pool[descriptor_index].toString()));
				descriptors.add(Integer.valueOf(descriptor_index));
				doAttributes(in, ElementType.FIELD, false);
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
			} else if (major == 48) {
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
			if (major >= JAVA.OpenJDK7.major)
				crawl = false;

			//
			// Handle the methods
			//
			int methodCount = in.readUnsignedShort();
			for (int i = 0; i < methodCount; i++) {
				int access_flags = in.readUnsignedShort();
				int name_index = in.readUnsignedShort();
				int descriptor_index = in.readUnsignedShort();
				descriptors.add(Integer.valueOf(descriptor_index));
				String name = pool[name_index].toString();
				String descriptor = pool[descriptor_index].toString();
				if (cd != null) {
					MethodDef mdef = new MethodDef(access_flags, name, descriptor);
					last = mdef;
					cd.method(mdef);
				}

				if ("<init>".equals(name)) {
					doAttributes(in, ElementType.CONSTRUCTOR, crawl);
				} else {
					doAttributes(in, ElementType.METHOD, crawl);
				}
			}
			if (cd != null)
				cd.memberEnd();

			doAttributes(in, ElementType.TYPE, false);

			//
			// Now iterate over all classes we found and
			// parse those as well. We skip duplicates
			//

			for (int n : classes) {
				String descr = (String) pool[n];

				TypeRef clazz = analyzer.getTypeRef(descr);
				referTo(clazz);
			}

			//
			// Parse all the descriptors we found
			//

			for (Iterator<Integer> e = descriptors.iterator(); e.hasNext();) {
				Integer index = e.next();
				String prototype = (String) pool[index.intValue()];
				if (prototype != null)
					parseDescriptor(prototype);
				else
					System.err.println("Unrecognized descriptor: " + index);
			}
			Set<TypeRef> xref = this.xref;
			reset();
			return xref;
		}
		finally {
			if (cd != null)
				cd.classEnd();
		}
	}

	private void constantFloat(DataInputStream in, int poolIndex) throws IOException {
		if (cd != null)
			pool[poolIndex] = in.readFloat(); // ALU
		else
			in.skipBytes(4);
	}

	private void constantInteger(DataInputStream in, int poolIndex) throws IOException {
		intPool[poolIndex] = in.readInt();
		if (cd != null)
			pool[poolIndex] = intPool[poolIndex];
	}

	protected void pool(@SuppressWarnings("unused") Object[] pool, @SuppressWarnings("unused") int[] intPool) {}

	/**
	 * @param in
	 * @param poolIndex
	 * @param tag
	 * @throws IOException
	 */
	protected void nameAndType(DataInputStream in, int poolIndex, byte tag) throws IOException {
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		descriptors.add(Integer.valueOf(descriptor_index));
		pool[poolIndex] = new Assoc(tag, name_index, descriptor_index);
	}

	/**
	 * @param in
	 * @param poolIndex
	 * @param tag
	 * @throws IOException
	 */
	private void methodRef(DataInputStream in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		pool[poolIndex] = new Assoc((byte) 10, class_index, name_and_type_index);
	}

	/**
	 * @param in
	 * @param poolIndex
	 * @throws IOException
	 */
	private void constantString(DataInputStream in, int poolIndex) throws IOException {
		int string_index = in.readUnsignedShort();
		intPool[poolIndex] = string_index;
	}

	/**
	 * @param in
	 * @param poolIndex
	 * @throws IOException
	 */
	protected void constantClass(DataInputStream in, int poolIndex) throws IOException {
		int class_index = in.readUnsignedShort();
		classes.add(Integer.valueOf(class_index));
		intPool[poolIndex] = class_index;
		ClassConstant c = new ClassConstant(class_index);
		pool[poolIndex] = c;
	}

	/**
	 * @param in
	 * @throws IOException
	 */
	protected void constantDouble(DataInputStream in, int poolIndex) throws IOException {
		if (cd != null)
			pool[poolIndex] = in.readDouble();
		else
			in.skipBytes(8);
	}

	/**
	 * @param in
	 * @throws IOException
	 */
	protected void constantLong(DataInputStream in, int poolIndex) throws IOException {
		if (cd != null) {
			pool[poolIndex] = in.readLong();
		} else
			in.skipBytes(8);
	}

	/**
	 * @param in
	 * @param poolIndex
	 * @throws IOException
	 */
	protected void constantUtf8(DataInputStream in, int poolIndex) throws IOException {
		// CONSTANT_Utf8

		String name = in.readUTF();
		pool[poolIndex] = name;
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
				if (methodref.tag == 10) {
					// Method ref
					int class_index = methodref.a;
					int class_name_index = intPool[class_index];
					if (clazz.equals(pool[class_name_index])) {
						int name_and_type_index = methodref.b;
						Assoc name_and_type = (Assoc) pool[name_and_type_index];
						if (name_and_type.tag == 12) {
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
				}
			}
		}
		return -1;
	}

	/**
	 * Called for each attribute in the class, field, or method.
	 * 
	 * @param in
	 *            The stream
	 * @throws Exception
	 */
	private void doAttributes(DataInputStream in, ElementType member, boolean crawl) throws Exception {
		int attributesCount = in.readUnsignedShort();
		for (int j = 0; j < attributesCount; j++) {
			// skip name CONSTANT_Utf8 pointer
			doAttribute(in, member, crawl);
		}
	}

	/**
	 * Process a single attribute, if not recognized, skip it.
	 * 
	 * @param in
	 *            the data stream
	 * @throws Exception
	 */
	private void doAttribute(DataInputStream in, ElementType member, boolean crawl) throws Exception {
		int attribute_name_index = in.readUnsignedShort();
		String attributeName = (String) pool[attribute_name_index];
		long attribute_length = in.readInt();
		attribute_length &= 0xFFFFFFFF;
		if ("Deprecated".equals(attributeName)) {
			if (cd != null)
				cd.deprecated();
		} else if ("RuntimeVisibleAnnotations".equals(attributeName))
			doAnnotations(in, member, RetentionPolicy.RUNTIME);
		else if ("RuntimeVisibleParameterAnnotations".equals(attributeName))
			doParameterAnnotations(in, member, RetentionPolicy.RUNTIME);
		else if ("RuntimeInvisibleAnnotations".equals(attributeName))
			doAnnotations(in, member, RetentionPolicy.CLASS);
		else if ("RuntimeInvisibleParameterAnnotations".equals(attributeName))
			doParameterAnnotations(in, member, RetentionPolicy.CLASS);
		else if ("InnerClasses".equals(attributeName))
			doInnerClasses(in);
		else if ("EnclosingMethod".equals(attributeName))
			doEnclosingMethod(in);
		else if ("SourceFile".equals(attributeName))
			doSourceFile(in);
		else if ("Code".equals(attributeName) && crawl)
			doCode(in);
		else if ("Signature".equals(attributeName))
			doSignature(in, member);
		else if ("ConstantValue".equals(attributeName))
			doConstantValue(in);
		else {
			if (attribute_length > 0x7FFFFFFF) {
				throw new IllegalArgumentException("Attribute > 2Gb");
			}
			in.skipBytes((int) attribute_length);
		}
	}

	/**
	 * <pre>
	 * EnclosingMethod_attribute { 
	 * 	u2 attribute_name_index; 
	 * 	u4 attribute_length; 
	 * 	u2 class_index
	 * 	u2 method_index;
	 * }
	 * </pre>
	 * 
	 * @param in
	 * @throws IOException
	 */
	private void doEnclosingMethod(DataInputStream in) throws IOException {
		int cIndex = in.readShort();
		int mIndex = in.readShort();

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
	 * InnerClasses_attribute {
	 * 	u2 attribute_name_index; 
	 * 	u4 attribute_length; 
	 * 	u2 number_of_classes; {	
	 * 		u2 inner_class_info_index;
	 * 		u2 outer_class_info_index; 
	 * 		u2 inner_name_index; 
	 * 		u2 inner_class_access_flags;
	 * 	} classes[number_of_classes];
	 * }
	 * </pre>
	 * 
	 * @param in
	 * @throws Exception
	 */
	private void doInnerClasses(DataInputStream in) throws Exception {
		int number_of_classes = in.readShort();
		for (int i = 0; i < number_of_classes; i++) {
			int inner_class_info_index = in.readShort();
			int outer_class_info_index = in.readShort();
			int inner_name_index = in.readShort();
			int inner_class_access_flags = in.readShort() & 0xFFFF;

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
	 * Signature_attribute { 
	 *     u2 attribute_name_index; 
	 *     u4 attribute_length; 
	 *     u2 signature_index; 
	 *     }
	 * </pre>
	 * 
	 * @param member
	 */

	void doSignature(DataInputStream in, ElementType member) throws IOException {
		int signature_index = in.readUnsignedShort();
		String signature = (String) pool[signature_index];

		// s.println("Signature " + signature );

		// // The type signature is kind of weird,
		// // lets skip it for now. Seems to be some kind of
		// // type variable name index but it does not seem to
		// // conform to the language specification.
		// if (member != ElementType.TYPE)
		parseDescriptor(signature);

		if (last != null)
			last.signature = signature;

		if (cd != null)
			cd.signature(signature);
	}

	/**
	 * Handle a constant value call the data collector with it
	 */
	void doConstantValue(DataInputStream in) throws IOException {
		int constantValue_index = in.readUnsignedShort();
		if (cd == null)
			return;

		Object object = pool[constantValue_index];
		if (object == null)
			object = pool[intPool[constantValue_index]];

		last.constant = object;
		cd.constant(object);
	}

	/**
	 * <pre>
	 * Code_attribute {
	 * 		u2 attribute_name_index;
	 * 		u4 attribute_length;
	 * 		u2 max_stack;
	 * 		u2 max_locals;
	 * 		u4 code_length;
	 * 		u1 code[code_length];
	 * 		u2 exception_table_length;
	 * 		{    	u2 start_pc;
	 * 		      	u2 end_pc;
	 * 		      	u2  handler_pc;
	 * 		      	u2  catch_type;
	 * 		}	exception_table[exception_table_length];
	 * 		u2 attributes_count;
	 * 		attribute_info attributes[attributes_count];
	 * 	}
	 * </pre>
	 * 
	 * @param in
	 * @param pool
	 * @throws Exception
	 */
	private void doCode(DataInputStream in) throws Exception {
		/* int max_stack = */in.readUnsignedShort();
		/* int max_locals = */in.readUnsignedShort();
		int code_length = in.readInt();
		byte code[] = new byte[code_length];
		in.readFully(code);
		crawl(code);
		int exception_table_length = in.readUnsignedShort();
		in.skipBytes(exception_table_length * 8);
		doAttributes(in, ElementType.METHOD, false);
	}

	/**
	 * We must find Class.forName references ...
	 * 
	 * @param code
	 */
	protected void crawl(byte[] code) {
		ByteBuffer bb = ByteBuffer.wrap(code);
		bb.order(ByteOrder.BIG_ENDIAN);
		int lastReference = -1;

		while (bb.remaining() > 0) {
			int instruction = 0xFF & bb.get();
			switch (instruction) {
				case OpCodes.ldc :
					lastReference = 0xFF & bb.get();
					break;

				case OpCodes.ldc_w :
					lastReference = 0xFFFF & bb.getShort();
					break;

				case OpCodes.invokespecial : {
					int mref = 0xFFFF & bb.getShort();
					if (cd != null)
						getMethodDef(0, mref);
					break;
				}

				case OpCodes.invokevirtual : {
					int mref = 0xFFFF & bb.getShort();
					if (cd != null)
						getMethodDef(0, mref);
					break;
				}

				case OpCodes.invokeinterface : {
					int mref = 0xFFFF & bb.getShort();
					if (cd != null)
						getMethodDef(0, mref);
					break;
				}

				case OpCodes.invokestatic : {
					int methodref = 0xFFFF & bb.getShort();
					if (cd != null)
						getMethodDef(0, methodref);

					if ((methodref == forName || methodref == class$) && lastReference != -1
							&& pool[intPool[lastReference]] instanceof String) {
						String fqn = (String) pool[intPool[lastReference]];
						if (!fqn.equals("class") && fqn.indexOf('.') > 0) {
							TypeRef clazz = analyzer.getTypeRefFromFQN(fqn);
							referTo(clazz);
						}
						lastReference = -1;
					}
					break;
				}

				case OpCodes.tableswitch :
					// Skip to place divisible by 4
					while ((bb.position() & 0x3) != 0)
						bb.get();
					/* int deflt = */
					bb.getInt();
					int low = bb.getInt();
					int high = bb.getInt();
					try {
						bb.position(bb.position() + (high - low + 1) * 4);
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					lastReference = -1;
					break;

				case OpCodes.lookupswitch :
					// Skip to place divisible by 4
					while ((bb.position() & 0x3) != 0)
						bb.get();
					/* deflt = */
					bb.getInt();
					int npairs = bb.getInt();
					bb.position(bb.position() + npairs * 8);
					lastReference = -1;
					break;

				default :
					lastReference = -1;
					bb.position(bb.position() + OpCodes.OFFSETS[instruction]);
			}
		}
	}

	private void doSourceFile(DataInputStream in) throws IOException {
		int sourcefile_index = in.readUnsignedShort();
		this.sourceFile = pool[sourcefile_index].toString();
	}

	private void doParameterAnnotations(DataInputStream in, ElementType member, RetentionPolicy policy)
			throws IOException {
		int num_parameters = in.readUnsignedByte();
		for (int p = 0; p < num_parameters; p++) {
			if (cd != null)
				cd.parameter(p);
			doAnnotations(in, member, policy);
		}
	}

	private void doAnnotations(DataInputStream in, ElementType member, RetentionPolicy policy) throws IOException {
		int num_annotations = in.readUnsignedShort(); // # of annotations
		for (int a = 0; a < num_annotations; a++) {
			if (cd == null)
				doAnnotation(in, member, policy, false);
			else {
				Annotation annotion = doAnnotation(in, member, policy, true);
				cd.annotation(annotion);
			}
		}
	}

	private Annotation doAnnotation(DataInputStream in, ElementType member, RetentionPolicy policy, boolean collect)
			throws IOException {
		int type_index = in.readUnsignedShort();
		if (annotations == null)
			annotations = new HashSet<TypeRef>();

		TypeRef tr = analyzer.getTypeRef(pool[type_index].toString());
		annotations.add(tr);

		if (policy == RetentionPolicy.RUNTIME) {
			descriptors.add(Integer.valueOf(type_index));
			hasRuntimeAnnotations = true;
		} else {
			hasClassAnnotations = true;
		}
		TypeRef name = analyzer.getTypeRef((String) pool[type_index]);
		int num_element_value_pairs = in.readUnsignedShort();
		Map<String,Object> elements = null;
		for (int v = 0; v < num_element_value_pairs; v++) {
			int element_name_index = in.readUnsignedShort();
			String element = (String) pool[element_name_index];
			Object value = doElementValue(in, member, policy, collect);
			if (collect) {
				if (elements == null)
					elements = new LinkedHashMap<String,Object>();
				elements.put(element, value);
			}
		}
		if (collect)
			return new Annotation(name, elements, member, policy);
		return null;
	}

	private Object doElementValue(DataInputStream in, ElementType member, RetentionPolicy policy, boolean collect)
			throws IOException {
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
				if (policy == RetentionPolicy.RUNTIME)
					descriptors.add(Integer.valueOf(type_name_index));
				int const_name_index = in.readUnsignedShort();
				return pool[const_name_index];

			case 'c' : // Class
				int class_info_index = in.readUnsignedShort();
				if (policy == RetentionPolicy.RUNTIME)
					descriptors.add(Integer.valueOf(class_info_index));
				return pool[class_info_index];

			case '@' : // Annotation type
				return doAnnotation(in, member, policy, collect);

			case '[' : // Array
				int num_values = in.readUnsignedShort();
				Object[] result = new Object[num_values];
				for (int i = 0; i < num_values; i++) {
					result[i] = doElementValue(in, member, policy, collect);
				}
				return result;

			default :
				throw new IllegalArgumentException("Invalid value for Annotation ElementValue tag " + tag);
		}
	}

	/**
	 * Add a new package reference.
	 * 
	 * @param packageRef
	 *            A '.' delimited package name
	 */
	void referTo(TypeRef typeRef) {
		if (xref != null)
			xref.add(typeRef);
		if (typeRef.isPrimitive())
			return;

		PackageRef packageRef = typeRef.getPackageRef();
		if (packageRef.isPrimitivePackage())
			return;

		imports.add(packageRef);
	}

	/**
	 * This method parses a descriptor and adds the package of the descriptor to
	 * the referenced packages. The syntax of the descriptor is:
	 * 
	 * <pre>
	 *   descriptor ::= ( '(' reference * ')' )? reference
	 *   reference  ::= 'L' classname ( '&lt;' references '&gt;' )? ';' | 'B' | 'Z' | ... | '+' | '-' | '['
	 * </pre>
	 * 
	 * This methods uses heavy recursion to parse the descriptor and a roving
	 * pointer to limit the creation of string objects.
	 * 
	 * @param descriptor
	 *            The to be parsed descriptor
	 * @param rover
	 *            The pointer to start at
	 */
	public void parseDescriptor(String descriptor) {
		// Some descriptors are weird, they start with a generic
		// declaration that contains ':', not sure what they mean ...
		int rover = 0;
		if (descriptor.charAt(0) == '<') {
			rover = parseFormalTypeParameters(descriptor, rover);
		}

		if (descriptor.charAt(rover) == '(') {
			rover = parseReferences(descriptor, rover + 1, ')');
			rover++;
		}
		parseReferences(descriptor, rover, (char) 0);
	}

	/**
	 * Parse a sequence of references. A sequence ends with a given character or
	 * when the string ends.
	 * 
	 * @param descriptor
	 *            The whole descriptor.
	 * @param rover
	 *            The index in the descriptor
	 * @param delimiter
	 *            The end character or 0
	 * @return the last index processed, one character after the delimeter
	 */
	int parseReferences(String descriptor, int rover, char delimiter) {
		int r = rover;
		while (r < descriptor.length() && descriptor.charAt(r) != delimiter) {
			r = parseReference(descriptor, r);
		}
		return r;
	}

	/**
	 * Parse a single reference. This can be a single character or an object
	 * reference when it starts with 'L'.
	 * 
	 * @param descriptor
	 *            The descriptor
	 * @param rover
	 *            The place to start
	 * @return The return index after the reference
	 */
	int parseReference(String descriptor, int rover) {
		int r = rover;
		char c = descriptor.charAt(r);
		while (c == '[')
			c = descriptor.charAt(++r);

		if (c == '<') {
			r = parseReferences(descriptor, r + 1, '>');
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
					r = parseReferences(descriptor, r + 1, '>');
				} else
					sb.append(c);
				r++;
			}
			TypeRef ref = analyzer.getTypeRef(sb.toString());
			if (cd != null)
				cd.addReference(ref);

			referTo(ref);
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
	 * @return
	 */
	private int parseFormalTypeParameters(String descriptor, int index) {
		index++;
		while (descriptor.charAt(index) != '>') {
			// Skip IDENTIFIER
			index = descriptor.indexOf(':', index) + 1;
			if (index == 0)
				throw new IllegalArgumentException("Expected IDENTIFIER: " + descriptor);

			// ClassBound? InterfaceBounds

			char c = descriptor.charAt(index);

			// Class Bound?
			if (c == 'L' || c == 'T') {
				index = parseReference(descriptor, index); // class reference
				c = descriptor.charAt(index);
			}

			// Interface Bounds
			while (c == ':') {
				index++;
				index = parseReference(descriptor, index);
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
		pool = null;
		intPool = null;
		xref = null;
		classes = null;
		descriptors = null;
	}

	public boolean is(QUERY query, Instruction instr, Analyzer analyzer) throws Exception {
		switch (query) {
			case ANY :
				return true;

			case NAMED :
				if (instr.matches(getClassName().getDottedOnly()))
					return !instr.isNegated();
				return false;

			case VERSION :
				String v = major + "." + minor;
				if (instr.matches(v))
					return !instr.isNegated();
				return false;

			case IMPLEMENTS :
				for (int i = 0; interfaces != null && i < interfaces.length; i++) {
					if (instr.matches(interfaces[i].getDottedOnly()))
						return !instr.isNegated();
				}
				break;

			case EXTENDS :
				if (zuper == null)
					return false;

				if (instr.matches(zuper.getDottedOnly()))
					return !instr.isNegated();
				break;

			case PUBLIC :
				return Modifier.isPublic(accessx);

			case CONCRETE :
				return !Modifier.isAbstract(accessx);

			case ANNOTATED :
				if (annotations == null)
					return false;

				for (TypeRef annotation : annotations) {
					if (instr.matches(annotation.getFQN()))
						return !instr.isNegated();
				}

				return false;

			case RUNTIMEANNOTATIONS :
				return hasClassAnnotations;
			case CLASSANNOTATIONS :
				return hasClassAnnotations;

			case ABSTRACT :
				return Modifier.isAbstract(accessx);

			case IMPORTS :
				for (PackageRef imp : imports) {
					if (instr.matches(imp.getFQN()))
						return !instr.isNegated();
				}
		}

		if (zuper == null)
			return false;

		Clazz clazz = analyzer.findClass(zuper);
		if (clazz == null)
			return false;

		return clazz.is(query, instr, analyzer);
	}

	public String toString() {
		return className.getFQN();
	}

	/**
	 * Called when crawling the byte code and a method reference is found
	 */
	void getMethodDef(int access, int methodRefPoolIndex) {
		if (methodRefPoolIndex == 0)
			return;

		Object o = pool[methodRefPoolIndex];
		if (o != null && o instanceof Assoc) {
			Assoc assoc = (Assoc) o;
			if (assoc.tag == 10) {
				int string_index = intPool[assoc.a];
				TypeRef className = analyzer.getTypeRef((String) pool[string_index]);
				int name_and_type_index = assoc.b;
				Assoc name_and_type = (Assoc) pool[name_and_type_index];
				if (name_and_type.tag == 12) {
					// Name and Type
					int name_index = name_and_type.a;
					int type_index = name_and_type.b;
					String method = (String) pool[name_index];
					String descriptor = (String) pool[type_index];
					cd.referenceMethod(access, className, method, descriptor);
				} else
					throw new IllegalArgumentException(
							"Invalid class file (or parsing is wrong), assoc is not type + name (12)");
			} else
				throw new IllegalArgumentException(
						"Invalid class file (or parsing is wrong), Assoc is not method ref! (10)");
		} else
			throw new IllegalArgumentException("Invalid class file (or parsing is wrong), Not an assoc at a method ref");
	}

	public boolean isPublic() {
		return Modifier.isPublic(accessx);
	}

	public boolean isProtected() {
		return Modifier.isProtected(accessx);
	}

	public boolean isEnum() {
		return zuper != null && zuper.getBinary().equals("java/lang/Enum");
	}

	public JAVA getFormat() {
		return JAVA.format(major);

	}

	public static String objectDescriptorToFQN(String string) {
		if (string.startsWith("L") && string.endsWith(";"))
			return string.substring(1, string.length() - 1).replace('/', '.');

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
			if (c == '_' || c == '$' || c == '.') {
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
	 * @return
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
}
