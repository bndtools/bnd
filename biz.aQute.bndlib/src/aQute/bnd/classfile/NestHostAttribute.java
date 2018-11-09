package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class NestHostAttribute implements Attribute {
	public static final String	NAME	= "NestHost";
	public final String			host_class;

	NestHostAttribute(String host_class) {
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

	static NestHostAttribute parseNestHostAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int host_class_index = in.readUnsignedShort();
		return new NestHostAttribute(constant_pool.className(host_class_index));
	}
}
