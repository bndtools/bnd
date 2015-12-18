package aQute.libg.cryptography;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;

public class Signer<D extends Digest> extends OutputStream {
	Signature	signature;
	Digester<D>	digester;

	Signer(Signature s, Digester<D> digester) {
		this.signature = s;
		this.digester = digester;
	}

	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException {
		try {
			signature.update(buffer, offset, length);
			digester.write(buffer, offset, length);
		} catch (SignatureException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void write(int b) throws IOException {
		try {
			signature.update((byte) b);
			digester.write(b);
		} catch (SignatureException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	public Signature signature() throws Exception {
		return signature;
	}

	public D digest() throws Exception {
		return digester.digest();
	}
}
