package aQute.libg.cryptography;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import aQute.libg.tuple.Pair;

public class RSA {
	final static String		ALGORITHM	= "RSA";

	final static KeyFactory	factory		= getKeyFactory();

	static private KeyFactory getKeyFactory() {
		try {
			return KeyFactory.getInstance(ALGORITHM);
		} catch (Exception e) {
			// built in
		}
		return null;
	}

	public static RSAPrivateKey create(RSAPrivateKeySpec keyspec) throws InvalidKeySpecException {
		return (RSAPrivateKey) factory.generatePrivate(keyspec);
	}

	public static RSAPublicKey create(RSAPublicKeySpec keyspec) throws InvalidKeySpecException {
		return (RSAPublicKey) factory.generatePrivate(keyspec);
	}

	public static RSAPublicKey createPublic(BigInteger m, BigInteger e) throws InvalidKeySpecException {
		return create(new RSAPublicKeySpec(m, e));
	}

	public static RSAPrivateKey createPrivate(BigInteger m, BigInteger e) throws InvalidKeySpecException {
		return create(new RSAPrivateKeySpec(m, e));
	}

	public static Pair<RSAPrivateKey, RSAPublicKey> generate() throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
		KeyPair keypair = kpg.generateKeyPair();
		return new Pair<>((RSAPrivateKey) keypair.getPrivate(), (RSAPublicKey) keypair.getPublic());
	}
}
