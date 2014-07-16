package aQute.lib.settings;

import java.io.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class PasswordCryptor {

    // Salt
    final static byte[] salt = {
        (byte)0x21, (byte)0x48, (byte)0x3F, (byte)0x65,
        (byte)0x0, (byte)0x8, (byte)0xee, (byte)0xFF
    };

    // Iteration count
    final static int count = 50;
	
    final static PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
    final SecretKeyFactory     keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");


    public PasswordCryptor() throws Exception {
	}
    

    public OutputStream encrypt( char[] password, OutputStream out ) throws Exception {
    	PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        final Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        return new CipherOutputStream(out, cipher);
    }

    public InputStream decrypt( char[] password, InputStream out ) throws Exception {
    	PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        final Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

        return new CipherInputStream(out, cipher);
    }

}
