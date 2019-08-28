package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
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

	public static AnnotationDefaultAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		Object value = ElementValueInfo.readValue(in, constant_pool);
		return new AnnotationDefaultAttribute(value);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		ElementValueInfo.writeValue(out, constant_pool, value);
	}

	@Override
	public int attribute_length() {
		int attribute_length = ElementValueInfo.value_length(value);
		return attribute_length;
	}
}
