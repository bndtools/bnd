package aQute.libg.cryptography;

import java.io.*;
import java.security.*;

public class MD5 extends Digest {
	public final static String	ALGORITHM	= "MD5";

	public static Digester<MD5> getDigester(OutputStream... out) throws Exception {
		return new Digester<MD5>(MessageDigest.getInstance(ALGORITHM), out) {

			@Override
			public MD5 digest() throws Exception {
				return new MD5(md.digest());
			}

			@Override
			public MD5 digest(byte[] bytes) {
				return new MD5(bytes);
			}

			@Override
			public String getAlgorithm() {
				return ALGORITHM;
			}
		};
	}

	public MD5(byte[] digest) {
		super(digest, 16);
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}

}