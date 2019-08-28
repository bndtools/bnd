package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class BootstrapMethodsAttribute implements Attribute {
	public static final String		NAME	= "BootstrapMethods";
	public final BootstrapMethod[]	bootstrap_methods;

	public BootstrapMethodsAttribute(BootstrapMethod[] bootstrap_methods) {
		this.bootstrap_methods = bootstrap_methods;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(bootstrap_methods);
	}

	public static BootstrapMethodsAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int num_bootstrap_methods = in.readUnsignedShort();
		BootstrapMethod[] bootstrap_methods = new BootstrapMethod[num_bootstrap_methods];
		for (int i = 0; i < num_bootstrap_methods; i++) {
			bootstrap_methods[i] = BootstrapMethod.read(in, constant_pool);
		}
		return new BootstrapMethodsAttribute(bootstrap_methods);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(bootstrap_methods.length);
		for (BootstrapMethod bootstrap_method : bootstrap_methods) {
			bootstrap_method.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (BootstrapMethod bootstrap_method : bootstrap_methods) {
			attribute_length += bootstrap_method.value_length();
		}
		return attribute_length;
	}

	public static class BootstrapMethod {
		public final int	bootstrap_method_ref;
		public final int[]	bootstrap_arguments;

		public BootstrapMethod(int bootstrap_method_ref, int[] bootstrap_arguments) {
			this.bootstrap_method_ref = bootstrap_method_ref;
			this.bootstrap_arguments = bootstrap_arguments;
		}

		@Override
		public String toString() {
			return bootstrap_method_ref + ":" + Arrays.toString(bootstrap_arguments);
		}

		static BootstrapMethod read(DataInput in, ConstantPool constant_pool) throws IOException {
			int bootstrap_method_ref = in.readUnsignedShort();
			int num_bootstrap_arguments = in.readUnsignedShort();
			int[] bootstrap_arguments = new int[num_bootstrap_arguments];
			for (int i = 0; i < num_bootstrap_arguments; i++) {
				bootstrap_arguments[i] = in.readUnsignedShort();
			}
			return new BootstrapMethod(bootstrap_method_ref, bootstrap_arguments);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(bootstrap_method_ref);
			out.writeShort(bootstrap_arguments.length);
			for (int bootstrap_argument : bootstrap_arguments) {
				out.writeShort(bootstrap_argument);
			}
		}

		int value_length() {
			return (2 + bootstrap_arguments.length) * Short.BYTES;
		}
	}
}
