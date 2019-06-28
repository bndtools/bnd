package aQute.libg.classdump;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;

import aQute.lib.io.IO;

public class ClassDumper {
	/**
	 * <pre>
	 *  ACC_PUBLIC 0x0001 Declared public; may be accessed from outside its
	 * package. ACC_FINAL 0x0010 Declared final; no subclasses allowed.
	 * ACC_SUPER 0x0020 Treat superclass methods specially when invoked by the
	 * invokespecial instruction. ACC_INTERFACE 0x0200 Is an interface, not a
	 * class. ACC_ABSTRACT 0x0400 Declared abstract; may not be instantiated.
	 * </pre>
	 */
	final static int	ACC_PUBLIC		= 0x0001;	// Declared public; may be
													// accessed
													// from outside its package.
	final static int	ACC_FINAL		= 0x0010;	// Declared final; no
													// subclasses
	// allowed.
	final static int	ACC_SUPER		= 0x0020;	// Treat superclass methods
	// specially when invoked by the
	// invokespecial instruction.
	final static int	ACC_INTERFACE	= 0x0200;	// Is an interface, not a
													// classs
	final static int	ACC_ABSTRACT	= 0x0400;	// Declared abstract; may
													// not be
													// instantiated.

	final static class Assoc {
		Assoc(byte tag, int a, int b) {
			this.tag = tag;
			this.a = a;
			this.b = b;
		}

		byte	tag;
		int		a;
		int		b;

	}

	final String		path;
	final static String	NUM_COLUMN	= "%-30s %d%n";
	final static String	HEX_COLUMN	= "%-30s %x%n";
	final static String	STR_COLUMN	= "%-30s %s%n";

	PrintStream			ps			= System.err;
	Object[]			pool;
	InputStream			in;

	public ClassDumper(String path) throws Exception {
		this(path, IO.stream(Paths.get(path)));
	}

	public ClassDumper(String path, InputStream in) throws IOException {
		this.path = path;
		this.in = in;
	}

	public void dump(PrintStream ps) throws Exception {
		if (ps != null)
			this.ps = ps;
		DataInputStream din = new DataInputStream(in);
		parseClassFile(din);
		din.close();
	}

	void parseClassFile(DataInputStream in) throws IOException {
		int magic = in.readInt();
		if (magic != 0xCAFEBABE)
			throw new IOException("Not a valid class file (no CAFEBABE header)");

		ps.printf(HEX_COLUMN, "magic", magic);
		int minor = in.readUnsignedShort(); // minor version
		int major = in.readUnsignedShort(); // major version
		ps.printf(STR_COLUMN, "version", "" + major + "." + minor);
		int pool_size = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, "pool size", pool_size);
		pool = new Object[pool_size];

