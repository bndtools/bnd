package aQute.lib.osgi;

import java.beans.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.annotation.*;
import aQute.libg.generics.*;

public class Clazz {

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
		UNKNOWN(Integer.MAX_VALUE), OpenJDK7(51), J2S6(50), J2SE5(49), JDK1_4(48), JDK1_3(47), JDK1_2(
				46), JDK1_1(45);

		final int	major;

		JAVA(int major) {
			this.major = major;
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
	};

	public static enum QUERY {
		IMPLEMENTS, EXTENDS, IMPORTS, NAMED, ANY, VERSION, CONCRETE, ABSTRACT, PUBLIC, ANNOTATION, RUNTIMEANNOTATIONS, CLASSANNOTATIONS
	};

	public static EnumSet<QUERY>	HAS_ARGUMENT	= EnumSet.of(QUERY.IMPLEMENTS, QUERY.EXTENDS,
															QUERY.IMPORTS, QUERY.NAMED,
															QUERY.VERSION, QUERY.ANNOTATION);

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
	final static int				ACC_PUBLIC		= 0x0001;									// Declared
	// public;
	// may
	// be
	// accessed
	// from outside its package.
	final static int				ACC_FINAL		= 0x0010;									// Declared
	// final;
	// no
	// subclasses
	// allowed.
	final static int				ACC_SUPER		= 0x0020;									// Treat
	// superclass
	// methods
	// specially when invoked by the
	// invokespecial instruction.
	final static int				ACC_INTERFACE	= 0x0200;									// Is
	// an
	// interface,
	// not
	// a
	// classs
	final static int				ACC_ABSTRACT	= 0x0400;									// Declared

	// abstract;
	// may
	// not
	// be

	// instantiated.

	final static int				ACC_ENUM		= 0x04000;

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

	static public class Def {

	}

	static public class FieldDef extends Def implements Comparable<FieldDef> {
		public FieldDef(int access, String clazz, String name, String descriptor) {
			this.access = access;
			this.clazz = clazz.replace('/', '.');
			this.name = name;
			this.descriptor = descriptor;
		}

		final public int	access;
		final public String	clazz;
		final public String	name;
		final public String	descriptor;
		public String		signature;
		public Object		constant;

		public boolean equals(Object other) {
			if (!(other instanceof MethodDef))
				return false;

			FieldDef m = (FieldDef) other;
			return clazz.equals(m.clazz) && name.equals(m.name) && descriptor.equals(m.descriptor);
		}

		public int hashCode() {
			return clazz.hashCode() ^ name.hashCode() ^ descriptor.hashCode();
		}

		public int compareTo(FieldDef o) {
			int result = clazz.compareTo(o.clazz);
			if (result == 0) {
				result = name.compareTo(o.name);
				if (result == 0) {
					result = descriptor.compareTo(o.descriptor);
				}
			}
			return result;
		}

		public String getPretty() {
			return name;
		}

		public String toString() {
			return getPretty();
		}

		public boolean isEnum() {
			return (access & ACC_ENUM) != 0;
		}
	}

	static public class MethodDef extends FieldDef {
		Pattern	METHOD_DESCRIPTOR	= Pattern.compile("\\((.*)\\)(.+)");

		public MethodDef(int access, String clazz, String method, String descriptor) {
			super(access, clazz, method, descriptor);
		}

		public boolean isConstructor() {
			return name.equals("<init>") || name.equals("<clinit>");
		}

		public String getReturnType() {
			String use = descriptor;
			if (signature != null)
				use = signature;

			Matcher m = METHOD_DESCRIPTOR.matcher(use);
			if (!m.matches())
				throw new IllegalArgumentException("Not a valid method descriptor: " + descriptor);

			String returnType = m.group(2);
			return objectDescriptorToFQN(returnType);
		}

		public String getPretty() {

			StringBuilder sb = new StringBuilder();
			sb.append(descriptor.charAt(0));
			int index = 1;
			String del = "";
			while (index < descriptor.length() && descriptor.charAt(index) != ')') {
				sb.append(del);
				index = printParameter(sb, descriptor, index);
				del = ",";
			}
			sb.append(descriptor.charAt(index++));
			StringBuilder sb2 = new StringBuilder();
			if (isConstructor()) {
				sb2.append(getShortName(clazz));
				index++; // skip the V
			} else {
				printParameter(sb2, descriptor, index);
				sb2.append(" ");
				sb2.append(getShortName(clazz));
				sb2.append(".");
				sb2.append(name);
			}
			sb2.append(sb);
			return sb2.toString();
		}

