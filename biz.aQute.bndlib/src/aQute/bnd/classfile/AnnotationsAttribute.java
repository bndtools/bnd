package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public abstract class AnnotationsAttribute implements Attribute {
	public final AnnotationInfo[] annotations;

	protected AnnotationsAttribute(AnnotationInfo[] annotations) {
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(annotations);
	}

	@FunctionalInterface
	public interface Constructor<A extends AnnotationsAttribute> {
		A init(AnnotationInfo[] annotations);
	}

	static <A extends AnnotationsAttribute> A read(DataInput in, ConstantPool constant_pool,
		Constructor<A> constructor) throws IOException {
		AnnotationInfo[] annotations = AnnotationInfo.readInfos(in, constant_pool);
		return constructor.init(annotations);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		AnnotationInfo.writeInfos(out, constant_pool, annotations);
	}

	@Override
	public int attribute_length() {
		int attribute_length = AnnotationInfo.infos_length(annotations);
		return attribute_length;
	}
}