		process: for (int poolIndex = 1; poolIndex < pool_size; poolIndex++) {
			byte tag = in.readByte();

			switch (tag) {
				case 0 :
					ps.printf("%30d tag (0)%n", poolIndex);
					break process;

				case 1 :
					String name = in.readUTF();
					pool[poolIndex] = name;
					ps.printf("%30d tag(1) utf8 '%s'%n", poolIndex, name);
					break;

				case 2 :
					throw new IOException("Invalid tag " + tag);

				case 3 :
					int i = in.readInt();
					pool[poolIndex] = Integer.valueOf(i);
					ps.printf("%30d tag(3) int %s%n", poolIndex, i);
					break;

				case 4 :
					float f = in.readFloat();
					pool[poolIndex] = Float.valueOf(f);
					ps.printf("%30d tag(4) float %s%n", poolIndex, f);
					break;

				// For some insane optimization reason are
				// the long and the double two entries in the
				// constant pool. See 4.4.5
				case 5 :
					long l = in.readLong();
					pool[poolIndex] = Long.valueOf(l);
					ps.printf("%30d tag(5) long %s%n", poolIndex, l);
					poolIndex++;
					break;

				case 6 :
					double d = in.readDouble();
					pool[poolIndex] = Double.valueOf(d);
					ps.printf("%30d tag(6) double %s%n", poolIndex, d);
					poolIndex++;
					break;

				case 7 :
					int class_index = in.readUnsignedShort();
					pool[poolIndex] = Integer.valueOf(class_index);
					ps.printf("%30d tag(7) constant classs %d%n", poolIndex, class_index);
					break;

				case 8 :
					int string_index = in.readUnsignedShort();
					pool[poolIndex] = Integer.valueOf(string_index);
					ps.printf("%30d tag(8) constant string %d%n", poolIndex, string_index);
					break;

				case 9 : // Field ref
					class_index = in.readUnsignedShort();
					int name_and_type_index = in.readUnsignedShort();
					pool[poolIndex] = new Assoc((byte) 9, class_index, name_and_type_index);
					ps.printf("%30d tag(9) field ref %d/%d%n", poolIndex, class_index, name_and_type_index);
					break;

				case 10 : // Method ref
					class_index = in.readUnsignedShort();
					name_and_type_index = in.readUnsignedShort();
					pool[poolIndex] = new Assoc((byte) 10, class_index, name_and_type_index);
					ps.printf("%30d tag(10) method ref %d/%d%n", poolIndex, class_index, name_and_type_index);
					break;

				case 11 : // Interface and Method ref
					class_index = in.readUnsignedShort();
					name_and_type_index = in.readUnsignedShort();
					pool[poolIndex] = new Assoc((byte) 11, class_index, name_and_type_index);
					ps.printf("%30d tag(11) interface and method ref %d/%d%n", poolIndex, class_index,
						name_and_type_index);
					break;

				// Name and Type
				case 12 :
					int name_index = in.readUnsignedShort();
					int descriptor_index = in.readUnsignedShort();
					pool[poolIndex] = new Assoc(tag, name_index, descriptor_index);
					ps.printf("%30d tag(12) name and type %d/%d%n", poolIndex, name_index, descriptor_index);
					break;

				default :
					throw new IllegalArgumentException("Unknown tag: " + tag);
			}
		}

		int access = in.readUnsignedShort(); // access
		printAccess(access);
		int this_class = in.readUnsignedShort();
		int super_class = in.readUnsignedShort();
		ps.printf("%-30s %x %s(#%d)%n", "this_class", access, pool[this_class], this_class);
		ps.printf("%-30s %s(#%d)%n", "super_class", pool[super_class], super_class);

