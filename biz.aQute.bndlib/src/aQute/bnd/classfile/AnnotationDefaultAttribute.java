package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;

public class AnnotationDefaultAttribute implements Attribute {
	public static final String	NAME	= "AnnotationDefault";
	public final Object			value;

	public AnnotationDefaultAttribute(Object value) {
		this.value = value;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + ElementValueInfo.toString(value);
	}

	static AnnotationDefaultAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		Object value = ElementValueInfo.readValue(in, constant_pool);
		return new AnnotationDefaultAttribute(value);
	}
}
