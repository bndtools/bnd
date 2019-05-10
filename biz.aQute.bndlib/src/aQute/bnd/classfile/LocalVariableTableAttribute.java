package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class LocalVariableTableAttribute implements Attribute {
	public static final String		NAME	= "LocalVariableTable";
	public final LocalVariable[]	local_variable_table;

	public LocalVariableTableAttribute(LocalVariable[] local_variable_table) {
		this.local_variable_table = local_variable_table;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(local_variable_table);
	}

	static LocalVariableTableAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int local_variable_table_length = in.readUnsignedShort();
		LocalVariable[] local_variable_table = new LocalVariable[local_variable_table_length];
		for (int i = 0; i < local_variable_table_length; i++) {
			local_variable_table[i] = LocalVariable.read(in, constant_pool);
		}
		return new LocalVariableTableAttribute(local_variable_table);
	}

	public static class LocalVariable {
		public final int	start_pc;
		public final int	length;
		public final String	name;
		public final String	descriptor;
		public final int	index;

		public LocalVariable(int start_pc, int length, String name, String descriptor, int index) {
			this.start_pc = start_pc;
			this.length = length;
			this.name = name;
			this.descriptor = descriptor;
			this.index = index;
		}

		@Override
		public String toString() {
			return start_pc + ":" + length + ":" + name + ":" + descriptor + ":" + index;
		}

		static LocalVariable read(DataInput in, ConstantPool constant_pool) throws IOException {
			int start_pc = in.readUnsignedShort();
			int length = in.readUnsignedShort();
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			int index = in.readUnsignedShort();
			return new LocalVariable(start_pc, length, constant_pool.utf8(name_index),
				constant_pool.utf8(descriptor_index), index);
		}
	}
}
