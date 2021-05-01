package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NestHostAttribute implements Attribute {
	public static final String	NAME	= "NestHost";
	public final String			host_class;

	public NestHostAttribute(String host_class) {
		this.host_class = host_class;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + host_class;
	}

	public static NestHostAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int host_class_index = in.readUnsignedShort();
		return new NestHostAttribute(constant_pool.className(host_class_index));
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(constant_pool.classInfo(host_class));
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		return attribute_length;
	}
}
