package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class NestMembersAttribute implements Attribute {
	public static final String	NAME	= "NestMembers";
	public final String[]		classes;

	public NestMembersAttribute(String[] classes) {
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

	public static NestMembersAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_classes = in.readUnsignedShort();
		String[] classes = new String[number_of_classes];
		for (int i = 0; i < number_of_classes; i++) {
			int member_class_index = in.readUnsignedShort();
			classes[i] = constant_pool.className(member_class_index);
		}
		return new NestMembersAttribute(classes);
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
