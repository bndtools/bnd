package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

import aQute.bnd.classfile.ConstantPool.StringInfo;

public class ConstantValueAttribute implements Attribute {
	public static final String	NAME	= "ConstantValue";
	public final Object			value;

	ConstantValueAttribute(Object value) {
		this.value = value;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + "=" + value;
	}

	static ConstantValueAttribute parseConstantValueAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		int constantvalue_index = in.readUnsignedShort();
		Object value = constant_pool.entry(constantvalue_index);
		if (value instanceof StringInfo) {
			value = constant_pool.string(constantvalue_index);
		}
		return new ConstantValueAttribute(value);
	}
}
