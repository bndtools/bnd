package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ModuleMainClassAttribute implements Attribute {
	public static final String	NAME	= "ModuleMainClass";
	public final String			main_class;

	public ModuleMainClassAttribute(String main_class) {
		this.main_class = main_class;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + main_class;
	}

	public static ModuleMainClassAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int main_class_index = in.readUnsignedShort();
		return new ModuleMainClassAttribute(constant_pool.className(main_class_index));
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		int main_class_index = constant_pool.classInfo(main_class);
		out.writeShort(main_class_index);
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		return attribute_length;
	}
}
