package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ParameterAnnotationInfo {
	public final int				parameter;
	public final AnnotationInfo[]	annotations;

	ParameterAnnotationInfo(int parameter, AnnotationInfo[] annotations) {
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
}
