package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class MethodParametersAttribute implements Attribute {
	public static final String		NAME	= "MethodParameters";
	public final MethodParameter[]	parameters;

	MethodParametersAttribute(MethodParameter[] parameters) {
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

	static MethodParametersAttribute parseMethodParametersAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		int parameters_count = in.readUnsignedByte();
		MethodParameter[] parameters = new MethodParameter[parameters_count];
		for (int i = 0; i < parameters_count; i++) {
			parameters[i] = MethodParameter.parseMethodParameter(in, constant_pool);
		}
		return new MethodParametersAttribute(parameters);
	}

	public static class MethodParameter {
		public final String	name;
		public final int	access_flags;

		MethodParameter(String name, int access_flags) {
			this.name = name;
			this.access_flags = access_flags;
		}

		@Override
		public String toString() {
			return name + ":" + access_flags;
		}

		static MethodParameter parseMethodParameter(DataInput in, ConstantPool constant_pool) throws IOException {
			int name_index = in.readUnsignedShort();
			int access_flags = in.readUnsignedShort();
			return new MethodParameter(constant_pool.utf8(name_index), access_flags);
		}
	}
}
