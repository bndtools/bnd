package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import aQute.bnd.classfile.ConstantPool.NameAndTypeInfo;

public class EnclosingMethodAttribute implements Attribute {
	public static final String	NAME	= "EnclosingMethod";
	public final String			class_name;
	public final String			method_name;
	public final String			method_descriptor;

	public EnclosingMethodAttribute(String class_name, String method_name, String method_descriptor) {
		this.class_name = class_name;
		this.method_name = method_name;
		this.method_descriptor = method_descriptor;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + class_name + "." + method_name + method_descriptor;
	}

	public static EnclosingMethodAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int class_index = in.readUnsignedShort();
		int method_index = in.readUnsignedShort();
		String class_name = constant_pool.className(class_index);
		String method_name;
		String method_descriptor;
		if (method_index != 0) {
			NameAndTypeInfo nameAndType = constant_pool.entry(method_index);
			method_name = constant_pool.utf8(nameAndType.name_index);
			method_descriptor = constant_pool.utf8(nameAndType.descriptor_index);
		} else {
			method_name = null;
			method_descriptor = null;
		}
		return new EnclosingMethodAttribute(class_name, method_name, method_descriptor);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		int class_index = constant_pool.classInfo(class_name);
		int method_index = (method_name != null) ? constant_pool.nameAndTypeInfo(method_name, method_descriptor) : 0;
		out.writeShort(class_index);
		out.writeShort(method_index);
	}

	@Override
	public int attribute_length() {
		int attribute_length = 2 * Short.BYTES;
		return attribute_length;
	}
}
