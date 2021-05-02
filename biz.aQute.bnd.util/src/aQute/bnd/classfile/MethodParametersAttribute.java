package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class MethodParametersAttribute implements Attribute {
	public static final String		NAME	= "MethodParameters";
	public final MethodParameter[]	parameters;

	public MethodParametersAttribute(MethodParameter[] parameters) {
		this.parameters = parameters;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(parameters);
	}

	public static MethodParametersAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int parameters_count = in.readUnsignedByte();
		MethodParameter[] parameters = new MethodParameter[parameters_count];
		for (int i = 0; i < parameters_count; i++) {
			parameters[i] = MethodParameter.read(in, constant_pool);
		}
		return new MethodParametersAttribute(parameters);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeByte(parameters.length);
		for (MethodParameter parameter : parameters) {
			parameter.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Byte.BYTES;
		for (MethodParameter parameter : parameters) {
			attribute_length += parameter.value_length();
		}
		return attribute_length;
	}

	public static class MethodParameter {
		public final String	name;
		public final int	access_flags;

		public MethodParameter(String name, int access_flags) {
			this.name = name;
			this.access_flags = access_flags;
		}

		@Override
		public String toString() {
			return name + ":" + access_flags;
		}

		static MethodParameter read(DataInput in, ConstantPool constant_pool) throws IOException {
			int name_index = in.readUnsignedShort();
			int access_flags = in.readUnsignedShort();
			return new MethodParameter(constant_pool.utf8(name_index), access_flags);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int name_index = constant_pool.utf8Info(name);
			out.writeShort(name_index);
			out.writeShort(access_flags);
		}

		int value_length() {
			return 2 * Short.BYTES;
		}
	}
}
