package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ParameterAnnotationInfo {
	public final int				parameter;
	public final AnnotationInfo[]	annotations;

	public ParameterAnnotationInfo(int parameter, AnnotationInfo[] annotations) {
		this.parameter = parameter;
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return parameter + ":" + Arrays.toString(annotations);
	}

	static ParameterAnnotationInfo read(DataInput in, ConstantPool constant_pool, int parameter)
		throws IOException {
		AnnotationInfo[] annotations = AnnotationInfo.readInfos(in, constant_pool);
		return new ParameterAnnotationInfo(parameter, annotations);
	}

	void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		AnnotationInfo.writeInfos(out, constant_pool, annotations);
	}

	int value_length() {
		return AnnotationInfo.infos_length(annotations);
	}
}
