package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ExceptionsAttribute implements Attribute {
	public static final String	NAME	= "Exceptions";
	public final String[]		exceptions;

	public ExceptionsAttribute(String[] exceptions) {
		this.exceptions = exceptions;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(exceptions);
	}

	public static ExceptionsAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_exceptions = in.readUnsignedShort();
		String[] exceptions = new String[number_of_exceptions];
		for (int i = 0; i < number_of_exceptions; i++) {
			int exception_index = in.readUnsignedShort();
			exceptions[i] = constant_pool.className(exception_index);
		}
		return new ExceptionsAttribute(exceptions);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(exceptions.length);
		for (String exception : exceptions) {
			int exception_index = constant_pool.classInfo(exception);
			out.writeShort(exception_index);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = (1 + exceptions.length) * Short.BYTES;
		return attribute_length;
	}
}
