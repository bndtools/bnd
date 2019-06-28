package aQute.libg.cryptography;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA512 extends Digest {
	public final static String ALGORITHM = "SHA-512";

	public static Digester<SHA512> getDigester(OutputStream... out) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		return new Digester<SHA512>(md, out) {
			@Override
			public SHA512 digest() throws Exception {
				return new SHA512(md.digest());
			}

			@Override
			public SHA512 digest(byte[] bytes) {
				return new SHA512(bytes);
			}

			@Override
			public String getAlgorithm() {
				return ALGORITHM;
			}
		};
	}

	public SHA512(byte[] b) {
		super(b, 32);
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}

	public static SHA512 digest(byte[] data) throws Exception {
		return getDigester().from(data);
	}

	public static SHA512 digest(File f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}

	public static SHA512 digest(InputStream f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}
}
