package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ExceptionsAttribute implements Attribute {
	public static final String	NAME	= "Exceptions";
	public final String[]		exceptions;

	ExceptionsAttribute(String[] exceptions) {
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

	static ExceptionsAttribute parseExceptionsAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_exceptions = in.readUnsignedShort();
		String[] exceptions = new String[number_of_exceptions];
		for (int i = 0; i < number_of_exceptions; i++) {
			int exception_index = in.readUnsignedShort();
			exceptions[i] = constant_pool.className(exception_index);
		}
		return new ExceptionsAttribute(exceptions);
	}
}
