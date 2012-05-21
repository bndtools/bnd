package aQute.libg.cryptography;

import java.math.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.regex.*;

public class Crypto {
	static final Pattern	RSA_PRIVATE	= Pattern
												.compile("\\s*RSA\\.Private\\((\\p{XDigit})+:(\\p{XDigit})+\\)\\s*");
	static final Pattern	RSA_PUBLIC	= Pattern
												.compile("\\s*RSA\\.Public\\((\\p{XDigit})+:(\\p{XDigit})+\\)\\s*");

	/**
	 * 
	 * @param <T>
	 * @param spec
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked") public static <T> T fromString(String spec, Class<T> c) throws Exception {
		if ( PrivateKey.class.isAssignableFrom(c)) {
			Matcher m = RSA_PRIVATE.matcher(spec); 
			if ( m.matches()) {
				return (T) RSA.createPrivate(  
						new BigInteger(m.group(1)), new BigInteger(m.group(2)));
			}
			throw new IllegalArgumentException("No such private key " + spec );
		}
		
		if ( PublicKey.class.isAssignableFrom(c)) {
			Matcher m = RSA_PUBLIC.matcher(spec); 
			if ( m.matches()) {
				return (T) RSA.create( new RSAPublicKeySpec( 
						new BigInteger(m.group(1)), new BigInteger(m.group(2))));
			}
			throw new IllegalArgumentException("No such public key " + spec );			
		}
		return null;
	}

	public static String toString( Object key ) {
		if ( key instanceof RSAPrivateKey ) {
			RSAPrivateKey pk = (RSAPrivateKey) key;
			return "RSA.Private(" + pk.getModulus() + ":" + pk.getPrivateExponent() + ")";
		}
		if ( key instanceof RSAPublicKey ) {
			RSAPublicKey pk = (RSAPublicKey) key;
			return "RSA.Private(" + pk.getModulus() + ":" + pk.getPublicExponent() + ")";
		}
		return null;
	}


//	public static <T extends Digest> Signer<T> signer(PrivateKey key, Digester<T> digester) throws NoSuchAlgorithmException {
//		Signature s = Signature.getInstance(key.getAlgorithm() + "with" + digester.getAlgorithm());
//		return new Signer<T>(s,digester);
//	}

	public static Verifier verifier(PublicKey key, Digest digest) throws NoSuchAlgorithmException {
		Signature s = Signature.getInstance(key.getAlgorithm() + "with" + digest.getAlgorithm());
		return new Verifier(s,digest);
	}

	
}
