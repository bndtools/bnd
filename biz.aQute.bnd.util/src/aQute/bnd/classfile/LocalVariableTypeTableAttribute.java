package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LocalVariableTypeTableAttribute implements Attribute {
	public static final String			NAME	= "LocalVariableTypeTable";
	public final LocalVariableType[]	local_variable_type_table;

	public LocalVariableTypeTableAttribute(LocalVariableType[] local_variable_type_table) {
		this.local_variable_type_table = local_variable_type_table;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(local_variable_type_table);
	}

	public static LocalVariableTypeTableAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int local_variable_type_table_length = in.readUnsignedShort();
		LocalVariableType[] local_variable_type_table = new LocalVariableType[local_variable_type_table_length];
		for (int i = 0; i < local_variable_type_table_length; i++) {
			local_variable_type_table[i] = LocalVariableType.read(in, constant_pool);
		}
		return new LocalVariableTypeTableAttribute(local_variable_type_table);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(local_variable_type_table.length);
		for (LocalVariableType local_variable_type : local_variable_type_table) {
			local_variable_type.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (LocalVariableType local_variable_type : local_variable_type_table) {
			attribute_length += local_variable_type.value_length();
		}
		return attribute_length;
	}

	public static class LocalVariableType {
		public final int	start_pc;
		public final int	length;
		public final String	name;
		public final String	signature;
		public final int	index;

		public LocalVariableType(int start_pc, int length, String name, String signature, int index) {
			this.start_pc = start_pc;
			this.length = length;
			this.name = name;
			this.signature = signature;
			this.index = index;
		}

		@Override
		public String toString() {
			return start_pc + ":" + length + ":" + name + ":" + signature + ":" + index;
		}

		static LocalVariableType read(DataInput in, ConstantPool constant_pool) throws IOException {
			int start_pc = in.readUnsignedShort();
			int length = in.readUnsignedShort();
			int name_index = in.readUnsignedShort();
			int signature_index = in.readUnsignedShort();
			int index = in.readUnsignedShort();
			return new LocalVariableType(start_pc, length, constant_pool.utf8(name_index),
				constant_pool.utf8(signature_index), index);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int name_index = constant_pool.utf8Info(name);
			int signature_index = constant_pool.utf8Info(signature);
			out.writeShort(start_pc);
			out.writeShort(length);
			out.writeShort(name_index);
			out.writeShort(signature_index);
			out.writeShort(index);
		}

		int value_length() {
			return 5 * Short.BYTES;
		}
	}
}
