package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public abstract class AnnotationsAttribute implements Attribute {
	public final AnnotationInfo[] annotations;

	AnnotationsAttribute(AnnotationInfo[] annotations) {
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(annotations);
	}

	static <A extends AnnotationsAttribute> A parseAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool, Function<AnnotationInfo[], A> constructor) throws IOException {
		int num_annotations = in.readUnsignedShort();
		AnnotationInfo[] annotations = new AnnotationInfo[num_annotations];
		for (int i = 0; i < num_annotations; i++) {
			annotations[i] = AnnotationInfo.parseAnnotationInfo(in, constant_pool);
		}

		return constructor.apply(annotations);
	}
}
