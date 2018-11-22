package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeVisibleTypeAnnotationsAttribute extends TypeAnnotationsAttribute {
	public static final String NAME = "RuntimeVisibleTypeAnnotations";

	RuntimeVisibleTypeAnnotationsAttribute(TypeAnnotationInfo[] type_annotations) {
		super(type_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeVisibleTypeAnnotationsAttribute parseRuntimeVisibleTypeAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool) throws IOException {
		return parseTypeAnnotationsAttribute(in, constant_pool, RuntimeVisibleTypeAnnotationsAttribute::new);
	}
}
