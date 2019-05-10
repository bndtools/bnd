package aQute.bnd.classfile.writer;

import java.io.DataOutput;
import java.io.IOException;

import aQute.bnd.classfile.ModuleMainClassAttribute;

public class ModuleMainClassAttributeWriter implements AttributeWriter {
	private final int	attribute_name_index;
	private final int	attribute_length;
	private final int	main_class_index;

	public ModuleMainClassAttributeWriter(ConstantPoolWriter constantPool, String mainClass) {
		attribute_name_index = constantPool.utf8Info(ModuleMainClassAttribute.NAME);
		main_class_index = constantPool.classInfo(mainClass);
		attribute_length = 1 * (Short.SIZE / Byte.SIZE);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(main_class_index);
	}
}
