package aQute.libg.cryptography;

import java.math.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import aQute.libg.tuple.*;

public class RSA {
	final static String		ALGORITHM	= "RSA";

	final static KeyFactory	factory		= getKeyFactory();

	static private KeyFactory getKeyFactory() {
		try {
			return KeyFactory.getInstance(ALGORITHM);
		}
		catch (Exception e) {
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

	public static Pair<RSAPrivateKey,RSAPublicKey> generate() throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
		KeyPair keypair = kpg.generateKeyPair();
		return new Pair<RSAPrivateKey,RSAPublicKey>((RSAPrivateKey) keypair.getPrivate(),
				(RSAPublicKey) keypair.getPublic());
	}
}