		private int printParameter(StringBuilder sb, CharSequence descriptor, int index) {
			char c = descriptor.charAt(index++);
			switch (c) {
			case 'B':
				sb.append("byte");
				break;
			case 'C':
				sb.append("char");
				break;
			case 'D':
				sb.append("double");
				break;
			case 'F':
				sb.append("float");
				break;
			case 'I':
				sb.append("int");
				break;
			case 'J':
				sb.append("long");
				break;
			case 'S':
				sb.append("short");
				break;
			case 'Z':
				sb.append("boolean");
				break;
			case 'V':
				sb.append("void");
				break;
			case 'L':
				index = reference(sb, descriptor, index);
				break;

			case '[':
				index = array(sb, descriptor, index);
				break;
			}
			return index;
		}

		private int reference(StringBuilder sb, CharSequence descriptor, int index) {
			int n = sb.length();
			int lastSlash = n;
			while (index < descriptor.length() && descriptor.charAt(index) != ';') {
				char c = descriptor.charAt(index++);
				if (c == '/') {
					c = '.';
					lastSlash = sb.length() + 1;
				}
				sb.append(c);
			}
			if (lastSlash != n) {
				sb.delete(n, lastSlash);
			}
			return ++index;
		}

		private int array(StringBuilder sb, CharSequence descriptor, int index) {
			int n = 1;
			while (index < descriptor.length() && descriptor.charAt(index) == '[') {
				index++;
			}
			index = printParameter(sb, descriptor, index);
			while (n-- > 0) {
				sb.append("[]");
			}
			return index;
		}
	}

	final static byte	SkipTable[]	= { 0, // 0 non existent
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
									};

	boolean				hasRuntimeAnnotations;
	boolean				hasClassAnnotations;

	String				className;
	Object				pool[];
	int					intPool[];
	Set<String>			imports		= Create.set();
	String				path;
	int					minor		= 0;
	int					major		= 0;
	int					access		= 0;
	String				sourceFile;
	Set<String>			xref;
	Set<Integer>		classes;
	Set<Integer>		descriptors;
	Set<String>			annotations;
	int					forName		= 0;
	int					class$		= 0;
	String[]			interfaces;
	String				zuper;
	ClassDataCollector	cd			= null;
	Resource			resource;
	FieldDef			last		= null;

	public Clazz(String path, Resource resource) {
		this.path = path;
		this.resource = resource;
	}

	public Set<String> parseClassFile() throws Exception {
		return parseClassFileWithCollector(null);
	}

	public Set<String> parseClassFile(InputStream in) throws IOException {
		return parseClassFile(in, null);
	}

	public Set<String> parseClassFileWithCollector(ClassDataCollector cd) throws Exception {
		InputStream in = resource.openInputStream();
		try {
			return parseClassFile(in, cd);
		} finally {
			in.close();
		}
	}

	public Set<String> parseClassFile(InputStream in, ClassDataCollector cd) throws IOException {
		DataInputStream din = new DataInputStream(in);
		try {
			this.cd = cd;
			return parseClassFile(din);
		} finally {
			cd = null;
			din.close();
		}
	}

