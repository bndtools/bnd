package aQute.libg.cryptography;

import java.io.*;
import java.security.*;



public class SHA256 extends Digest {
	public final static String ALGORITHM = "SHA256";
	
	
	public static Digester<SHA256> getDigester(OutputStream ... out  ) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		return new Digester<SHA256>(md, out) {
			@Override public SHA256 digest() throws Exception {
				return new SHA256(md.digest());
			}

			@Override public SHA256 digest(byte[] bytes) {
				return new SHA256(bytes);
			}
			@Override public String getAlgorithm() {
				return ALGORITHM;
			}
		};
	}
	
	public SHA256(byte[] b) {
		super(b, 32);
	}


	@Override public String getAlgorithm() { return ALGORITHM; }

}