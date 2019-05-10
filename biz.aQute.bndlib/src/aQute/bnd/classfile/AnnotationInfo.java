package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiFunction;

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

	static <A extends AnnotationInfo> A read(DataInput in, ConstantPool constant_pool,
		BiFunction<String, ElementValueInfo[], A> constructor) throws IOException {
		int type_index = in.readUnsignedShort();
		int num_element_value_pairs = in.readUnsignedShort();
		ElementValueInfo[] element_value_pairs = new ElementValueInfo[num_element_value_pairs];
		for (int i = 0; i < num_element_value_pairs; i++) {
			element_value_pairs[i] = ElementValueInfo.read(in, constant_pool);
		}

		return constructor.apply(constant_pool.utf8(type_index), element_value_pairs);
	}
}
