package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class MethodInfo extends MemberInfo {

	public MethodInfo(int access_flags, String name, String descriptor, Attribute[] attributes) {
		super(access_flags, name, descriptor, attributes);
	}

	@Override
	public String toString() {
		return toString(Modifier.methodModifiers());
	}

	public static MethodInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
		return read(in, constant_pool, MethodInfo::new);
	}
}
