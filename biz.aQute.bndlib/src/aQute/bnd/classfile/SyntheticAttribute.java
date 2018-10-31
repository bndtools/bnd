package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class SyntheticAttribute implements Attribute {
	public static final String NAME = "Synthetic";

	SyntheticAttribute() {}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME;
	}

	static SyntheticAttribute parseSyntheticAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		return new SyntheticAttribute();
	}
}