		int interfaces_count = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, "interface count", interfaces_count);
		for (int i = 0; i < interfaces_count; i++) {
			int interface_index = in.readUnsignedShort();
			ps.printf("%-30s interface %s(#%d)", "interface count", pool[interface_index], interfaces_count);
		}

		int field_count = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, "field count", field_count);
		for (int i = 0; i < field_count; i++) {
			access = in.readUnsignedShort(); // access
			printAccess(access);
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			ps.printf("%-30s %x %s(#%d) %s(#%d)%n", "field def", access, pool[name_index], name_index,
				pool[descriptor_index], descriptor_index);
			doAttributes(in, "  ");
		}

		int method_count = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, "method count", method_count);
		for (int i = 0; i < method_count; i++) {
			int access_flags = in.readUnsignedShort();
			printAccess(access_flags);
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			ps.printf("%-30s %x %s(#%d) %s(#%d)%n", "method def", access_flags, pool[name_index], name_index,
				pool[descriptor_index], descriptor_index);
			doAttributes(in, "  ");
		}

		doAttributes(in, "");
		if (in.read() >= 0)
			ps.printf("Extra bytes follow ...");
	}

	/**
	 * Called for each attribute in the class, field, or method.
	 *
	 * @param in The stream
	 * @throws IOException
	 */
	private void doAttributes(DataInputStream in, String indent) throws IOException {
		int attribute_count = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "attribute count", attribute_count);
		for (int j = 0; j < attribute_count; j++) {
			doAttribute(in, indent + j + ": ");
		}
	}

	/**
	 * Process a single attribute, if not recognized, skip it.
	 *
	 * @param in the data stream
	 * @throws IOException
	 */
	private void doAttribute(DataInputStream in, String indent) throws IOException {
		int attribute_name_index = in.readUnsignedShort();
		long attribute_length = in.readInt();
		attribute_length &= 0xFFFF;
		String attributeName = (String) pool[attribute_name_index];
		ps.printf("%-30s %s(#%d)%n", indent + "attribute", attributeName, attribute_name_index);
		if ("RuntimeVisibleAnnotations".equals(attributeName))
			doAnnotations(in, indent);
		else if ("SourceFile".equals(attributeName))
			doSourceFile(in, indent);
		else if ("Code".equals(attributeName))
			doCode(in, indent);
		else if ("LineNumberTable".equals(attributeName))
			doLineNumberTable(in, indent);
		else if ("LocalVariableTable".equals(attributeName))
			doLocalVariableTable(in, indent);
		else if ("InnerClasses".equals(attributeName))
			doInnerClasses(in, indent);
		else if ("Exceptions".equals(attributeName))
			doExceptions(in, indent);
		else if ("EnclosingMethod".equals(attributeName))
			doEnclosingMethod(in, indent);
		else if ("Signature".equals(attributeName))
			doSignature(in, indent);
		else if ("Synthetic".equals(attributeName))
			; // Is empty!
		else if ("Deprecated".equals(attributeName))
			; // Is Empty
		else {
			ps.printf("%-30s %d%n", indent + "Unknown attribute, skipping", attribute_length);
			if (attribute_length > 0x7FFFFFFF) {
				throw new IllegalArgumentException("Attribute > 2Gb");
			}
			byte buffer[] = new byte[(int) attribute_length];
			in.readFully(buffer);
			printHex(buffer);
		}
	}

	/**
	 * <pre>
	 *  Signature_attribute { u2 attribute_name_index; u4 attribute_length;
	 * u2 signature_index; }
	 * </pre>
	 *
	 * @param in
	 * @param indent
	 */
	void doSignature(DataInputStream in, String indent) throws IOException {
		int signature_index = in.readUnsignedShort();
		ps.printf("%-30s %s(#%d)%n", indent + "signature", pool[signature_index], signature_index);
	}

	/**
	 * <pre>
	 *  EnclosingMethod_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 class_index u2 method_index; }
	 * </pre>
	 */
	void doEnclosingMethod(DataInputStream in, String indent) throws IOException {
		int class_index = in.readUnsignedShort();
		int method_index = in.readUnsignedShort();
		ps.printf("%-30s %s(#%d/c) %s%n", //
			indent + "enclosing method", //
			pool[((Integer) pool[class_index]).intValue()], //
			class_index, //
			(method_index == 0 ? "<>" : pool[method_index]));
	}

	/**
	 * <pre>
	 *  Exceptions_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 number_of_exceptions; u2
	 * exception_index_table[number_of_exceptions]; }
	 * </pre>
	 *
	 * @param in
	 * @param indent
	 */
	private void doExceptions(DataInputStream in, String indent) throws IOException {
		int number_of_exceptions = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "number of exceptions", number_of_exceptions);
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < number_of_exceptions; i++) {
			int exception_index_table = in.readUnsignedShort();
			sb.append(del);
			sb.append(pool[((Integer) pool[exception_index_table])]);
			sb.append("(#");
			sb.append(exception_index_table);
			sb.append("/c)");
			del = ", ";
		}
		ps.printf("%-30s %d: %s%n", indent + "exceptions", number_of_exceptions, sb);
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
	 * @throws IOException
	 */
	private void doCode(DataInputStream in, String indent) throws IOException {
		int max_stack = in.readUnsignedShort();
		int max_locals = in.readUnsignedShort();
		int code_length = in.readInt();
		ps.printf(NUM_COLUMN, indent + "max_stack", max_stack);
		ps.printf(NUM_COLUMN, indent + "max_locals", max_locals);
		ps.printf(NUM_COLUMN, indent + "code_length", code_length);
		byte code[] = new byte[code_length];
		in.readFully(code);
		printHex(code);
		int exception_table_length = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "exception_table_length", exception_table_length);

		for (int i = 0; i < exception_table_length; i++) {
			int start_pc = in.readUnsignedShort();
			int end_pc = in.readUnsignedShort();
			int handler_pc = in.readUnsignedShort();
			int catch_type = in.readUnsignedShort();
			ps.printf("%-30s %d/%d/%d/%d%n", indent + "exception_table", start_pc, end_pc, handler_pc, catch_type);
		}
		doAttributes(in, indent + "  ");
	}

	/**
	 * We must find Class.forName references ...
	 *
	 * @param code
	 */
	protected void printHex(byte[] code) {
		int index = 0;
		while (index < code.length) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 16 && index < code.length; i++) {
				String s = Integer.toHexString((0xFF & code[index++]))
					.toUpperCase();
				if (s.length() == 1)
					sb.append("0");
				sb.append(s);
				sb.append(" ");
			}
			ps.printf(STR_COLUMN, "", sb.toString());
		}
	}

	private void doSourceFile(DataInputStream in, String indent) throws IOException {
		int sourcefile_index = in.readUnsignedShort();
		ps.printf("%-30s %s(#%d)%n", indent + "Source file", pool[sourcefile_index], sourcefile_index);
	}

	private void doAnnotations(DataInputStream in, String indent) throws IOException {
		int num_annotations = in.readUnsignedShort(); // # of annotations
		ps.printf(NUM_COLUMN, indent + "Number of annotations", num_annotations);
		for (int a = 0; a < num_annotations; a++) {
			doAnnotation(in, indent);
		}
	}

	private void doAnnotation(DataInputStream in, String indent) throws IOException {
		int type_index = in.readUnsignedShort();
		ps.printf("%-30s %s(#%d)", indent + "type", pool[type_index], type_index);
		int num_element_value_pairs = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "num_element_value_pairs", num_element_value_pairs);
		for (int v = 0; v < num_element_value_pairs; v++) {
			int element_name_index = in.readUnsignedShort();
			ps.printf(NUM_COLUMN, indent + "element_name_index", element_name_index);
			doElementValue(in, indent);
		}
	}

	private void doElementValue(DataInputStream in, String indent) throws IOException {
		int tag = in.readUnsignedByte();
		switch (tag) {
			case 'B' :
			case 'C' :
			case 'D' :
			case 'F' :
			case 'I' :
			case 'J' :
			case 'S' :
			case 'Z' :
			case 's' :
				int const_value_index = in.readUnsignedShort();
				ps.printf("%-30s %c %s(#%d)%n", indent + "element value", tag, pool[const_value_index],
					const_value_index);
				break;

			case 'e' :
				int type_name_index = in.readUnsignedShort();
				int const_name_index = in.readUnsignedShort();
				ps.printf("%-30s %c %s(#%d) %s(#%d)%n", indent + "type+const", tag, pool[type_name_index],
					type_name_index, pool[const_name_index], const_name_index);
				break;

			case 'c' :
				int class_info_index = in.readUnsignedShort();
				ps.printf("%-30s %c %s(#%d)%n", indent + "element value", tag, pool[class_info_index],
					class_info_index);
				break;

			case '@' :
				ps.printf("%-30s %c%n", indent + "sub annotation", tag);
				doAnnotation(in, indent);
				break;

			case '[' :
				int num_values = in.readUnsignedShort();
				ps.printf("%-30s %c num_values=%d%n", indent + "sub element value", tag, num_values);
				for (int i = 0; i < num_values; i++) {
					doElementValue(in, indent);
				}
				break;

			default :
				throw new IllegalArgumentException("Invalid value for Annotation ElementValue tag " + tag);
		}
	}

	/**
	 * <pre>
	 *  LineNumberTable_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 line_number_table_length; { u2 start_pc; u2
	 * line_number; } line_number_table[line_number_table_length]; }
	 * </pre>
	 */
	void doLineNumberTable(DataInputStream in, String indent) throws IOException {
		int line_number_table_length = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "line number table length", line_number_table_length);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < line_number_table_length; i++) {
			int start_pc = in.readUnsignedShort();
			int line_number = in.readUnsignedShort();
			sb.append(start_pc);
			sb.append("/");
			sb.append(line_number);
			sb.append(" ");
		}
		ps.printf("%-30s %d: %s%n", indent + "line number table", line_number_table_length, sb);
	}

	/**
	 * <pre>
	 *  LocalVariableTable_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 local_variable_table_length; { u2 start_pc; u2
	 * length; u2 name_index; u2 descriptor_index; u2 index; }
	 * local_variable_table[local_variable_table_length]; }
	 * </pre>
	 */

	void doLocalVariableTable(DataInputStream in, String indent) throws IOException {
		int local_variable_table_length = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "local variable table length", local_variable_table_length);
		for (int i = 0; i < local_variable_table_length; i++) {
			int start_pc = in.readUnsignedShort();
			int length = in.readUnsignedShort();
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			int index = in.readUnsignedShort();
			ps.printf("%-30s %d: %d/%d %s(#%d) %s(#%d)%n", indent, index, start_pc, length, pool[name_index],
				name_index, pool[descriptor_index], descriptor_index);
		}
	}

	/**
	 * <pre>
	 *  InnerClasses_attribute { u2 attribute_name_index; u4
	 * attribute_length; u2 number_of_classes; { u2 inner_class_info_index; u2
	 * outer_class_info_index; u2 inner_name_index; u2 inner_class_access_flags;
	 * } classes[number_of_classes]; }
	 * </pre>
	 */
	void doInnerClasses(DataInputStream in, String indent) throws IOException {
		int number_of_classes = in.readUnsignedShort();
		ps.printf(NUM_COLUMN, indent + "number of classes", number_of_classes);
		for (int i = 0; i < number_of_classes; i++) {
			int inner_class_info_index = in.readUnsignedShort();
			int outer_class_info_index = in.readUnsignedShort();
			int inner_name_index = in.readUnsignedShort();
			int inner_class_access_flags = in.readUnsignedShort();
			printAccess(inner_class_access_flags);

			String iname = "<>";
			String oname = iname;

			if (inner_class_info_index != 0)
				iname = (String) pool[((Integer) pool[inner_class_info_index]).intValue()];
			if (outer_class_info_index != 0)
				oname = (String) pool[((Integer) pool[outer_class_info_index]).intValue()];

			ps.printf("%-30s %d: %x %s(#%d/c) %s(#%d/c) %s(#%d) %n", indent, i, inner_class_access_flags, iname,
				inner_class_info_index, oname, outer_class_info_index, pool[inner_name_index], inner_name_index);
		}
	}

	void printClassAccess(int mod) {
		ps.printf("%-30s", "Class Access");
		if ((ACC_PUBLIC & mod) != 0)
			ps.print(" public");
		if ((ACC_FINAL & mod) != 0)
			ps.print(" final");
		if ((ACC_SUPER & mod) != 0)
			ps.print(" super");
		if ((ACC_INTERFACE & mod) != 0)
			ps.print(" interface");
		if ((ACC_ABSTRACT & mod) != 0)
			ps.print(" abstract");

		ps.println();
	}

	void printAccess(int mod) {
		ps.printf("%-30s", "Access");
		if (Modifier.isStatic(mod))
			ps.print(" static");
		if (Modifier.isAbstract(mod))
			ps.print(" abstract");
		if (Modifier.isPublic(mod))
			ps.print(" public");
		if (Modifier.isFinal(mod))
			ps.print(" final");
		if (Modifier.isInterface(mod))
			ps.print(" interface");
		if (Modifier.isNative(mod))
			ps.print(" native");
		if (Modifier.isPrivate(mod))
			ps.print(" private");
		if (Modifier.isProtected(mod))
			ps.print(" protected");
		if (Modifier.isStrict(mod))
			ps.print(" strict");
		if (Modifier.isSynchronized(mod))
			ps.print(" synchronized");
		if (Modifier.isTransient(mod))
			ps.print(" transient");
		if (Modifier.isVolatile(mod))
			ps.print(" volatile");

		ps.println();
	}

	public static void main(String args[]) throws Exception {
		if (args.length == 0) {
			System.err.println("clsd <class file>+");
		}
		for (int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			if (!f.isFile())
				System.err.println("File does not exist or is directory " + f);
			else {
				ClassDumper cd = new ClassDumper(args[i]);
				cd.dump(null);
			}
		}
	}

}
