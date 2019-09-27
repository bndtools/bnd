package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LineNumberTableAttribute implements Attribute {
	public static final String	NAME	= "LineNumberTable";
	public final LineNumber[]	line_number_table;

	public LineNumberTableAttribute(LineNumber[] line_number_table) {
		this.line_number_table = line_number_table;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(line_number_table);
	}

	public static LineNumberTableAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int line_number_table_length = in.readUnsignedShort();
		LineNumber[] line_number_table = new LineNumber[line_number_table_length];
		for (int i = 0; i < line_number_table_length; i++) {
			line_number_table[i] = LineNumber.read(in, constant_pool);
		}
		return new LineNumberTableAttribute(line_number_table);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(line_number_table.length);
		for (LineNumber line_number : line_number_table) {
			line_number.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (LineNumber line_number : line_number_table) {
			attribute_length += line_number.value_length();
		}
		return attribute_length;
	}

	public static class LineNumber {
		public final int	start_pc;
		public final int	line_number;

		public LineNumber(int start_pc, int line_number) {
			this.start_pc = start_pc;
			this.line_number = line_number;
		}

		@Override
		public String toString() {
			return start_pc + ":" + line_number;
		}

		static LineNumber read(DataInput in, ConstantPool constant_pool) throws IOException {
			int start_pc = in.readUnsignedShort();
			int line_number = in.readUnsignedShort();
			return new LineNumber(start_pc, line_number);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(start_pc);
			out.writeShort(line_number);
		}

		int value_length() {
			return 2 * Short.BYTES;
		}
	}
}
