package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public abstract class TypeAnnotationsAttribute implements Attribute {
	public final TypeAnnotationInfo[] type_annotations;

	protected TypeAnnotationsAttribute(TypeAnnotationInfo[] type_annotations) {
		this.type_annotations = type_annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(type_annotations);
	}

	@FunctionalInterface
	public interface Constructor<A extends TypeAnnotationsAttribute> {
		A init(TypeAnnotationInfo[] type_annotations);
	}

	static <A extends TypeAnnotationsAttribute> A read(DataInput in, ConstantPool constant_pool,
		Constructor<A> constructor) throws IOException {
		int num_annotations = in.readUnsignedShort();
		TypeAnnotationInfo[] type_annotations = new TypeAnnotationInfo[num_annotations];
		for (int i = 0; i < num_annotations; i++) {
			type_annotations[i] = TypeAnnotationInfo.read(in, constant_pool);
		}

		return constructor.init(type_annotations);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(type_annotations.length);
		for (TypeAnnotationInfo type_annotation : type_annotations) {
			type_annotation.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (TypeAnnotationInfo type_annotation : type_annotations) {
			attribute_length += type_annotation.value_length();
		}
		return attribute_length;
	}
}
