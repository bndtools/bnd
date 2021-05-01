package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeInvisibleAnnotationsAttribute extends AnnotationsAttribute {
	public static final String NAME = "RuntimeInvisibleAnnotations";

	public RuntimeInvisibleAnnotationsAttribute(AnnotationInfo[] annotations) {
		super(annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	public static RuntimeInvisibleAnnotationsAttribute read(DataInput in, ConstantPool constant_pool)
		throws IOException {
		return read(in, constant_pool, RuntimeInvisibleAnnotationsAttribute::new);
	}
}
