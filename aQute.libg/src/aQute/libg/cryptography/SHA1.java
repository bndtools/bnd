package aQute.libg.cryptography;

import java.io.*;
import java.security.*;

public class SHA1 extends Digest {
	public final static String	ALGORITHM	= "SHA1";

	public static Digester<SHA1> getDigester(OutputStream... out) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		return new Digester<SHA1>(md, out) {
			@Override
			public SHA1 digest() throws Exception {
				return new SHA1(md.digest());
			}

			@Override
			public SHA1 digest(byte[] bytes) {
				return new SHA1(bytes);
			}

			@Override
			public String getAlgorithm() {
				return ALGORITHM;
			}
		};
	}

	public SHA1(byte[] b) {
		super(b, 20);
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}

	public static SHA1 digest(byte [] data) throws Exception {
		return getDigester().digest(data);
	}
}