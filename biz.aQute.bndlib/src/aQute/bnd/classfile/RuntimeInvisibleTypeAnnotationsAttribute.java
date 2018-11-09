package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeInvisibleTypeAnnotationsAttribute extends TypeAnnotationsAttribute {
	public static final String NAME = "RuntimeInvisibleTypeAnnotations";

	RuntimeInvisibleTypeAnnotationsAttribute(TypeAnnotationInfo[] type_annotations) {
		super(type_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeInvisibleTypeAnnotationsAttribute parseRuntimeInvisibleTypeAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool) throws IOException {
		return parseTypeAnnotationsAttribute(in, constant_pool, RuntimeInvisibleTypeAnnotationsAttribute::new);
	}
}
