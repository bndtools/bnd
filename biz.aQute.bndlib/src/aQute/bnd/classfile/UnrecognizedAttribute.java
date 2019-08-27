package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

public class UnrecognizedAttribute implements Attribute {
	public final String		name;
	public final ByteBuffer	value;

	public UnrecognizedAttribute(String name, ByteBuffer value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name();
	}

	public static UnrecognizedAttribute read(DataInput in, String name, int attribute_length) throws IOException {
		if (attribute_length < 0) {
			throw new IOException("attribute length > 2Gb");
		}
		ByteBuffer value = ClassFile.slice(in, attribute_length);
		return new UnrecognizedAttribute(name, value);
	}
}
