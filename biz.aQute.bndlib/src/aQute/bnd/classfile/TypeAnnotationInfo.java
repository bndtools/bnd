package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class TypeAnnotationInfo extends AnnotationInfo {
	public static final int	TYPEUSE_INDEX_NONE				= -1;
	public static final int	TYPEUSE_TARGET_INDEX_EXTENDS	= 65535;
	public final int		target_type;
	public final byte[]		target_info;
	public final int		target_index;
	public final byte[]		type_path;

	public TypeAnnotationInfo(int target_type, byte[] target_info, int target_index, byte[] type_path, String type,
		ElementValueInfo[] values) {
		super(type, values);
		this.target_type = target_type;
		this.target_info = target_info;
		this.target_index = target_index;
		this.type_path = type_path;
	}

	@Override
	public String toString() {
		return type + " " + Arrays.toString(values);
	}

	static TypeAnnotationInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
		// Table 4.7.20-A. Interpretation of target_type values (Part 1)
		int target_type = in.readUnsignedByte();
		byte[] target_info;
		int target_index;
		switch (target_type) {
			case 0x00 : // type parameter declaration of generic class or
						// interface
			case 0x01 : // type parameter declaration of generic method or
						// constructor
				//
				// type_parameter_target {
				// u1 type_parameter_index;
				// }
				target_info = new byte[1];
				in.readFully(target_info);
				target_index = Byte.toUnsignedInt(target_info[0]);
				break;

			case 0x10 : // type in extends clause of class or interface
						// declaration (including the direct superclass of
						// an anonymous class declaration), or in implements
						// clause of interface declaration
				// supertype_target {
				// u2 supertype_index;
				// }
				target_info = new byte[2];
				in.readFully(target_info);
				target_index = (Byte.toUnsignedInt(target_info[0]) << 8) | Byte.toUnsignedInt(target_info[1]);
				break;

			case 0x11 : // type in bound of type parameter declaration of
						// generic class or interface
			case 0x12 : // type in bound of type parameter declaration of
						// generic method or constructor
				// type_parameter_bound_target {
				// u1 type_parameter_index;
				// u1 bound_index;
				// }
				target_info = new byte[2];
				in.readFully(target_info);
				target_index = Byte.toUnsignedInt(target_info[0]);
				break;

			case 0x13 : // type in field declaration
			case 0x14 : // return type of method, or type of newly
						// constructed object
			case 0x15 : // receiver type of method or constructor
				target_info = new byte[0];
				target_index = TYPEUSE_INDEX_NONE;
				break;

			case 0x16 : // type in formal parameter declaration of method,
						// constructor, or lambda expression
				// formal_parameter_target {
				// u1 formal_parameter_index;
				// }
				target_info = new byte[1];
				in.readFully(target_info);
				target_index = Byte.toUnsignedInt(target_info[0]);
				break;

			case 0x17 : // type in throws clause of method or constructor
				// throws_target {
				// u2 throws_type_index;
				// }
				target_info = new byte[2];
				in.readFully(target_info);
				target_index = (Byte.toUnsignedInt(target_info[0]) << 8) | Byte.toUnsignedInt(target_info[1]);
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
				target_info = new byte[table_length * 6];
				in.readFully(target_info);
				target_index = TYPEUSE_INDEX_NONE;
				break;

			case 0x42 : // type in exception parameter declaration
				// catch_target {
				// u2 exception_table_index;
				// }
				target_info = new byte[2];
				in.readFully(target_info);
				target_index = (Byte.toUnsignedInt(target_info[0]) << 8) | Byte.toUnsignedInt(target_info[1]);
				break;

			case 0x43 : // type in instanceof expression
			case 0x44 : // type in new expression
			case 0x45 : // type in method reference expression using ::new
			case 0x46 : // type in method reference expression using
						// ::Identifier
				// offset_target {
				// u2 offset;
				// }
				target_info = new byte[2];
				in.readFully(target_info);
				target_index = TYPEUSE_INDEX_NONE;
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
				target_info = new byte[3];
				in.readFully(target_info);
				target_index = Byte.toUnsignedInt(target_info[2]);
				break;
			default :
				throw new IOException("Unknown target_type: " + target_type);
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
		byte[] type_path = new byte[path_length * 2];
		in.readFully(type_path);

		// Rest is identical to the normal annotations
		return read(in, constant_pool,
			(type, values) -> new TypeAnnotationInfo(target_type, target_info, target_index, type_path, type, values));
	}

	@Override
	void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		out.writeByte(target_type);
		switch (target_type) {
			case 0x00 : // type parameter declaration of generic class or
						// interface
			case 0x01 : // type parameter declaration of generic method or
						// constructor
				//
				// type_parameter_target {
				// u1 type_parameter_index;
				// }
				out.write(target_info, 0, 1);
				break;

			case 0x10 : // type in extends clause of class or interface
						// declaration (including the direct superclass of
						// an anonymous class declaration), or in implements
						// clause of interface declaration
				// supertype_target {
				// u2 supertype_index;
				// }
				out.write(target_info, 0, 2);
				break;

			case 0x11 : // type in bound of type parameter declaration of
						// generic class or interface
			case 0x12 : // type in bound of type parameter declaration of
						// generic method or constructor
				// type_parameter_bound_target {
				// u1 type_parameter_index;
				// u1 bound_index;
				// }
				out.write(target_info, 0, 2);
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
				out.write(target_info, 0, 1);
				break;

			case 0x17 : // type in throws clause of method or constructor
				// throws_target {
				// u2 throws_type_index;
				// }
				out.write(target_info, 0, 2);
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
				int table_length = target_info.length / 6;
				out.writeShort(table_length);
				out.write(target_info, 0, target_info.length);
				break;

			case 0x42 : // type in exception parameter declaration
				// catch_target {
				// u2 exception_table_index;
				// }
				out.write(target_info, 0, 2);
				break;

			case 0x43 : // type in instanceof expression
			case 0x44 : // type in new expression
			case 0x45 : // type in method reference expression using ::new
			case 0x46 : // type in method reference expression using
						// ::Identifier
				// offset_target {
				// u2 offset;
				// }
				out.write(target_info, 0, 2);
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
				out.write(target_info, 0, 3);
				break;
			default :
				throw new IOException("Unknown target_type: " + target_type);
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

		int path_length = type_path.length / 2;
		out.writeByte(path_length);
		out.write(type_path, 0, type_path.length);

		super.write(out, constant_pool);
	}

	@Override
	int value_length() {
		int value_length = 1 * Byte.BYTES;
		switch (target_type) {
			case 0x00 : // type parameter declaration of generic class or
						// interface
			case 0x01 : // type parameter declaration of generic method or
						// constructor
				//
				// type_parameter_target {
				// u1 type_parameter_index;
				// }
				value_length += 1 * Byte.BYTES;
				break;

			case 0x10 : // type in extends clause of class or interface
						// declaration (including the direct superclass of
						// an anonymous class declaration), or in implements
						// clause of interface declaration
				// supertype_target {
				// u2 supertype_index;
				// }
				value_length += 1 * Short.BYTES;
				break;

			case 0x11 : // type in bound of type parameter declaration of
						// generic class or interface
			case 0x12 : // type in bound of type parameter declaration of
						// generic method or constructor
				// type_parameter_bound_target {
				// u1 type_parameter_index;
				// u1 bound_index;
				// }
				value_length += 2 * Byte.BYTES;
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
				value_length += 1 * Byte.BYTES;
				break;

			case 0x17 : // type in throws clause of method or constructor
				// throws_target {
				// u2 throws_type_index;
				// }
				value_length += 1 * Short.BYTES;
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
				value_length += 1 * Short.BYTES + target_info.length;
				break;

			case 0x42 : // type in exception parameter declaration
				// catch_target {
				// u2 exception_table_index;
				// }
				value_length += 1 * Short.BYTES;
				break;

			case 0x43 : // type in instanceof expression
			case 0x44 : // type in new expression
			case 0x45 : // type in method reference expression using ::new
			case 0x46 : // type in method reference expression using
						// ::Identifier
				// offset_target {
				// u2 offset;
				// }
				value_length += 1 * Short.BYTES;
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
				value_length += 1 * Short.BYTES + 1 * Byte.BYTES;
				break;
			default :
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

		value_length += 1 * Byte.BYTES + type_path.length;

		value_length += super.value_length();
		return value_length;
	}
}
