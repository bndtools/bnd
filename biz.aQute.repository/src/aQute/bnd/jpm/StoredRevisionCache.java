package aQute.bnd.jpm;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import javax.net.ssl.*;

import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.lib.settings.*;
import aQute.rest.urlclient.*;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.RevisionRef;

public class StoredRevisionCache {
	private static TrustManager[]	trustAllCerts;
	private static HostnameVerifier	trustAnyHost;
	private static SSLContext		sslContext;
	private static SSLSocketFactory	sslSocketFactory;
	private final File				root;
	final File						tmpdir;
	final File						repodir;
	final File						programdir;
	final Settings					settings;
	boolean							sign;
	final Map<File,Exception>		errors		= new HashMap<File,Exception>();
	private Map<String,Program>		programs	= new HashMap<String,Program>();
	private static final JSONCodec	codec		= new JSONCodec();
	File							refresh;
	final URLClient					urlc;

	public StoredRevisionCache(File root, Settings settings) throws Exception {
		this.root = root;
		this.settings = settings;
		this.getRoot().mkdirs();
		this.tmpdir = new File(root, "tmp");
		this.tmpdir.mkdir();
		this.repodir = new File(root, "shas");
		this.repodir.mkdir();
		this.programdir = new File(root, "programs");
		this.programdir.mkdir();
		this.refresh = new File(root, ".refreshed");
		if (!this.refresh.isFile())
			this.refresh.createNewFile();
		urlc = new URLClient("");
		if (settings != null)
			urlc.credentials(settings.getEmail(), InetAddress.getLocalHost().toString(), settings.getPublicKey(),
					settings.getPrivateKey());
		else
			System.out.println("no settings");
	}

	/*
	 * We create a path that ends in a readable name since Eclipse shows the
	 * last segement. The name is superfluous
	 */

	public File getPath(String bsn, String version, byte[] sha) {
		return getPath(bsn, version, sha, false);
	}

	public File getPath(String bsn, String version, byte[] sha, boolean withsource) {
		if (withsource)
			return IO.getFile(repodir, Hex.toHexString(sha) + "/+" + bsn + "-" + version + ".jar");
		else
			return IO.getFile(repodir, Hex.toHexString(sha) + "/" + bsn + "-" + version + ".jar");
	}

	/*
	 * Delete a revision
	 */
	public void delete(byte[] sha) {
		File f = IO.getFile(getRoot(), Hex.toHexString(sha));
		IO.delete(f);
	}

	/*
	 * Actually download a file. First in a temp directory, then it is renamed
	 */
	public void download(final File file, final Set<URI> urls, byte[] sha) throws Exception {
		if (errors.containsKey(file))
			throw errors.get(file);

		if (urls.isEmpty())
			throw new Exception("No URLs to download " + file);

		File tmp = IO.createTempFile(tmpdir, "tmp", ".tmp");
		for (URI url : urls) {

			// Fixup for older bug that put the file url in the urls :-(

			try {
				URLConnection connection = getConnection(url);

				MessageDigest digest = MessageDigest.getInstance("SHA1");
				DigestInputStream din = new DigestInputStream(connection.getInputStream(), digest);
				IO.copy(din, tmp);

				byte[] receivedsha = digest.digest();

				if (!Arrays.equals(sha, receivedsha))
					throw new Exception("Shas did not match (expected)" + Hex.toHexString(sha) + " (downloaded)" + tmp
							+ " (" + Hex.toHexString(receivedsha) + ")");

				file.getParentFile().mkdirs();
				if (!tmp.renameTo(file)) {
					// We must have had a race condition
					// on a system where we have not destructive
					// rename. Since we are in a SHA name dir
					// the content must ALWAYS be the same. So if
					// we have a name clash we can throw away the newly download
					// one since we seem to have lost.
					tmp.delete();
				} else {
					long modified = connection.getLastModified();
					if (modified > 0)
						file.setLastModified(modified);
				}
				return;
			}
			catch (Exception e) {
				errors.put(file, e);
				throw e;
			}
			finally {
				tmp.delete();
			}
		}
	}

	public void add(RevisionRef d, File file) throws IOException {
		File path = getPath(d.bsn, d.version, d.revision);
		path.getParentFile().mkdirs();
		IO.copy(file, path);
		long modified = file.lastModified();
		if (modified > 0)
			path.setLastModified(modified);
	}

	private URLConnection getConnection(URI url) throws Exception {
		int count = 4;
		while (count-- > 0) {

			if (url.getScheme().equalsIgnoreCase("file")) {
				String s = url.toString();

				File f = IO.getFile(s.substring("FILE:".length()));
				url = f.toURI();
			}

			URLConnection urlc = url.toURL().openConnection();

			//
			// For testing, fixup file paths to make them relative
			//

			if (!(urlc instanceof HttpURLConnection)) {
				return urlc;
			}

			urlc.setConnectTimeout(30000);
			HttpURLConnection connection = (HttpURLConnection) urlc;
			authenticate(connection);
			if (connection.getResponseCode() / 100 == 2)
				return connection;

			if (connection.getResponseCode() == 301 || connection.getResponseCode() == 302) {
				String u = connection.getHeaderField("Location");
				if (u == null)
					throw new IOException("Had a url redirect but no location was set");
				url = new URI(u.trim());

			} else {

				throw new IOException("Could not open URL, [" + connection.getResponseCode() + "] "
						+ connection.getResponseMessage() + " for '" + connection.getURL() + "'");
			}

		}
		throw new IOException("Could not open URL, too many redirects ");
	}

	protected void authenticate(URLConnection connection) throws Exception {
		if (connection instanceof HttpURLConnection) {
			urlc.sign(connection);
		}
	}

	static void disableTrust(HttpsURLConnection httpsConnection) throws GeneralSecurityException {
		if (sslSocketFactory == null) {
			trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType)
							throws CertificateException {}

					public void checkClientTrusted(X509Certificate[] certs, String authType)
							throws CertificateException {}
				}
			};

			trustAnyHost = new HostnameVerifier() {
				public boolean verify(String string, SSLSession session) {
					return true;
				}
			};
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		}
		httpsConnection.setSSLSocketFactory(sslSocketFactory);
		httpsConnection.setHostnameVerifier(trustAnyHost);
	}

	public File getRoot() {
		return root;
	}

	public void refresh() {
		errors.clear();
		refresh.setLastModified(System.currentTimeMillis());
	}

	public void deleteAll() {
		IO.delete(root);
		root.mkdirs();
	}

	public Program getProgram(String bsn) {
		Program p = programs.get(bsn);
		if (p != null)
			return p;

		File pf = IO.getFile(programdir, bsn + ".json");
		if (pf != null && pf.isFile() && pf.lastModified() >= refresh.lastModified()) {
			try {
				p = codec.dec().from(pf).get(Program.class);
				programs.put(bsn, p);
				return p;
			}
			catch (Exception e) {
				//
				return null;
			}
		} else
			return null;
	}

	public void putProgram(String bsn, Program p) throws IOException, Exception {
		programs.put(bsn, p);
		File pf = IO.getFile(root, bsn + ".json");
		codec.enc().to(pf).put(p);
	}

	/**
	 * Check if a revision has sources ...
	 * 
	 * @param revision
	 * @return
	 */
	public boolean hasSources(String bsn, String version, byte[] sha) {
		return getPath(bsn, version, sha, true).isFile();
	}

	/**
	 * Remove the sources from the cache.
	 * 
	 * @param bsn
	 * @param string
	 * @param revision
	 */
	public void removeSources(String bsn, String version, byte[] sha) {
		getPath(bsn, version, sha, true).delete();
	}

}
