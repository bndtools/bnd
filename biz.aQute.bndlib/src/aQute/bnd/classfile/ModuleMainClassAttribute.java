package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class ModuleMainClassAttribute implements Attribute {
	public static final String	NAME	= "ModuleMainClass";
	public final String			main_class;

	ModuleMainClassAttribute(String main_class) {
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

	static ModuleMainClassAttribute parseModuleMainClassAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		int main_class_index = in.readUnsignedShort();
		return new ModuleMainClassAttribute(constant_pool.className(main_class_index));
	}
}
