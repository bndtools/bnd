package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class DeprecatedAttribute implements Attribute {
	public static final String NAME = "Deprecated";

	public DeprecatedAttribute() {
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME;
	}

	static DeprecatedAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		return new DeprecatedAttribute();
	}
}
