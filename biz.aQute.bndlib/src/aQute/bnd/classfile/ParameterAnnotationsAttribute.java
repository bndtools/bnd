package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public abstract class ParameterAnnotationsAttribute implements Attribute {
	public final ParameterAnnotationInfo[] parameter_annotations;

	protected ParameterAnnotationsAttribute(ParameterAnnotationInfo[] parameter_annotations) {
		this.parameter_annotations = parameter_annotations;
	}

	@Override
	public String toString() {
		return name() + " " + Arrays.toString(parameter_annotations);
	}

	@FunctionalInterface
	public interface Constructor<A extends ParameterAnnotationsAttribute> {
		A init(ParameterAnnotationInfo[] parameter_annotations);
	}

	static <A extends ParameterAnnotationsAttribute> A read(DataInput in, ConstantPool constant_pool,
		Constructor<A> constructor) throws IOException {
		int num_parameters = in.readUnsignedByte();
		ParameterAnnotationInfo[] parameter_annotations = new ParameterAnnotationInfo[num_parameters];
		for (int parameter = 0; parameter < num_parameters; parameter++) {
			parameter_annotations[parameter] = ParameterAnnotationInfo.read(in, constant_pool, parameter);
		}

		return constructor.init(parameter_annotations);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeByte(parameter_annotations.length);
		for (ParameterAnnotationInfo parameter_annotation : parameter_annotations) {
			parameter_annotation.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Byte.BYTES;
		for (ParameterAnnotationInfo parameter_annotation : parameter_annotations) {
			attribute_length += parameter_annotation.value_length();
		}
		return attribute_length;
	}
}
