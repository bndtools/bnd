package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeVisibleAnnotationsAttribute extends AnnotationsAttribute {
	public static final String NAME = "RuntimeVisibleAnnotations";

	public RuntimeVisibleAnnotationsAttribute(AnnotationInfo[] annotations) {
		super(annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	public static RuntimeVisibleAnnotationsAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		return read(in, constant_pool, RuntimeVisibleAnnotationsAttribute::new);
	}
}
