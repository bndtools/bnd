package aQute.libg.cryptography;

import java.io.*;
import java.security.*;


public class Verifier extends OutputStream {
	final Signature signature;
	final Digest d;
	
	Verifier(Signature s, Digest d) {
		this.signature = s;
		this.d = d;
	}
	
	@Override
	public void write( byte[] buffer, int offset, int length) throws IOException {
		try {
			signature.update(buffer, offset, length);
		} catch (SignatureException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public void write( int b) throws IOException {
		try {
			signature.update((byte) b);
		} catch (SignatureException e) {
			throw new IOException(e.getMessage());
		}
	}

	public boolean verify() throws Exception {
		return signature.verify(d.digest());
	}
	
}
