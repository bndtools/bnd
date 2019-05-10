package aQute.bnd.classfile;

import java.io.DataInput;
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

	static SignatureAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int signature_index = in.readUnsignedShort();
		return new SignatureAttribute(constant_pool.utf8(signature_index));
	}
}