	Set<String> parseClassFile(DataInputStream in) throws IOException {

		xref = new HashSet<String>();
		classes = new HashSet<Integer>();
		descriptors = new HashSet<Integer>();

		boolean crawl = cd != null; // Crawl the byte code if we have a
		// collector
		int magic = in.readInt();
		if (magic != 0xCAFEBABE)
			throw new IOException("Not a valid class file (no CAFEBABE header)");

		minor = in.readUnsignedShort(); // minor version
		major = in.readUnsignedShort(); // major version
		if ( cd != null) 
			cd.version(minor,major);
		int count = in.readUnsignedShort();
		pool = new Object[count];
		intPool = new int[count];

		process: for (int poolIndex = 1; poolIndex < count; poolIndex++) {
			byte tag = in.readByte();
			switch (tag) {
			case 0:
				break process;
			case 1:
				constantUtf8(in, poolIndex);
				break;

			case 3:
				constantInteger(in, poolIndex);
				break;

			case 4:
				constantFloat(in, poolIndex);
				break;

			// For some insane optimization reason are
			// the long and the double two entries in the
			// constant pool. See 4.4.5
			case 5:
				constantLong(in, poolIndex);
				poolIndex++;
				break;

			case 6:
				constantDouble(in, poolIndex);
				poolIndex++;
				break;

			case 7:
				constantClass(in, poolIndex);
				break;

			case 8:
				constantString(in, poolIndex);
				break;

			case 10: // Method ref
			case 11: // Interface Method ref
				methodRef(in, poolIndex);
				break;

			// Name and Type
			case 12:
				nameAndType(in, poolIndex, tag);
				break;

			// We get the skip count for each record type
			// from the SkipTable. This will also automatically
			// abort when
			default:
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

		access = in.readUnsignedShort(); // access

		int this_class = in.readUnsignedShort();
		className = (String) pool[intPool[this_class]];

		try {

			if (cd != null) {
				if (!cd.classStart(access, className))
					return null;
			}

			int super_class = in.readUnsignedShort();
			zuper = (String) pool[intPool[super_class]];
			if (zuper != null) {
				String pack = getPackage(zuper);
				packageReference(pack);
				if (cd != null)
					cd.extendsClass(zuper);
			}

			int interfacesCount = in.readUnsignedShort();
			if (interfacesCount > 0) {
				interfaces = new String[interfacesCount];
				for (int i = 0; i < interfacesCount; i++)
					interfaces[i] = (String) pool[intPool[in.readUnsignedShort()]];
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
					cd.field(last = new FieldDef(access_flags, className, name,
							pool[descriptor_index].toString()));
				descriptors.add(new Integer(descriptor_index));
				doAttributes(in, ElementType.FIELD, false);
			}

			//
			// Check if we have to crawl the code to find
			// the ldc(_w) <string constant> invokestatic Class.forName
			// if so, calculate the method ref index so we
			// can do this efficiently
			//
			if (crawl) {
				forName = findMethodReference("java/lang/Class", "forName",
						"(Ljava/lang/String;)Ljava/lang/Class;");
				class$ = findMethodReference(className, "class$",
						"(Ljava/lang/String;)Ljava/lang/Class;");
			} else if (major == 48) {
				forName = findMethodReference("java/lang/Class", "forName",
						"(Ljava/lang/String;)Ljava/lang/Class;");
				if (forName > 0) {
					crawl = true;
					class$ = findMethodReference(className, "class$",
							"(Ljava/lang/String;)Ljava/lang/Class;");
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
				descriptors.add(new Integer(descriptor_index));
				String name = pool[name_index].toString();
				String descriptor = pool[descriptor_index].toString();
				if (cd != null) {
					MethodDef mdef = new MethodDef(access_flags, className, name, descriptor);
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
				String clazz = (String) pool[n];
				if (clazz.endsWith(";") || clazz.startsWith("["))
					parseReference(clazz, 0);
				else {

					String pack = getPackage(clazz);
					packageReference(pack);
				}
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
			Set<String> xref = this.xref;
			reset();
			return xref;
		} finally {
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

	protected void pool(Object[] pool, int[] intPool) {
	}

	/**
	 * @param in
	 * @param poolIndex
	 * @param tag
	 * @throws IOException
	 */
	protected void nameAndType(DataInputStream in, int poolIndex, byte tag) throws IOException {
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		descriptors.add(new Integer(descriptor_index));
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
		classes.add(new Integer(class_index));
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
		xref.add(name);
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
	 * @throws IOException
	 */
	private void doAttributes(DataInputStream in, ElementType member, boolean crawl)
			throws IOException {
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
	 * @throws IOException
	 */
	private void doAttribute(DataInputStream in, ElementType member, boolean crawl)
			throws IOException {
		int attribute_name_index = in.readUnsignedShort();
		String attributeName = (String) pool[attribute_name_index];
		long attribute_length = in.readInt();
		attribute_length &= 0xFFFFFFFF;
		if ("RuntimeVisibleAnnotations".equals(attributeName))
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
	 * 
	 * @param in
	 * @throws IOException
	 */
	private void doEnclosingMethod(DataInputStream in) throws IOException {
		int cIndex = in.readShort();
		int mIndex = in.readShort();

		if (cd != null) {
			int nameIndex = intPool[cIndex];
			String cName = (String) pool[nameIndex];

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
	 * @throws IOException
	 */
	private void doInnerClasses(DataInputStream in) throws IOException {
		int number_of_classes = in.readShort();
		for (int i = 0; i < number_of_classes; i++) {
			int inner_class_info_index = in.readShort();
			int outer_class_info_index = in.readShort();
			int inner_name_index = in.readShort();
			int inner_class_access_flags = in.readShort() & 0xFFFF;

			if (cd != null) {
				String innerClass = null;
				String outerClass = null;
				String innerName = null;

				if (inner_class_info_index != 0) {
					int nameIndex = intPool[inner_class_info_index];
					innerClass = (String) pool[nameIndex];
				}

				if (outer_class_info_index != 0) {
					int nameIndex = intPool[outer_class_info_index];
					outerClass = (String) pool[nameIndex];
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

		// System.out.println("Signature " + signature );

		// The type signature is kind of weird,
		// lets skip it for now. Seems to be some kind of
		// type variable name index but it does not seem to
		// conform to the language specification.
		if (member != ElementType.TYPE)
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
	 * @throws IOException
	 */
	private void doCode(DataInputStream in) throws IOException {
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
			case OpCodes.ldc:
				lastReference = 0xFF & bb.get();
				break;

			case OpCodes.ldc_w:
				lastReference = 0xFFFF & bb.getShort();
				break;

			case OpCodes.invokespecial: {
				int mref = 0xFFFF & bb.getShort();
				if (cd != null)
					cd.reference(getMethodDef(0, mref));
				break;
			}

			case OpCodes.invokevirtual: {
				int mref = 0xFFFF & bb.getShort();
				if (cd != null)
					cd.reference(getMethodDef(0, mref));
				break;
			}

			case OpCodes.invokeinterface: {
				int mref = 0xFFFF & bb.getShort();
				if (cd != null)
					cd.reference(getMethodDef(0, mref));
				break;
			}

			case OpCodes.invokestatic: {
				int methodref = 0xFFFF & bb.getShort();
				if (cd != null)
					cd.reference(getMethodDef(0, methodref));

				if ((methodref == forName || methodref == class$) && lastReference != -1
						&& pool[intPool[lastReference]] instanceof String) {
					String clazz = (String) pool[intPool[lastReference]];
					if (clazz.startsWith("[") || clazz.endsWith(";"))
						parseReference(clazz, 0);
					else {
						int n = clazz.lastIndexOf('.');
						if (n > 0)
							packageReference(clazz.substring(0, n));
					}
				}
				break;
			}

			case OpCodes.tableswitch:
				// Skip to place divisible by 4
				while ((bb.position() & 0x3) != 0)
					bb.get();
				/* int deflt = */
				bb.getInt();
				int low = bb.getInt();
				int high = bb.getInt();
				try {
					bb.position(bb.position() + (high - low + 1) * 4);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lastReference = -1;
				break;

			case OpCodes.lookupswitch:
				// Skip to place divisible by 4
				while ((bb.position() & 0x3) != 0)
					bb.get();
				/* deflt = */
				bb.getInt();
				int npairs = bb.getInt();
				bb.position(bb.position() + npairs * 8);
				lastReference = -1;
				break;

			default:
				lastReference = -1;
				bb.position(bb.position() + OpCodes.OFFSETS[instruction]);
			}
		}
	}

	private void doSourceFile(DataInputStream in) throws IOException {
		int sourcefile_index = in.readUnsignedShort();
		this.sourceFile = pool[sourcefile_index].toString();
	}

	private void doParameterAnnotations(DataInputStream in, ElementType member,
			RetentionPolicy policy) throws IOException {
		int num_parameters = in.readUnsignedByte();
		for (int p = 0; p < num_parameters; p++) {
			if (cd != null)
				cd.parameter(p);
			doAnnotations(in, member, policy);
		}
	}

	private void doAnnotations(DataInputStream in, ElementType member, RetentionPolicy policy)
			throws IOException {
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

	private Annotation doAnnotation(DataInputStream in, ElementType member, RetentionPolicy policy,
			boolean collect) throws IOException {
		int type_index = in.readUnsignedShort();
		if (annotations == null)
			annotations = new HashSet<String>();

		annotations.add(pool[type_index].toString());

		if (policy == RetentionPolicy.RUNTIME) {
			descriptors.add(new Integer(type_index));
			hasRuntimeAnnotations = true;
		} else {
			hasClassAnnotations = true;
		}
		String name = (String) pool[type_index];
		int num_element_value_pairs = in.readUnsignedShort();
		Map<String, Object> elements = null;
		for (int v = 0; v < num_element_value_pairs; v++) {
			int element_name_index = in.readUnsignedShort();
			String element = (String) pool[element_name_index];
			Object value = doElementValue(in, member, policy, collect);
			if (collect) {
				if (elements == null)
					elements = new LinkedHashMap<String, Object>();
				elements.put(element, value);
			}
		}
		if (collect)
			return new Annotation(name, elements, member, policy);
		else
			return null;
	}

	private Object doElementValue(DataInputStream in, ElementType member, RetentionPolicy policy,
			boolean collect) throws IOException {
		char tag = (char) in.readUnsignedByte();
		switch (tag) {
		case 'B': // Byte
		case 'C': // Character
		case 'I': // Integer
		case 'S': // Short
			int const_value_index = in.readUnsignedShort();
			return intPool[const_value_index];

		case 'D': // Double
		case 'F': // Float
		case 's': // String
		case 'J': // Long
			const_value_index = in.readUnsignedShort();
			return pool[const_value_index];

		case 'Z': // Boolean
			const_value_index = in.readUnsignedShort();
			return pool[const_value_index] == null || pool[const_value_index].equals(0) ? false
					: true;

		case 'e': // enum constant
			int type_name_index = in.readUnsignedShort();
			if (policy == RetentionPolicy.RUNTIME)
				descriptors.add(new Integer(type_name_index));
			int const_name_index = in.readUnsignedShort();
			return pool[const_name_index];

		case 'c': // Class
			int class_info_index = in.readUnsignedShort();
			if (policy == RetentionPolicy.RUNTIME)
				descriptors.add(new Integer(class_info_index));
			return pool[class_info_index];

		case '@': // Annotation type
			return doAnnotation(in, member, policy, collect);

		case '[': // Array
			int num_values = in.readUnsignedShort();
			Object[] result = new Object[num_values];
			for (int i = 0; i < num_values; i++) {
				result[i] = doElementValue(in, member, policy, collect);
			}
			return result;

		default:
			throw new IllegalArgumentException("Invalid value for Annotation ElementValue tag "
					+ tag);
		}
	}

	/**
	 * Add a new package reference.
	 * 
	 * @param pack
	 *            A '.' delimited package name
	 */
	void packageReference(String pack) {
		imports.add(pack);
	}

	/**
	 * This method parses a descriptor and adds the package of the descriptor to
	 * the referenced packages.
	 * 
	 * The syntax of the descriptor is:
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
		if (descriptor.charAt(0) == '<')
			return;

		int rover = 0;
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
		while (rover < descriptor.length() && descriptor.charAt(rover) != delimiter) {
			rover = parseReference(descriptor, rover);
		}
		return rover;
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

		char c = descriptor.charAt(rover);
		while (c == '[')
			c = descriptor.charAt(++rover);

		if (c == '<') {
			rover = parseReferences(descriptor, rover + 1, '>');
		} else if (c == 'T') {
			// Type variable name
			rover++;
			while (descriptor.charAt(rover) != ';')
				rover++;
		} else if (c == 'L') {
			StringBuilder sb = new StringBuilder();
			rover++;
			int lastSlash = -1;
			while ((c = descriptor.charAt(rover)) != ';') {
				if (c == '<') {
					rover = parseReferences(descriptor, rover + 1, '>');
				} else if (c == '/') {
					lastSlash = sb.length();
					sb.append('.');
				} else
					sb.append(c);
				rover++;
			}
			if (cd != null)
				cd.addReference(sb.toString());

			if (lastSlash > 0)
				packageReference(sb.substring(0, lastSlash));
		} else {
			if ("+-*BCDFIJSZV".indexOf(c) < 0)
				;// System.out.println("Should not skip: " + c);
		}

		// this skips a lot of characters
		// [, *, +, -, B, etc.

		return rover + 1;
	}

	public static String getPackage(String clazz) {
		int n = clazz.lastIndexOf('/');
		if (n < 0) {
			n = clazz.lastIndexOf('.');
			if (n < 0)
				return ".";
		}
		return clazz.substring(0, n).replace('/', '.');
	}

	public Set<String> getReferred() {
		return imports;
	}

	String getClassName() {
		if (className == null)
			return "NOCLASSNAME";
		return className;
	}

	public String getPath() {
		return path;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * .class construct for different compilers
	 * 
	 * sun 1.1 Detect static variable class$com$acme$MyClass 1.2 " 1.3 " 1.4 "
	 * 1.5 ldc_w (class) 1.6 "
	 * 
	 * eclipse 1.1 class$0, ldc (string), invokestatic Class.forName 1.2 " 1.3 "
	 * 1.5 ldc (class) 1.6 "
	 * 
	 * 1.5 and later is not an issue, sun pre 1.5 is easy to detect the static
	 * variable that decodes the class name. For eclipse, the class$0 gives away
	 * we have a reference encoded in a string.
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
		case ANY:
			return true;

		case NAMED:
			if (instr.matches(getClassName()))
				return !instr.isNegated();
			return false;

		case VERSION:
			String v = major + "/" + minor;
			if (instr.matches(v))
				return !instr.isNegated();
			return false;

		case IMPLEMENTS:
			for (int i = 0; interfaces != null && i < interfaces.length; i++) {
				if (instr.matches(interfaces[i]))
					return !instr.isNegated();
			}
			break;

		case EXTENDS:
			if (zuper == null)
				return false;

			if (instr.matches(zuper))
				return !instr.isNegated();
			break;

		case PUBLIC:
			return Modifier.isPublic(access);

		case CONCRETE:
			return !Modifier.isAbstract(access);

		case ANNOTATION:
			if (annotations == null)
				return false;

			if (annotations.contains(instr.getPattern()))
				return true;

			for (String annotation : annotations) {
				if (instr.matches(annotation))
					return !instr.isNegated();
			}

			return false;

		case RUNTIMEANNOTATIONS:
			return hasClassAnnotations;
		case CLASSANNOTATIONS:
			return hasClassAnnotations;

		case ABSTRACT:
			return Modifier.isAbstract(access);

		case IMPORTS:
			for (String imp : imports) {
				if (instr.matches(imp.replace('.', '/')))
					return !instr.isNegated();
			}
		}

		if (zuper == null)
			return false;

		Clazz clazz = analyzer.findClass(zuper + ".class");
		if (clazz == null)
			return false;

		return clazz.is(query, instr, analyzer);
	}

	public String toString() {
		return getFQN();
	}

	public String getFQN() {
		String s = getClassName().replace('/', '.');
		return s;
	}

	/**
	 * Return a list of packages implemented by this class.
	 * 
	 * @param implemented
	 * @param classspace
	 * @param clazz
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation") final static String	USEPOLICY		= toDescriptor(UsePolicy.class);
	final static String										PROVIDERPOLICY	= toDescriptor(ProviderType.class);

	public static void getImplementedPackages(Set<String> implemented, Analyzer analyzer,
			Clazz clazz) throws Exception {
		if (clazz.interfaces != null) {
			for (String interf : clazz.interfaces) {
				interf = interf + ".class";
				Clazz c = analyzer.getClassspace().get(interf);

				// If not found, actually parse the imported
				// class file to check for implementation policy.
				if (c == null)
					c = analyzer.findClass(interf);

				if (c != null) {
					boolean consumer = false;
					Set<String> annotations = c.annotations;
					if (annotations != null)
						// Override if we marked the interface as a consumer
						// interface
						consumer = annotations.contains(USEPOLICY)
								|| annotations.contains(PROVIDERPOLICY);

					if (!consumer)
						implemented.add(getPackage(interf));
					getImplementedPackages(implemented, analyzer, c);
				} else
					implemented.add(getPackage(interf));

			}
		}
		if (clazz.zuper != null) {
			Clazz c = analyzer.getClassspace().get(clazz.zuper);
			if (c != null) {
				getImplementedPackages(implemented, analyzer, c);
			}
		}

	}

	// String RNAME = "LaQute/bnd/annotation/UsePolicy;";

	public static String toDescriptor(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		sb.append('L');
		sb.append(clazz.getName().replace('.', '/'));
		sb.append(';');
		return sb.toString();
	}

	MethodDef getMethodDef(int access, int methodRefPoolIndex) {
		Object o = pool[methodRefPoolIndex];
		if (o != null && o instanceof Assoc) {
			Assoc assoc = (Assoc) o;
			if (assoc.tag == 10) {
				int string_index = intPool[assoc.a];
				String className = (String) pool[string_index];
				int name_and_type_index = assoc.b;
				Assoc name_and_type = (Assoc) pool[name_and_type_index];
				if (name_and_type.tag == 12) {
					// Name and Type
					int name_index = name_and_type.a;
					int type_index = name_and_type.b;
					String method = (String) pool[name_index];
					String descriptor = (String) pool[type_index];
					return new MethodDef(access, className, method, descriptor);
				} else
					throw new IllegalArgumentException(
							"Invalid class file (or parsing is wrong), assoc is not type + name (12)");
			} else
				throw new IllegalArgumentException(
						"Invalid class file (or parsing is wrong), Assoc is not method ref! (10)");
		} else
			throw new IllegalArgumentException(
					"Invalid class file (or parsing is wrong), Not an assoc at a method ref");
	}

	public static String getShortName(String cname) {
		int n = cname.lastIndexOf('.');
		if (n < 0)
			return cname;
		return cname.substring(n + 1, cname.length());
	}

	public static String fqnToPath(String dotted) {
		return dotted.replace('.', '/') + ".class";
	}

	public static String fqnToBinary(String dotted) {
		return "L" + dotted.replace('.', '/') + ";";
	}

	public static String pathToFqn(String path) {
		return path.replace('/', '.').substring(0, path.length() - 6);
	}

	public boolean isPublic() {
		return Modifier.isPublic(access);
	}

	public boolean isProtected() {
		return Modifier.isProtected(access);
	}

	public boolean isEnum() {
		return zuper != null && zuper.equals("java/lang/Enum");
	}

	public JAVA getFormat() {
		return JAVA.format(major);

	}

	public static String objectDescriptorToFQN(String string) {
		if (string.startsWith("L") && string.endsWith(";"))
			return string.substring(1, string.length() - 1).replace('/', '.');

		switch (string.charAt(0)) {
		case 'V':
			return "void";
		case 'B':
			return "byte";
		case 'C':
			return "char";
		case 'I':
			return "int";
		case 'S':
			return "short";
		case 'D':
			return "double";
		case 'F':
			return "float";
		case 'J':
			return "long";
		case 'Z':
			return "boolean";
		case '[': // Array
			return objectDescriptorToFQN(string.substring(1)) + "[]";
		}
		throw new IllegalArgumentException("Invalid type character in descriptor " + string);
	}

	public static String internalToFqn(String string) {
		return string.replace('/', '.');
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

	public String getPackage() {
		String fqn = getFQN();
		int n = fqn.lastIndexOf('.');
		return fqn.substring(0, n == -1 ? 0 : n);
	}

	public boolean isInterface() {
		return Modifier.isInterface(access);
	}

	public static String binaryToFQN(String name) {
		return name.replace('/', '.');
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(access);
	}

	static Pattern	TYPE_PATTERN	= Pattern.compile("(\\[*)(L[^;]+;|V|B|C|I|S|D|F|J|Z)");

	public static String descriptorToJava(String name, String descriptor) {
		if ( descriptor == null)
			return name;

		StringBuilder sb = new StringBuilder();
		int type = sb.length();
		sb.append(" "); // will be prefixed with type
		sb.append(name);
		Matcher matcher = TYPE_PATTERN.matcher(descriptor);
		int index = 0;
		
		if (descriptor.charAt(index) == '(') {
			index++; // skip (
			sb.append('(');
			String del = "";
			while (descriptor.charAt(index) != ')') {
				sb.append(del);
				matcher.find(index);
				sb.append(objectDescriptorToJava(sb, descriptor, matcher.start(), matcher.end()));
				index = matcher.end();
				del = ",";
			}
			sb.append(')');
			index++; // skip ')'
		}
		boolean end = matcher.find(index);
		if ( end ) {
			sb.insert(type, objectDescriptorToJava(sb,  descriptor, matcher.start(),matcher.end()));
		} else
			System.out.println("No end");
		return sb.toString();
	}

	public static String objectDescriptorToJava(StringBuilder sb, String string, int start,int end) {

		switch (string.charAt(start)) {
		case 'L':
			int last = end-1;
			return string.substring(start+1, last).replace('/', '.');

		case 'V':
			return "void";
		case 'B':
			return "byte";
		case 'C':
			return "char";
		case 'I':
			return "int";
		case 'S':
			return "short";
		case 'D':
			return "double";
		case 'F':
			return "float";
		case 'J':
			return "long";
		case 'Z':
			return "boolean";
		case '[': // Array
			return objectDescriptorToJava(sb, string, start +1, end) + "[]";
		}
		throw new IllegalArgumentException("Invalid type character in descriptor " + string);
	}

	public int getAccess() {
		return access;
	}

}
