package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class AnnotationInfo {
	public final String				type;
	public final ElementValueInfo[]	values;

	public AnnotationInfo(String type, ElementValueInfo[] values) {
		this.type = type;
		this.values = values;
	}

	@Override
	public String toString() {
		return type + " " + Arrays.toString(values);
	}

	static AnnotationInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
		return read(in, constant_pool, AnnotationInfo::new);
	}

	@FunctionalInterface
	public interface Constructor<A extends AnnotationInfo> {
		A init(String type, ElementValueInfo[] values);
	}

	static <A extends AnnotationInfo> A read(DataInput in, ConstantPool constant_pool,
		Constructor<A> constructor) throws IOException {
		int type_index = in.readUnsignedShort();
		int num_element_value_pairs = in.readUnsignedShort();
		ElementValueInfo[] element_value_pairs = new ElementValueInfo[num_element_value_pairs];
		for (int i = 0; i < num_element_value_pairs; i++) {
			element_value_pairs[i] = ElementValueInfo.read(in, constant_pool);
		}

		return constructor.init(constant_pool.utf8(type_index), element_value_pairs);
	}

	void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int type_index = constant_pool.utf8Info(type);
		out.writeShort(type_index);
		out.writeShort(values.length);
		for (ElementValueInfo value : values) {
			value.write(out, constant_pool);
		}
	}

	int value_length() {
		int value_length = 2 * Short.BYTES;
		for (ElementValueInfo value : values) {
			value_length += value.value_length();
		}
		return value_length;
	}

	static AnnotationInfo[] readInfos(DataInput in, ConstantPool constant_pool) throws IOException {
		int num_annotations = in.readUnsignedShort();
		AnnotationInfo[] annotations = new AnnotationInfo[num_annotations];
		for (int i = 0; i < num_annotations; i++) {
			annotations[i] = AnnotationInfo.read(in, constant_pool);
		}
		return annotations;
	}

	static void writeInfos(DataOutput out, ConstantPool constant_pool, AnnotationInfo[] annotations)
		throws IOException {
		out.writeShort(annotations.length);
		for (AnnotationInfo annotation : annotations) {
			annotation.write(out, constant_pool);
		}
	}

	static int infos_length(AnnotationInfo[] annotations) {
		int attribute_length = 1 * Short.BYTES;
		for (AnnotationInfo annotation : annotations) {
			attribute_length += annotation.value_length();
		}
		return attribute_length;
	}
}
