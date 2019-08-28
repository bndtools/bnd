package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class RuntimeVisibleParameterAnnotationsAttribute extends ParameterAnnotationsAttribute {
	public static final String NAME = "RuntimeVisibleParameterAnnotations";

	public RuntimeVisibleParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		super(parameter_annotations);
	}

	@Override
	public String name() {
		return NAME;
	}

	public static RuntimeVisibleParameterAnnotationsAttribute read(DataInput in, ConstantPool constant_pool)
		throws IOException {
		return read(in, constant_pool, RuntimeVisibleParameterAnnotationsAttribute::new);
	}
}
