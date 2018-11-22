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


	static MethodInfo parseMethodInfo(DataInput in, ConstantPool constant_pool) throws IOException {
		return parseMemberInfo(in, constant_pool, MethodInfo::new);
	}
}
