package aQute.bnd.libsync;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.prefs.*;

import aQute.bnd.service.*;
import aQute.bnd.settings.*;
import aQute.lib.base64.Base64;
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.osgi.*;
import aQute.libg.cafs.*;
import aQute.libg.cryptography.*;
import aQute.libg.cryptography.Signer;
import aQute.libg.cryptography.Verifier;
import aQute.libg.version.*;

public class Libsync implements RepositoryPlugin {
	Processor			processor;
	Server				server;
	final File			home		= new File(System.getProperty("user.home"));
	final File			bnd			= new File(home, ".bnd");
	final File			cafsdir		= new File(bnd, "cafs");
	final File			libsyncdir	= new File(bnd, "libsync");
	final CAFS			cafs;
	final FileRepo		repo		= new FileRepo("libsync", libsyncdir, true);
	final Settings		settings	= new Settings();
	KeyStore			keystore	= KeyStore.getInstance(KeyStore.getDefaultType());
	final Preferences	preferences	= Preferences.userNodeForPackage(Libsync.class);

	public Libsync() throws Exception {
		cafs = new CAFS(cafsdir, true);
	}

	public boolean canWrite() {
		return true;
	}

	public File[] get(String bsn, String range) throws Exception {
		return null;
	}

	public String getName() {
		return "LIBSYNC";
	}

	public List<String> list(String regex) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public File put(Jar jar) throws Exception {
		Jar request = new Jar("request");
		try {
			jar.calcChecksums(new String[] { "MD5", "SHA1" });
			Manifest manifest = jar.getManifest();
			request.setManifest(manifest);
			request.setDoNotTouchManifest();

			sign(jar, manifest);

			Jar result = server.send("putc", request);
			if (!errors(result)) {
				Resource r = result.getResource("META-INF/missing");

				if (r != null) {
					// The dest is missing paths, get those
					// and send them.
					LineCollection lc = new LineCollection(r.openInputStream());
					while (lc.hasNext()) {
						String path = lc.next();
						Resource missingResource = jar.getResource(path);
						request.putResource(path, missingResource);
					}

					result = server.send("put", request);
					if (!errors(result)) {
						// failed
					}
				}
				verify(result, manifest);

				manifest = request.getManifest();
				for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
					Attributes attrs = entry.getValue();
					String s = attrs.getValue("SHA1-Digest");
					byte digest[] = Base64.decodeBase64(s);

					SHA1 sha1 = new SHA1(digest);
					if (!cafs.exists(sha1.digest())) {
						r = jar.getResource(entry.getKey());
						SHA1 stored = cafs.write(r.openInputStream());
						assert sha1.equals(stored);
					}
				}
				jar.setManifest(manifest);
				jar.setDoNotTouchManifest();
				return repo.put(jar);
			}
		} finally {
			request.close();
		}
		return null;
	}

	private boolean verify(Jar result, Manifest manifest) throws Exception {
		int n = 0;
		Map<String, Resource> map = result.getDirectories().get("META-INF");
		for( Map.Entry<String, Resource> entry : map.entrySet()) {
			if ( entry.getKey().endsWith(".signer")) {
				SHA1 sha1 = SHA1.getDigester().from(entry.getValue().openInputStream());
				PublicKey publicKey = settings.getPublicKey();
				Verifier v = Crypto.verifier(publicKey, sha1);
				manifest.write(v);
				if ( !v.verify())
					return false;
				
			}
			n++;
		}
		return n > 0;
	}

	/**
	 * @param jar
	 * @param manifest
	 * @throws Exception
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void sign(Jar jar, Manifest manifest) throws Exception, NoSuchAlgorithmException,
			IOException {
		PrivateKey privateKey = settings.getPrivateKey();
		Signer s = Crypto.signer(privateKey, SHA1.getDigester());
		manifest.write(s);

		jar.putResource("META-INF/" + Crypto.toString(settings.getPublicKey()) + ".signature",
				new EmbeddedResource(s.signature().digest(), 0));
	}

	/**
	 * @param result
	 * @throws IOException
	 */
	private boolean errors(Jar result) throws IOException {
		 Resource errors = result.getResource("META-INF/errors");
		 Resource warnings = result.getResource("META-INF/warnings");
//		 if (errors != null)
//		 parse(errors.openInputStream(), url.getFile(), getErrors());
//		 if (warnings != null)
//		 parse(warnings.openInputStream(), url.getFile(), getWarnings());
		return errors != null || warnings != null;
	}

	public List<Version> versions(String bsn) throws Exception {
		return null;
	}

	public File get(String bsn, String version, Strategy strategy) throws Exception {
		Jar result = server.send("get/"+bsn+"/"+version+"/"+strategy, null);
		if (!errors(result)) {
			List<String> missing = new ArrayList<String>();
			Manifest manifest = result.getManifest();
			for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
				Attributes attrs = entry.getValue();
				String s = attrs.getValue("SHA1-Digest");
				byte digest[] = Base64.decodeBase64(s);

				if (!cafs.exists(digest)) {
					missing.add(entry.getKey());
				}
			}
			
//			IO.collect
//			result.putResource("META-INF/missing", s);
			
		}
		return null;
	}

}
