package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import aQute.bnd.classfile.ConstantPool.StringInfo;

public class ConstantValueAttribute implements Attribute {
	public static final String	NAME	= "ConstantValue";
	public final Object			value;

	public ConstantValueAttribute(Object value) {
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

	public static ConstantValueAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int constantvalue_index = in.readUnsignedShort();
		Object value = constant_pool.entry(constantvalue_index);
		if (value instanceof StringInfo) {
			value = constant_pool.string(constantvalue_index);
		}
		return new ConstantValueAttribute(value);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		int constantvalue_index;
		if (value instanceof String) {
			constantvalue_index = constant_pool.stringInfo((String) value);
		} else if (value instanceof Integer) {
			constantvalue_index = constant_pool.integerInfo((Integer) value);
		} else if (value instanceof Long) {
			constantvalue_index = constant_pool.longInfo((Long) value);
		} else if (value instanceof Double) {
			constantvalue_index = constant_pool.doubleInfo((Double) value);
		} else if (value instanceof Float) {
			constantvalue_index = constant_pool.floatInfo((Float) value);
		} else {
			throw new IOException("Unrecognized constant value type " + value);
		}
		out.writeShort(constantvalue_index);
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		return attribute_length;
	}
}
