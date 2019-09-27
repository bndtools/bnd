package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeInvisibleParameterAnnotationsAttribute extends ParameterAnnotationsAttribute {
	public static final String NAME = "RuntimeInvisibleParameterAnnotations";

	public RuntimeInvisibleParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		super(parameter_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	public static RuntimeInvisibleParameterAnnotationsAttribute read(DataInput in, ConstantPool constant_pool)
		throws IOException {
		return read(in, constant_pool, RuntimeInvisibleParameterAnnotationsAttribute::new);
	}
}
