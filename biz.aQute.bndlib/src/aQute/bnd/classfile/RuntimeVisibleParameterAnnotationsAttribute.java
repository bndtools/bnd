package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeVisibleParameterAnnotationsAttribute extends ParameterAnnotationsAttribute {
	public static final String NAME = "RuntimeVisibleParameterAnnotations";

	RuntimeVisibleParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		super(parameter_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	static RuntimeVisibleParameterAnnotationsAttribute parseRuntimeVisibleParameterAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool) throws IOException {
		return parseParameterAnnotationsAttribute(in, constant_pool, RuntimeVisibleParameterAnnotationsAttribute::new);
	}
}
