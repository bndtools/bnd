package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import aQute.libg.filelock.*;

/**
 * An entry in the maven cache in the .m2/repository directory. It provides
 * methods to get the pom and the artifact.
 * 
 */
public class MavenEntry implements Closeable {
	final Maven					maven;
	final File					dir;
	final String				path;
	final DirectoryLock			lock;
	Future<File>				artifact;
	final Map<URI, CachedPom>	poms	= new HashMap<URI, CachedPom>();

	MavenEntry(Maven maven, String path) {
		this.maven = maven;
		this.path = path;
		this.dir = IO.getFile(maven.repository, path).getParentFile();
		this.dir.mkdirs();
		this.lock = new DirectoryLock(dir, 5 * 60000); // 5 mins
	}

	public CachedPom getPom(URI[] urls) throws Exception {

		synchronized (this) {
			// Try to find it in the in-memory cache
			for (URI url : urls) {
				CachedPom pom = poms.get(url);
				if (pom != null)
					return pom;
			}
		}

		// lock.lock();
		try {
			try {
				return getPom0(urls);
			} catch (Exception e) {
				// If things fail, we delete the directory and
				// try one more time.
				remove0();
				return getPom0(urls);
			}
		} finally {
			// lock.release();
		}
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	public void remove() throws InterruptedException {
		lock.lock();
		try {
			remove0();
		} finally {
			lock.release();
		}
	}

	/**
	 * 
	 */
	private void remove0() {
		System.out.println("Removing " + dir);
		poms.clear();
		for (File sub : dir.listFiles()) {
			if (!sub.getName().equals(DirectoryLock.LOCKNAME))
				sub.delete();
		}
		poms.clear();
		artifact = null;
	}

	private synchronized CachedPom getPom0(URI... urls) throws Exception {
		String pomPath = this.path + ".pom";
		File pomFile = new File(maven.repository, pomPath);
		if (!pomFile.isFile()) {

			// Try to download it.
			for (final URI url : urls) {
				if (download(url, pomPath, pomFile, true)) {
					final String artifactPath = this.path + ".jar";
					final File artifact = new File(maven.repository, artifactPath);
					FutureTask<File> runnable = new FutureTask<File>(new Runnable() {
						public void run() {
							try {
								if (!download(url, artifactPath, artifact, true))
									throw new IllegalStateException("Could not properly download "
											+ artifact);
								System.out.println("download " + artifact);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					}, artifact);
//					maven.schedule(runnable);
					this.artifact = runnable;
					runnable.run();
					return createPom(url);
				}
			}
		} else {
			for (URI url : urls) {
				if (check(url, false)) {
					return createPom(url);
				}
			}
			for (URI url : urls) {
				if (check(url, true)) {
					return createPom(url);
				}
			}
		}
		throw new FileNotFoundException("Cannot find pom for " + pomPath + " in "
				+ Arrays.toString(urls));
	}

	private CachedPom createPom(URI url) throws Exception {
		File pomFile = IO.getFile(maven.repository, path + ".pom");
		CachedPom pom = new CachedPom(this, pomFile, url);
		pom.parse();
		poms.put(url, pom);
		return pom;
	}

	/**
	 * Check if the current download matches the download.
	 * @param url
	 * @param download
	 * @return
	 * @throws Exception
	 */
	private boolean check(URI url, boolean download) throws Exception {
		StringBuilder sb = new StringBuilder();
		String extform = url.normalize().toString();
		for (int i = 0; i < extform.length(); i++) {
			char c = extform.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-')
				sb.append(c);
			else
				sb.append('_');
		}
		sb.append(".pom.sha1");
		String key = sb.toString();

		File keyFile = new File(dir, key);
		if (keyFile.isFile())
			return true;

		if (download) {
			if (download(url, path + ".pom.sha1", keyFile, false)) {
				// TODO Verify the checksum?
				return true;
			}
		}
		keyFile.delete();
		return false;
	}

	public void close() throws IOException {
	}

	private boolean download(URI base, String path, File dest, boolean verify) throws Exception {
		try {
			StringBuilder r = new StringBuilder();
			r.append(base.toString());
			if (r.charAt(r.length() - 1) != '/')
				r.append('/');
			r.append(path);
			URL url = new URL(r.toString());

			System.out.println("Downloading " + url);
			IO.copy(url.openStream(), dest);
			if (verify) {
				File algFile = new File(dest.getAbsolutePath() + ".sha1");
				IO.copy(new URL(url.toExternalForm() + ".sha1").openStream(), algFile);
				if (!verify(dest, SHA1.getDigester())) {
					System.out.println("Not verified!");
					throw new IllegalStateException("could not verify");
				}
			}
			return true;
		} catch (Exception e) {
			System.out.println("Download failed due to " + e);
		}
		dest.delete();
		return false;
	}

	private boolean verify(File file, Digester<?> digester) throws Exception {
		File newFile = new File(file.getAbsolutePath() + "."
				+ digester.getAlgorithm().toLowerCase());
		if (!newFile.isFile()) {
			return false;
		}

		IO.copy(file, digester);
		digester.flush();
		byte[] digest = digester.getMessageDigest().digest();
		String s = IO.collect(newFile).toUpperCase();
		String hex = Hex.toHexString(digest).toUpperCase();
		if (s.startsWith(hex)) {
			System.out.println("Verified ok " + file);
			return true;
		}

		System.out.printf("Failed to verify %s checksum for %s\n", digester.getAlgorithm(), file);
		return false;
	}

	public File getArtifact() throws Exception {
		if (artifact != null) {
			File f = artifact.get();
			if ( ! f.isFile())
				System.out.println("ouch " + f);
		}
		
		return IO.getFile(maven.repository, path + ".jar");
	}
	
	

}
