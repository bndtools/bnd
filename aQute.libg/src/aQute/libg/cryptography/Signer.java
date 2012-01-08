package aQute.libg.cryptography;

import java.io.*;
import java.security.*;

public class Signer<D extends Digest> extends OutputStream {
	Signature	signature;
	Digester<D> digester;
	
	Signer(Signature s, Digester<D> digester) {
		this.signature = s;
		this.digester  = digester;
	}

	@Override public void write(byte[] buffer, int offset, int length) throws IOException {
		try {
			signature.update(buffer, offset, length);
		} catch (SignatureException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override public void write(int b) throws IOException {
		try {
			signature.update((byte) b);
		} catch (SignatureException e) {
			throw new IOException(e.getMessage());
		}
	}
	

	public D signature() throws Exception {
		return digester.digest(signature().digest());
	}
}
