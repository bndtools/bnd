package aQute.bnd.classfile;

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
		return parameter + ":" + Arrays.deepToString(annotations);
	}
}
