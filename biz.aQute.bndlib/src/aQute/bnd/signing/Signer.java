package aQute.bnd.signing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.base64.Base64;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;

/**
 * This class is used with the aQute.bnd.osgi package, it signs jars with DSA
 * signature. -sign: md5, sha1
 */
public class Signer extends Processor {
	static final int				BUFFER_SIZE		= IOConstants.PAGE_SIZE * 1;

	private final static Pattern	METAINFDIR		= Pattern.compile("META-INF/[^/]*");
	String							digestNames[]	= new String[] {
		"MD5"
	};
	File							keystoreFile	= new File("keystore");
	String							password;
	String							alias;

	public void signJar(Jar jar) {
		if (digestNames == null || digestNames.length == 0)
			error("Need at least one digest algorithm name, none are specified");

		if (keystoreFile == null || !keystoreFile.getAbsoluteFile()
			.exists()) {
			error("No such keystore file: %s", keystoreFile);
			return;
		}

		if (alias == null) {
			error("Private key alias not set for signing");
			return;
		}

		MessageDigest digestAlgorithms[] = new MessageDigest[digestNames.length];

		getAlgorithms(digestNames, digestAlgorithms);

		try (ByteBufferOutputStream o = new ByteBufferOutputStream()) {
			Manifest manifest = jar.getManifest();
			manifest.getMainAttributes()
				.putValue("Signed-By", "Bnd");

			// Create a new manifest that contains the
			// Name parts with the specified digests

			manifest.write(o);
			doManifest(jar, digestNames, digestAlgorithms, o);
			o.flush();
			byte newManifestBytes[] = o.toByteArray();
			jar.putResource("META-INF/MANIFEST.MF", new EmbeddedResource(newManifestBytes, 0L));

			// Use the bytes from the new manifest to create
			// a signature file

			byte[] signatureFileBytes = doSignatureFile(digestNames, digestAlgorithms, newManifestBytes);
			jar.putResource("META-INF/BND.SF", new EmbeddedResource(signatureFileBytes, 0L));

			// Now we must create an RSA signature
			// this requires the private key from the keystore

			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

			KeyStore.PrivateKeyEntry privateKeyEntry = null;

			try (InputStream keystoreInputStream = IO.stream(keystoreFile)) {
				char[] pw = password == null ? new char[0] : password.toCharArray();

				keystore.load(keystoreInputStream, pw);
				keystoreInputStream.close();
				privateKeyEntry = (PrivateKeyEntry) keystore.getEntry(alias, new KeyStore.PasswordProtection(pw));
			} catch (Exception e) {
				exception(e, "Not able to load the private key from the given keystore(%s) with alias %s",
					keystoreFile.getAbsolutePath(), alias);
				return;
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

			try (ByteBufferOutputStream tmpStream = new ByteBufferOutputStream()) {
				jar.putResource("META-INF/BND.RSA", new EmbeddedResource(tmpStream.toByteArray(), 0L));
			}
		} catch (Exception e) {
			exception(e, "During signing: %s", e);
		}
	}

	private byte[] doSignatureFile(String[] digestNames, MessageDigest[] algorithms, byte[] manbytes)
		throws IOException {
		try (ByteBufferOutputStream out = new ByteBufferOutputStream(); PrintWriter ps = IO.writer(out)) {
			ps.print("Signature-Version: 1.0\r\n");

			for (int a = 0; a < algorithms.length; a++) {
				if (algorithms[a] != null) {
					byte[] digest = algorithms[a].digest(manbytes);
					ps.print(digestNames[a] + "-Digest-Manifest: ");
					ps.print(new Base64(digest));
					ps.print("\r\n");
				}
			}
			ps.flush();
			return out.toByteArray();
		}
	}

	private void doManifest(Jar jar, String[] digestNames, MessageDigest[] algorithms, OutputStream out)
		throws Exception {
		Writer w = IO.writer(out, UTF_8);
		try {
			for (Map.Entry<String, Resource> entry : jar.getResources()
				.entrySet()) {
				String name = entry.getKey();
				if (!METAINFDIR.matcher(name)
					.matches()) {
					w.write("\r\n");
					w.write("Name: ");
					w.write(name);
					w.write("\r\n");

					digest(algorithms, entry.getValue());
					for (int a = 0; a < algorithms.length; a++) {
						if (algorithms[a] != null) {
							byte[] digest = algorithms[a].digest();
							String header = digestNames[a] + "-Digest: " + new Base64(digest) + "\r\n";
							w.write(header);
						}
					}
				}
			}
		} finally {
			w.flush();
		}
	}

	private void digest(MessageDigest[] algorithms, Resource r) throws Exception {
		try (InputStream in = r.openInputStream()) {
			byte[] data = new byte[BUFFER_SIZE];
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
	}

	private void getAlgorithms(String[] digestNames, MessageDigest[] algorithms) {
		for (int i = 0; i < algorithms.length; i++) {
			String name = digestNames[i];
			try {
				algorithms[i] = MessageDigest.getInstance(name);
			} catch (NoSuchAlgorithmException e) {
				exception(e, "Specified digest algorithm %s, but not such algorithm was found", digestNames[i]);
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
