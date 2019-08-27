package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;

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

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		ByteBuffer duplicate = value.duplicate();
		duplicate.rewind();
		IO.copy(duplicate, out);
	}

	@Override
	public int attribute_length() {
		int attribute_length = value.limit();
		return attribute_length;
	}
}
