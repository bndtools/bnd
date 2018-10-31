package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public abstract class MemberInfo extends ElementInfo {
	public final String	name;
	public final String	descriptor;

	MemberInfo(int access, String name, String descriptor, Attribute[] attributes) {
		super(access, attributes);
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public String toString() {
		return Modifier.toString(access) + " " + name + " " + descriptor + " " + Arrays.deepToString(attributes);
	}

	@FunctionalInterface
	interface Constructor<M> {
		M apply(int access_flags, String name, String descriptor, Attribute[] attributes);
	}

	static <M extends MemberInfo> M parseMemberInfo(DataInput in, ConstantPool constant_pool,
		Constructor<M> constructor) throws IOException {
		int access_flags = in.readUnsignedShort();
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		Attribute[] attributes = ClassFile.parseAttributes(in, constant_pool);

		return constructor.apply(access_flags, constant_pool.utf8(name_index), constant_pool.utf8(descriptor_index),
			attributes);
	}
}
