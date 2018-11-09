package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class NestMembersAttribute implements Attribute {
	public static final String	NAME	= "NestMembers";
	public final String[]		classes;

	NestMembersAttribute(String[] classes) {
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

	static NestMembersAttribute parseNestMembersAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_classes = in.readUnsignedShort();
		String[] classes = new String[number_of_classes];
		for (int i = 0; i < number_of_classes; i++) {
			int member_class_index = in.readUnsignedShort();
			classes[i] = constant_pool.className(member_class_index);
		}
		return new NestMembersAttribute(classes);
	}
}
