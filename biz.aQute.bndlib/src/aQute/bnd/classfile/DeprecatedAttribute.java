package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DeprecatedAttribute implements Attribute {
	public static final String NAME = "Deprecated";

	public DeprecatedAttribute() {}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME;
	}

	public static DeprecatedAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		return new DeprecatedAttribute();
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
	}

	@Override
	public int attribute_length() {
		int attribute_length = 0;
		return attribute_length;
	}
}
