package aQute.bnd.classfile.preview;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ConstantPool;

public class PermittedSubclassesAttribute implements Attribute {
	public static final String	NAME	= "PermittedSubclasses";
	public final String[]		classes;

	public PermittedSubclassesAttribute(String[] classes) {
		this.classes = classes;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(classes);
	}

	public static PermittedSubclassesAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_classes = in.readUnsignedShort();
		String[] classes = new String[number_of_classes];
		for (int i = 0; i < number_of_classes; i++) {
			int class_index = in.readUnsignedShort();
			classes[i] = constant_pool.className(class_index);
		}
		return new PermittedSubclassesAttribute(classes);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(classes.length);
		for (String class_name : classes) {
			int class_index = constant_pool.classInfo(class_name);
			out.writeShort(class_index);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = (1 + classes.length) * Short.BYTES;
		return attribute_length;
	}
}
