package aQute.libg.cryptography;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 extends Digest {
	public final static String ALGORITHM = "MD5";

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

	public static MD5 digest(byte[] data) throws Exception {
		return getDigester().from(data);
	}

	public static MD5 digest(File f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}

	public static MD5 digest(InputStream f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}
}
