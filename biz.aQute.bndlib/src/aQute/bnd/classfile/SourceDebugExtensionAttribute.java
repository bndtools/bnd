package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SourceDebugExtensionAttribute implements Attribute {
	public static final String	NAME	= "SourceDebugExtension";
	public final ByteBuffer		debug_extension;

	SourceDebugExtensionAttribute(ByteBuffer debug_extension) {
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

	static SourceDebugExtensionAttribute parseSourceDebugExtensionAttribute(DataInput in, int attribute_length)
		throws IOException {
		if (attribute_length < 0) {
			throw new IOException("attribute length > 2Gb");
		}
		ByteBuffer debug_extension = ClassFile.slice(in, attribute_length);
		return new SourceDebugExtensionAttribute(debug_extension);
	}
}
