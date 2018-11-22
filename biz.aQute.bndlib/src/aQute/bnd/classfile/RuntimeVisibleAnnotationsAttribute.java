package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeVisibleAnnotationsAttribute extends AnnotationsAttribute {
	public static final String NAME = "RuntimeVisibleAnnotations";

	RuntimeVisibleAnnotationsAttribute(AnnotationInfo[] annotations) {
		super(annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeVisibleAnnotationsAttribute parseRuntimeVisibleAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool) throws IOException {
		return parseAnnotationsAttribute(in, constant_pool, RuntimeVisibleAnnotationsAttribute::new);
	}
}
