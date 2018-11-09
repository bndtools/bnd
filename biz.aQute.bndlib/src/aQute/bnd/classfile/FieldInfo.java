package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class FieldInfo extends MemberInfo {

	FieldInfo(int access_flags, String name, String descriptor, Attribute[] attributes) {
		super(access_flags, name, descriptor, attributes);
	}

	static FieldInfo parseFieldInfo(DataInput in, ConstantPool constant_pool) throws IOException {
		return parseMemberInfo(in, constant_pool, FieldInfo::new);
	}
}
