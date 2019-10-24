package aQute.libg.cryptography;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import aQute.lib.exceptions.Exceptions;

public class SHA1 extends Digest {
	public final static String ALGORITHM = "SHA-1";

	public static Digester<SHA1> getDigester(OutputStream... out) {
		try {
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
		} catch (NoSuchAlgorithmException e) {
			throw Exceptions.duck(e);
		}
	}

	public SHA1(byte[] b) {
		super(b, 20);
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}

	public static SHA1 digest(byte[] data) throws Exception {
		return getDigester().from(data);
	}

	public static SHA1 digest(File f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}

	public static SHA1 digest(InputStream f) throws NoSuchAlgorithmException, Exception {
		return getDigester().from(f);
	}
}
