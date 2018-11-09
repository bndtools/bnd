package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeInvisibleAnnotationsAttribute extends AnnotationsAttribute {
	public static final String NAME = "RuntimeInvisibleAnnotations";

	RuntimeInvisibleAnnotationsAttribute(AnnotationInfo[] annotations) {
		super(annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeInvisibleAnnotationsAttribute parseRuntimeInvisibleAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool) throws IOException {
		return parseAnnotationsAttribute(in, constant_pool, RuntimeInvisibleAnnotationsAttribute::new);
	}
}
