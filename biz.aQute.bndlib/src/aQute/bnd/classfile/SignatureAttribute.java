package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SignatureAttribute implements Attribute {
	public static final String	NAME	= "Signature";
	public final String			signature;

	public SignatureAttribute(String signature) {
		this.signature = signature;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + signature;
	}

	public static SignatureAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int signature_index = in.readUnsignedShort();
		return new SignatureAttribute(constant_pool.utf8(signature_index));
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(constant_pool.utf8Info(signature));
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		return attribute_length;
	}
}
