package aQute.maven.nexus.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import aQute.lib.io.IO;

@SuppressWarnings("restriction")
public class Signer {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	private PGPPrivateKey				privateKey;
	private PGPSecretKey				secretKey;
	private JcaPGPContentSignerBuilder	contentSigner;

	public Signer(File secretKey, char[] passphrase) throws Exception {
		this(new FileInputStream(secretKey), passphrase);
	}

	public Signer(InputStream keySource, char[] passphrase) throws Exception {
		this.secretKey = readSecretKey(keySource);
		JcePBESecretKeyDecryptorBuilder decryptorBuilder = new JcePBESecretKeyDecryptorBuilder();
		PBESecretKeyDecryptor decryptor = decryptorBuilder.setProvider("BC").build(passphrase);
		this.privateKey = this.secretKey.extractPrivateKey(decryptor);
		JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
				secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA1);
		contentSigner = contentSignerBuilder.setProvider("BC");

	}

	public Signer(char[] passphrase) throws Exception {
		this(new FileInputStream(IO.getFile("~/.gnupg/secring.gpg")), passphrase);
	}

	public void sign(File file) throws Exception {
		File outFile = new File(file.getParentFile(), file.getName() + ".asc");
		try (OutputStream out = new FileOutputStream(outFile);) {

			PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSigner);
			signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

			Iterator< ? > it = secretKey.getPublicKey().getUserIDs();
			if (it.hasNext()) {
				PGPSignatureSubpacketGenerator signatureSubPacketGenerator = new PGPSignatureSubpacketGenerator();

				signatureSubPacketGenerator.setSignerUserID(false, (String) it.next());
				signatureGenerator.setHashedSubpackets(signatureSubPacketGenerator.generate());
			}

			PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);
			BCPGOutputStream bOut = new BCPGOutputStream(compressedDataGenerator.open(out));

			signatureGenerator.generateOnePassVersion(false).encode(bOut);

			PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
			OutputStream lOut = literalDataGenerator.open(bOut, PGPLiteralData.BINARY, file);

			try (FileInputStream fIn = new FileInputStream(file);) {
				byte[] buffer = new byte[10000];
				for (int n = fIn.read(buffer); n > 0; n = fIn.read(buffer)) {
					lOut.write(buffer, 0, n);
					signatureGenerator.update(buffer, 0, n);
				}
			}
			literalDataGenerator.close();

			signatureGenerator.generate().encode(bOut);

			compressedDataGenerator.close();
		}
	}

	PGPSecretKey readSecretKey(InputStream input) throws IOException, PGPException {
		PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input),
				new JcaKeyFingerprintCalculator());

		//
		// we just loop through the collection till we find a key suitable for
		// encryption, in the real
		// world you would probably want to be a bit smarter about this.
		//

		Iterator< ? > keyRingIter = pgpSec.getKeyRings();
		while (keyRingIter.hasNext()) {
			PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRingIter.next();

			Iterator< ? > keyIter = keyRing.getSecretKeys();
			while (keyIter.hasNext()) {
				PGPSecretKey key = (PGPSecretKey) keyIter.next();

				if (key.isSigningKey()) {
					return key;
				}
			}
		}

		throw new IllegalArgumentException("Can't find signing key in key ring.");
	}
}