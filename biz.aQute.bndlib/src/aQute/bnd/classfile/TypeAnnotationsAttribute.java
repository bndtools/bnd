package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public abstract class TypeAnnotationsAttribute implements Attribute {
	public final TypeAnnotationInfo[] type_annotations;

	TypeAnnotationsAttribute(TypeAnnotationInfo[] type_annotations) {
		this.type_annotations = type_annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(type_annotations);
	}

	static <A extends TypeAnnotationsAttribute> A parseTypeAnnotationsAttribute(DataInput in,
		ConstantPool constant_pool, Function<TypeAnnotationInfo[], A> constructor) throws IOException {
		int num_annotations = in.readUnsignedShort();
		TypeAnnotationInfo[] type_annotations = new TypeAnnotationInfo[num_annotations];
		for (int i = 0; i < num_annotations; i++) {
			type_annotations[i] = TypeAnnotationInfo.parseTypeAnnotationInfo(in, constant_pool);
		}

		return constructor.apply(type_annotations);
	}
}
