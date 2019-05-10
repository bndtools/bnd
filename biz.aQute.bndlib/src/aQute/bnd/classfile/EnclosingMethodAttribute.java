package aQute.bnd.classfile;

import java.io.DataInput;
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

	static EnclosingMethodAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int class_index = in.readUnsignedShort();
		int method_index = in.readUnsignedShort();
		String class_name = constant_pool.className(class_index);
		String method_name;
		String method_descriptor;
		if (method_index != 0) {
			NameAndTypeInfo nameAndTyoe = constant_pool.entry(method_index);
			method_name = constant_pool.utf8(nameAndTyoe.name_index);
			method_descriptor = constant_pool.utf8(nameAndTyoe.descriptor_index);
		} else {
			method_name = null;
			method_descriptor = null;
		}
		return new EnclosingMethodAttribute(class_name, method_name, method_descriptor);
	}
}
