package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeInvisibleParameterAnnotationsAttribute extends ParameterAnnotationsAttribute {
	public static final String NAME = "RuntimeInvisibleParameterAnnotations";

	RuntimeInvisibleParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		super(parameter_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeInvisibleParameterAnnotationsAttribute parseRuntimeInvisibleParameterAnnotationsAttribute(
		DataInput in, ConstantPool constant_pool) throws IOException {
		return parseParameterAnnotationsAttribute(in, constant_pool,
			RuntimeInvisibleParameterAnnotationsAttribute::new);
	}
}
