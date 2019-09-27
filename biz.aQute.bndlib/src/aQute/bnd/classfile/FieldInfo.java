package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class FieldInfo extends MemberInfo {

	public FieldInfo(int access_flags, String name, String descriptor, Attribute[] attributes) {
		super(access_flags, name, descriptor, attributes);
	}

	@Override
	public String toString() {
		return toString(Modifier.fieldModifiers());
	}

	public static FieldInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
		return read(in, constant_pool, FieldInfo::new);
	}
}
