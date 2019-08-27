package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SourceFileAttribute implements Attribute {
	public static final String	NAME	= "SourceFile";
	public final String			sourcefile;

	public SourceFileAttribute(String sourcefile) {
		this.sourcefile = sourcefile;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + sourcefile;
	}

	public static SourceFileAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int sourcefile_index = in.readUnsignedShort();
		return new SourceFileAttribute(constant_pool.utf8(sourcefile_index));
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		int sourcefile_index = constant_pool.utf8Info(sourcefile);
		out.writeShort(sourcefile_index);
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		return attribute_length;
	}
}
