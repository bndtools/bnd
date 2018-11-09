package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class AnnotationDefaultAttribute implements Attribute {
	public static final String	NAME	= "AnnotationDefault";
	public final Object			value;

	AnnotationDefaultAttribute(Object value) {
		this.value = value;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + value;
	}

	static AnnotationDefaultAttribute parseAnnotationDefaultAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		Object value = ElementValueInfo.parseElementValue(in, constant_pool);
		return new AnnotationDefaultAttribute(value);
	}
}
