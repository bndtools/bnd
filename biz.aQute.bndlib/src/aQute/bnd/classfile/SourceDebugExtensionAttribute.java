package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;

public class SourceDebugExtensionAttribute implements Attribute {
	public static final String	NAME	= "SourceDebugExtension";
	public final ByteBuffer		debug_extension;

	public SourceDebugExtensionAttribute(ByteBuffer debug_extension) {
		this.debug_extension = debug_extension;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME;
	}

	public static SourceDebugExtensionAttribute read(DataInput in, int attribute_length) throws IOException {
		if (attribute_length < 0) {
			throw new IOException("attribute length > 2Gb");
		}
		ByteBuffer debug_extension = ClassFile.slice(in, attribute_length);
		return new SourceDebugExtensionAttribute(debug_extension);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		ByteBuffer duplicate = debug_extension.duplicate();
		duplicate.rewind();
		IO.copy(duplicate, out);
	}

	@Override
	public int attribute_length() {
		int attribute_length = debug_extension.limit();
		return attribute_length;
	}
}
