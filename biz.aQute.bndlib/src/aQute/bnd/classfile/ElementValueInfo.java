package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ElementValueInfo {
	public final String	name;
	public final Object	value;

	ElementValueInfo(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return name + "=" + ((value instanceof Object[]) ? Arrays.deepToString((Object[]) value) : value);
	}

	static ElementValueInfo parseElementValueInfo(DataInput in, ConstantPool constant_pool) throws IOException {
		int element_name_index = in.readUnsignedShort();
		Object value = parseElementValue(in, constant_pool);
		return new ElementValueInfo(constant_pool.utf8(element_name_index), value);
	}

	static Object parseElementValue(DataInput in, ConstantPool constant_pool) throws IOException {
		int tag = in.readUnsignedByte();
		switch (tag) {
			case 'B' : // Byte
			case 'C' : // Character
			case 'I' : // Integer
			case 'S' : // Short
			case 'D' : // Double
			case 'F' : // Float
			case 'J' : // Long
			case 's' : // String
			{
				int const_value_index = in.readUnsignedShort();
				return constant_pool.entry(const_value_index);
			}

			case 'Z' : // Boolean
			{
				int const_value_index = in.readUnsignedShort();
				Integer const_value = constant_pool.entry(const_value_index);
				return Boolean.valueOf(const_value.intValue() != 0);
			}

			case 'e' : // enum constant
			{
				return EnumConst.parseEnumConst(in, constant_pool);
			}

			case 'c' : // Class
			{
				return ResultConst.parseResultConst(in, constant_pool);
			}
			case '@' : // Annotation type
			{
				return AnnotationInfo.parseAnnotationInfo(in, constant_pool);
			}

			case '[' : // Array
			{
				int num_values = in.readUnsignedShort();
				Object[] array_value = new Object[num_values];
				for (int i = 0; i < num_values; i++) {
					array_value[i] = parseElementValue(in, constant_pool);
				}
				return array_value;
			}

			default : {
				throw new IOException("Invalid value for Annotation ElementValue tag " + tag);
			}
		}
	}

	public static class EnumConst {
		public final String	type;
		public final String	name;

		EnumConst(String type, String name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public String toString() {
			return type + "." + name;
		}

		static EnumConst parseEnumConst(DataInput in, ConstantPool constant_pool) throws IOException {
			int type_name_index = in.readUnsignedShort();
			int const_name_index = in.readUnsignedShort();
			return new EnumConst(constant_pool.utf8(type_name_index), constant_pool.utf8(const_name_index));
		}
	}

	public static class ResultConst {
		public final String descriptor;

		ResultConst(String descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public String toString() {
			return descriptor;
		}

		static ResultConst parseResultConst(DataInput in, ConstantPool constant_pool) throws IOException {
			int class_index = in.readUnsignedShort();
			return new ResultConst(constant_pool.utf8(class_index));
		}
	}
}
