package aQute.bnd.classfile;

import java.io.DataInput;
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
}
