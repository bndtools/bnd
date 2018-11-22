package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class BootstrapMethodsAttribute implements Attribute {
	public static final String		NAME	= "BootstrapMethods";
	public final BootstrapMethod[]	bootstrap_methods;

	BootstrapMethodsAttribute(BootstrapMethod[] bootstrap_methods) {
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

	static BootstrapMethodsAttribute parseBootstrapMethodsAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		int num_bootstrap_methods = in.readUnsignedShort();
		BootstrapMethod[] bootstrap_methods = new BootstrapMethod[num_bootstrap_methods];
		for (int i = 0; i < num_bootstrap_methods; i++) {
			bootstrap_methods[i] = BootstrapMethod.parseBootstrapMethod(in, constant_pool);
		}
		return new BootstrapMethodsAttribute(bootstrap_methods);
	}

	public static class BootstrapMethod {
		public final int	bootstrap_method_ref;
		public final int[]	bootstrap_arguments;

		BootstrapMethod(int bootstrap_method_ref, int[] bootstrap_arguments) {
			this.bootstrap_method_ref = bootstrap_method_ref;
			this.bootstrap_arguments = bootstrap_arguments;
		}

		@Override
		public String toString() {
			return bootstrap_method_ref + ":" + Arrays.toString(bootstrap_arguments);
		}

		static BootstrapMethod parseBootstrapMethod(DataInput in, ConstantPool constant_pool) throws IOException {
			int bootstrap_method_ref = in.readUnsignedShort();
			int num_bootstrap_arguments = in.readUnsignedShort();
			int[] bootstrap_arguments = new int[num_bootstrap_arguments];
			for (int i = 0; i < num_bootstrap_arguments; i++) {
				bootstrap_arguments[i] = in.readUnsignedShort();
			}
			return new BootstrapMethod(bootstrap_method_ref, bootstrap_arguments);
		}
	}
}
