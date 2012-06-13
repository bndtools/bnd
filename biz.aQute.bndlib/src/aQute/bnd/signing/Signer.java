package aQute.bnd.signing;

import java.io.*;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.base64.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;

/**
 * This class is used with the aQute.lib.osgi package, it signs jars with DSA
 * signature. -sign: md5, sha1
 */
public class Signer extends Processor {
	static Pattern	METAINFDIR		= Pattern.compile("META-INF/[^/]*");
	String			digestNames[]	= new String[] {
										"MD5"
									};
	File			keystoreFile	= new File("keystore");
	String			password;
	String			alias;

	public void signJar(Jar jar) {
		if (digestNames == null || digestNames.length == 0)
			error("Need at least one digest algorithm name, none are specified");

		if (keystoreFile == null || !keystoreFile.getAbsoluteFile().exists()) {
			error("No such keystore file: " + keystoreFile);
			return;
		}

		if (alias == null) {
			error("Private key alias not set for signing");
			return;
		}

		MessageDigest digestAlgorithms[] = new MessageDigest[digestNames.length];

		getAlgorithms(digestNames, digestAlgorithms);

		try {
			Manifest manifest = jar.getManifest();
			manifest.getMainAttributes().putValue("Signed-By", "Bnd");

			// Create a new manifest that contains the
			// Name parts with the specified digests

			ByteArrayOutputStream o = new ByteArrayOutputStream();
			manifest.write(o);
			doManifest(jar, digestNames, digestAlgorithms, o);
			o.flush();
			byte newManifestBytes[] = o.toByteArray();
			jar.putResource("META-INF/MANIFEST.MF", new EmbeddedResource(newManifestBytes, 0));

			// Use the bytes from the new manifest to create
			// a signature file

			byte[] signatureFileBytes = doSignatureFile(digestNames, digestAlgorithms, newManifestBytes);
			jar.putResource("META-INF/BND.SF", new EmbeddedResource(signatureFileBytes, 0));

			// Now we must create an RSA signature
			// this requires the private key from the keystore

			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

			KeyStore.PrivateKeyEntry privateKeyEntry = null;

			java.io.FileInputStream keystoreInputStream = null;
			try {
				keystoreInputStream = new java.io.FileInputStream(keystoreFile);
				char[] pw = password == null ? new char[0] : password.toCharArray();

				keystore.load(keystoreInputStream, pw);
				keystoreInputStream.close();
				privateKeyEntry = (PrivateKeyEntry) keystore.getEntry(alias, new KeyStore.PasswordProtection(pw));
			}
			catch (Exception e) {
				error("No able to load the private key from the give keystore(" + keystoreFile.getAbsolutePath()
						+ ") with alias " + alias + " : " + e);
				return;
			}
			finally {
				IO.close(keystoreInputStream);
			}
			PrivateKey privateKey = privateKeyEntry.getPrivateKey();

			Signature signature = Signature.getInstance("MD5withRSA");
			signature.initSign(privateKey);

			signature.update(signatureFileBytes);

			signature.sign();

			// TODO, place the SF in a PCKS#7 structure ...
			// no standard class for this? The following
			// is an idea but we will to have do ASN.1 BER
			// encoding ...

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			jar.putResource("META-INF/BND.RSA", new EmbeddedResource(tmpStream.toByteArray(), 0));
		}
		catch (Exception e) {
			error("During signing: " + e);
		}
	}

	private byte[] doSignatureFile(String[] digestNames, MessageDigest[] algorithms, byte[] manbytes)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter ps = IO.writer(out);
		ps.print("Signature-Version: 1.0\r\n");

		for (int a = 0; a < algorithms.length; a++) {
			if (algorithms[a] != null) {
				byte[] digest = algorithms[a].digest(manbytes);
				ps.print(digestNames[a] + "-Digest-Manifest: ");
				ps.print(new Base64(digest));
				ps.print("\r\n");
			}
		}
		return out.toByteArray();
	}

	private void doManifest(Jar jar, String[] digestNames, MessageDigest[] algorithms, OutputStream out)
			throws Exception {

		for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
			String name = entry.getKey();
			if (!METAINFDIR.matcher(name).matches()) {
				out.write("\r\n".getBytes());
				out.write("Name: ".getBytes());
				out.write(name.getBytes("UTF-8"));
				out.write("\r\n".getBytes());

				digest(algorithms, entry.getValue());
				for (int a = 0; a < algorithms.length; a++) {
					if (algorithms[a] != null) {
						byte[] digest = algorithms[a].digest();
						String header = digestNames[a] + "-Digest: " + new Base64(digest) + "\r\n";
						out.write(header.getBytes());
					}
				}
			}
		}
	}

	private void digest(MessageDigest[] algorithms, Resource r) throws Exception {
		InputStream in = r.openInputStream();
		byte[] data = new byte[1024];
		int size = in.read(data);
		while (size > 0) {
			for (int a = 0; a < algorithms.length; a++) {
				if (algorithms[a] != null) {
					algorithms[a].update(data, 0, size);
				}
			}
			size = in.read(data);
		}
	}

	private void getAlgorithms(String[] digestNames, MessageDigest[] algorithms) {
		for (int i = 0; i < algorithms.length; i++) {
			String name = digestNames[i];
			try {
				algorithms[i] = MessageDigest.getInstance(name);
			}
			catch (NoSuchAlgorithmException e) {
				error("Specified digest algorithm " + digestNames[i] + ", but not such algorithm was found: " + e);
			}
		}
	}

	public void setPassword(String string) {
		password = string;
	}

	public void setKeystore(File keystore) {
		this.keystoreFile = keystore;
	}

	public void setAlias(String string) {
		this.alias = string;
	}
}
