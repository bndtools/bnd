package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ElementValueInfo {
	public final String	name;
	public final Object	value;

	public ElementValueInfo(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return name + "=" + toString(value);
	}

	static String toString(Object value) {
		return (value instanceof Object[]) ? Arrays.toString((Object[]) value) : value.toString();
	}

	static ElementValueInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
		int element_name_index = in.readUnsignedShort();
		Object value = readValue(in, constant_pool);
		return new ElementValueInfo(constant_pool.utf8(element_name_index), value);
	}

	void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int element_name_index = constant_pool.utf8Info(name);
		out.writeShort(element_name_index);
		writeValue(out, constant_pool, value);
	}

	int value_length() {
		return 1 * Short.BYTES + value_length(value);
	}

	static Object readValue(DataInput in, ConstantPool constant_pool) throws IOException {
		int tag = in.readUnsignedByte();
		switch (tag) {
			case 'B' : // Byte
			{
				int const_value_index = in.readUnsignedShort();
				Integer const_value = constant_pool.entry(const_value_index);
				return Byte.valueOf(const_value.byteValue());
			}

			case 'C' : // Character
			{
				int const_value_index = in.readUnsignedShort();
				Integer const_value = constant_pool.entry(const_value_index);
				return Character.valueOf((char) const_value.intValue());
			}

			case 'I' : // Integer
			case 'D' : // Double
			case 'F' : // Float
			case 'J' : // Long
			case 's' : // String
			{
				int const_value_index = in.readUnsignedShort();
				return constant_pool.entry(const_value_index);
			}

			case 'S' : // Short
			{
				int const_value_index = in.readUnsignedShort();
				Integer const_value = constant_pool.entry(const_value_index);
				return Short.valueOf(const_value.shortValue());
			}

			case 'Z' : // Boolean
			{
				int const_value_index = in.readUnsignedShort();
				Integer const_value = constant_pool.entry(const_value_index);
				return Boolean.valueOf(const_value.intValue() != 0);
			}

			case 'e' : // enum constant
			{
				return EnumConst.read(in, constant_pool);
			}

			case 'c' : // Class
			{
				return ResultConst.read(in, constant_pool);
			}
			case '@' : // Annotation type
			{
				return AnnotationInfo.read(in, constant_pool);
			}

			case '[' : // Array
			{
				int num_values = in.readUnsignedShort();
				Object[] array_value = new Object[num_values];
				for (int i = 0; i < num_values; i++) {
					array_value[i] = readValue(in, constant_pool);
				}
				return array_value;
			}

			default : {
				throw new IOException("Invalid value for Annotation ElementValue tag " + tag);
			}
		}
	}

	static void writeValue(DataOutput out, ConstantPool constant_pool, Object value) throws IOException {
		if (value instanceof Byte) {
			out.writeByte('B');
			int const_value_index = constant_pool.integerInfo((Byte) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Character) {
			out.writeByte('C');
			int const_value_index = constant_pool.integerInfo((Character) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Double) {
			out.writeByte('D');
			int const_value_index = constant_pool.doubleInfo((Double) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Float) {
			out.writeByte('F');
			int const_value_index = constant_pool.floatInfo((Float) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Integer) {
			out.writeByte('I');
			int const_value_index = constant_pool.integerInfo((Integer) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Long) {
			out.writeByte('J');
			int const_value_index = constant_pool.longInfo((Long) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Short) {
			out.writeByte('S');
			int const_value_index = constant_pool.integerInfo((Short) value);
			out.writeShort(const_value_index);
		} else if (value instanceof Boolean) {
			out.writeByte('Z');
			int const_value_index = constant_pool.integerInfo((Boolean) value);
			out.writeShort(const_value_index);
		} else if (value instanceof String) {
			out.writeByte('s');
			int const_value_index = constant_pool.utf8Info((String) value);
			out.writeShort(const_value_index);
		} else if (value instanceof EnumConst) {
			out.writeByte('e');
			EnumConst enum_const_value = (EnumConst) value;
			enum_const_value.write(out, constant_pool);
		} else if (value instanceof ResultConst) {
			out.writeByte('c');
			ResultConst class_info = (ResultConst) value;
			class_info.write(out, constant_pool);
		} else if (value instanceof AnnotationInfo) {
			out.writeByte('@');
			AnnotationInfo annotation_value = (AnnotationInfo) value;
			annotation_value.write(out, constant_pool);
		} else if (value instanceof Object[]) {
			out.writeByte('[');
			Object[] array_value = (Object[]) value;
			out.writeShort(array_value.length);
			for (Object const_value : array_value) {
				writeValue(out, constant_pool, const_value);
			}
		} else {
			throw new IOException("Unknown value type for ElementValueInfo value " + value);
		}
	}

	static int value_length(Object value) {
		int value_length = 1 * Byte.BYTES;

		if (value instanceof Byte) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Character) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Double) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Float) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Integer) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Long) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Short) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof Boolean) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof String) {
			value_length += 1 * Short.BYTES;
		} else if (value instanceof EnumConst) {
			EnumConst enum_const_value = (EnumConst) value;
			value_length += enum_const_value.value_length();
		} else if (value instanceof ResultConst) {
			ResultConst class_info = (ResultConst) value;
			value_length += class_info.value_length();
		} else if (value instanceof AnnotationInfo) {
			AnnotationInfo annotation_value = (AnnotationInfo) value;
			value_length += annotation_value.value_length();
		} else if (value instanceof Object[]) {
			Object[] array_value = (Object[]) value;
			value_length += 1 * Short.BYTES;
			for (Object const_value : array_value) {
				value_length += value_length(const_value);
			}
		}
		return value_length;
	}

	public static class EnumConst {
		public final String	type;
		public final String	name;

		public EnumConst(String type, String name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public String toString() {
			return type + "." + name;
		}

		static EnumConst read(DataInput in, ConstantPool constant_pool) throws IOException {
			int type_name_index = in.readUnsignedShort();
			int const_name_index = in.readUnsignedShort();
			return new EnumConst(constant_pool.utf8(type_name_index), constant_pool.utf8(const_name_index));
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int type_name_index = constant_pool.utf8Info(type);
			int const_name_index = constant_pool.utf8Info(name);
			out.writeShort(type_name_index);
			out.writeShort(const_name_index);
		}

		int value_length() {
			return 2 * Short.BYTES;
		}
	}

	public static class ResultConst {
		public final String descriptor;

		public ResultConst(String descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public String toString() {
			return descriptor;
		}

		static ResultConst read(DataInput in, ConstantPool constant_pool) throws IOException {
			int class_index = in.readUnsignedShort();
			return new ResultConst(constant_pool.utf8(class_index));
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int class_index = constant_pool.utf8Info(descriptor);
			out.writeShort(class_index);
		}

		int value_length() {
			return 1 * Short.BYTES;
		}
	}
}
