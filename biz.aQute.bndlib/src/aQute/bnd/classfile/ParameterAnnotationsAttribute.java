package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public abstract class ParameterAnnotationsAttribute implements Attribute {
	public final ParameterAnnotationInfo[] parameter_annotations;

	ParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		this.parameter_annotations = parameter_annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(parameter_annotations);
	}

	static <A extends ParameterAnnotationsAttribute> A parseParameterAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool, Function<ParameterAnnotationInfo[], A> constructor) throws IOException {
		int num_parameters = in.readUnsignedByte();
		ParameterAnnotationInfo[] parameter_annotations = new ParameterAnnotationInfo[num_parameters];
		for (int p = 0; p < num_parameters; p++) {
			int num_annotations = in.readUnsignedShort();
			AnnotationInfo[] annotations = new AnnotationInfo[num_annotations];
			for (int i = 0; i < num_annotations; i++) {
				annotations[i] = AnnotationInfo.parseAnnotationInfo(in, constant_pool);
			}
			parameter_annotations[p] = new ParameterAnnotationInfo(p, annotations);
		}

		return constructor.apply(parameter_annotations);
	}
}
