package aQute.bnd.maven.support;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.FutureTask;

import aQute.bnd.osgi.Constants;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.filelock.DirectoryLock;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * An entry (a group/artifact) in the maven cache in the .m2/repository
 * directory. It provides methods to get the pom and the artifact.
 */
public class MavenEntry implements Closeable {
	final Maven					maven;
	final File					root;
	final File					dir;
	final String				path;
	final DirectoryLock			lock;
	final Map<URI, CachedPom>	poms	= new HashMap<>();
	final File					pomFile;
	final File					artifactFile;
	final String				pomPath;
	final File					propertiesFile;
	final Reporter				reporter;
	UTF8Properties				properties;
	private boolean				propertiesChanged;
	FutureTask<File>			artifact;
	String						artifactPath;

	/**
	 * Constructor.
	 *
	 * @param maven
	 * @param path
	 */
	MavenEntry(Maven maven, String path) {
		this(maven, path, new ReporterAdapter());
	}

	/**
	 * Constructor.
	 *
	 * @param maven
	 * @param path
	 * @param reporter
	 */
	MavenEntry(Maven maven, String path, Reporter reporter) {
		this.root = maven.repository;
		this.maven = maven;
		this.path = path;
		this.pomPath = path + ".pom";
		this.artifactPath = path + ".jar";
		this.dir = IO.getFile(maven.repository, path)
			.getParentFile();
		try {
			IO.mkdirs(this.dir);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		this.pomFile = new File(maven.repository, pomPath);
		this.artifactFile = new File(maven.repository, artifactPath);
		this.propertiesFile = new File(dir, "bnd.properties");
		this.lock = new DirectoryLock(dir, 5 * 60000); // 5 mins
		this.reporter = reporter;
	}

	public File getArtifactFile() {
		return artifactFile;
	}

	/**
	 * This is the method to get the POM for a cached entry.
	 *
	 * @param urls The allowed URLs
	 * @return a CachedPom for this maven entry
	 * @throws Exception If something goes haywire
	 */
	public CachedPom getPom(URI[] urls) throws Exception {

		// First check if we have the pom cached in memory
		synchronized (this) {
			// Try to find it in the in-memory cache
			for (URI url : urls) {
				CachedPom pom = poms.get(url);
				if (pom != null)
					return pom;
			}
		}

		// Ok, we need to see if it exists on disk

		// lock.lock();
		try {

			if (isValid()) {
				// Check if one of our repos had the correct file.
				for (URI url : urls) {
					String valid = getProperty(url.toASCIIString());
					if (valid != null)
						return createPom(url);
				}

				// we have the info, but have to verify that it
				// exists in one of our repos but we do not have
				// to download it as our cache is already ok.
				for (URI url : urls) {
					if (verify(url, pomPath)) {
						return createPom(url);
					}
				}

				// It does not exist in out repo
				// so we have to fail even though we do have
				// the file.

			} else {
				IO.mkdirs(dir);
				// We really do not have the file
				// so we have to find out who has it.
				for (final URI url : urls) {

					if (download(url, pomPath)) {
						if (verify(url, pomPath)) {
							artifact = new FutureTask<>(() -> {
								if (download(url, artifactPath)) {
									verify(url, artifactPath);
								}
								return artifactFile;
							});
							maven.executor.execute(artifact);
							return createPom(url);
						}
					}
				}
			}
			return null;
		} finally {
			saveProperties();
			// lock.release();
		}
	}

	/**
	 * Download a resource from the given repo.
	 *
	 * @param url The base url for the repo
	 * @param path The path part
	 * @throws MalformedURLException
	 */
	boolean download(URI repo, String path) throws MalformedURLException {
		try {
			URL url = toURL(repo, path);
			System.err.println("Downloading " + repo + " path " + path + " url " + url);
			File file = new File(root, path);
			IO.copy(url.openStream(), file);
			System.err.println("Downloaded " + url);
			return true;
		} catch (Exception e) {
			System.err.println("debug: " + e);
			return false;
		}
	}

	/**
	 * Converts a repo + path to a URL..
	 *
	 * @param base The base repo
	 * @param path The path in the directory + url
	 * @return a URL that points to the file in the repo
	 * @throws MalformedURLException
	 */
	URL toURL(URI base, String path) throws MalformedURLException {
		StringBuilder r = new StringBuilder();
		r.append(base.toString());
		if (r.charAt(r.length() - 1) != '/')
			r.append('/');
		r.append(path);
		return new URL(r.toString());
	}

	/**
	 * Check if this is a valid cache directory, might probably need some more
	 * stuff.
	 *
	 * @return true if valid
	 */
	private boolean isValid() {
		return pomFile.isFile() && pomFile.length() > 100 && artifactFile.isFile() && artifactFile.length() > 100;
	}

	/**
	 * We maintain a set of bnd properties in the cache directory.
	 *
	 * @param key The key for the property
	 * @param value The value for the property
	 */
	private void setProperty(String key, String value) {
		Properties properties = getProperties();
		properties.setProperty(key, value);
		propertiesChanged = true;
	}

	/**
	 * Answer the properties, loading if needed.
	 */
	protected Properties getProperties() {
		if (properties == null) {
			properties = new UTF8Properties();
			File props = new File(dir, "bnd.properties");
			if (props.exists()) {
				try {
					properties.load(props, null, Constants.OSGI_SYNTAX_HEADERS);
				} catch (Exception e) {
					// we ignore for now, will handle it on safe
				}
			}
		}
		return properties;
	}

	/**
	 * Answer a property.
	 *
	 * @param key The key
	 * @return The value
	 */
	private String getProperty(String key) {
		Properties properties = getProperties();
		return properties.getProperty(key);
	}

	private void saveProperties() throws IOException {
		if (propertiesChanged) {
			try (OutputStreamWriter osw = new OutputStreamWriter(IO.outputStream(propertiesFile))) {
				properties.store(osw, "");
			} finally {
				properties = null;
				propertiesChanged = false;
			}
		}
	}

	/**
	 * Help function to create the POM and record its source.
	 *
	 * @param url the repo from which it was constructed
	 * @return the new pom
	 * @throws Exception
	 */
	private CachedPom createPom(URI url) throws Exception {
		CachedPom pom = new CachedPom(this, url, reporter);
		pom.parse();
		poms.put(url, pom);
		setProperty(url.toASCIIString(), "true");
		return pom;
	}

	/**
	 * Verify that the repo has a checksum file for the given path and that this
	 * checksum matchs.
	 *
	 * @param repo The repo
	 * @param path The file id
	 * @return true if there is a digest and it matches one of the algorithms
	 * @throws Exception
	 */
	boolean verify(URI repo, String path) throws Exception {
		for (String algorithm : Maven.ALGORITHMS) {
			if (verify(repo, path, algorithm))
				return true;
		}
		return false;
	}

	/**
	 * Verify the path against its digest for the given algorithm.
	 *
	 * @param repo
	 * @param path
	 * @param algorithm
	 * @throws Exception
	 */
	private boolean verify(URI repo, String path, String algorithm) throws Exception {
		String digestPath = path + "." + algorithm;
		File actualFile = new File(root, path);

		if (download(repo, digestPath)) {
			File digestFile = new File(root, digestPath);
			final MessageDigest md = MessageDigest.getInstance(algorithm);
			IO.copy(actualFile, md);
			byte[] digest = md.digest();
			String source = IO.collect(digestFile)
				.toUpperCase();
			String hex = Hex.toHexString(digest)
				.toUpperCase();
			if (source.startsWith(hex)) {
				System.err.println("Verified ok " + actualFile + " digest " + algorithm);
				return true;
			}
		}
		System.err.println("Failed to verify " + actualFile + " for digest " + algorithm);
		return false;
	}

	public File getArtifact() throws Exception {
		if (artifact == null)
			return artifactFile;
		return artifact.get();
	}

	public File getPomFile() {
		return pomFile;
	}

	@Override
	public void close() throws IOException {

	}

	public void remove() {
		if (dir.getParentFile() != null) {
			IO.delete(dir);
		}
	}

}
