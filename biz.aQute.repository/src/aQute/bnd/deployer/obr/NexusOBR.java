package aQute.bnd.deployer.obr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import aQute.bnd.deployer.repository.AbstractIndexedRepo;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.lib.base64.Base64;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;

/**
 * A read-write nexus OBR-based repository.
 * <p>
 * You will need to install the nexus-obr-plugin in nexus (you can download it
 * on <a href="http://search.maven.org/#search|ga|1|a%3A%22nexus-obr-plugin%22">
 * maven central</a>)
 * </p>
 * <p>
 * <h2>Properties</h2>
 * <ul>
 * <li><b>repositoryUrl</b>: the nexus repository url
 * (http://localhost:8081/nexus/content/repositories/obr/ for example)</li>
 * <li><b>username</b>: the username; defaults to "deployment"</li>
 * <li><b>password</b>: the password; defaults to "deployment123"</li>
 * <li><b>name</b>: repository name; defaults to the nexus repository url</li>
 * <li><b>cache</b>: local cache directory. May be omitted, in which case a
 * default directory will be used.</li>
 * <li><b>readonly</b>: if readonly, no bundle can be added to the repository
 * </ul>
 * </p>
 * <p>
 * <h2>Example</h2>
 * 
 * <pre>
 *  -plugin:
 * aQute.bnd.deployer.obr.NexusOBR;readonly=false;repositoryUrl=http://localhost
 * :8081/nexus/content/repositories/obr/;username=deployment;password=
 * deployment123;name=nexus-obr
 * </pre>
 * </p>
 * 
 * @author Cedric Chabanois <cchabanois at gmail.com>
 */
public class NexusOBR extends AbstractIndexedRepo {
	static final int			BUFFER_SIZE				= IOConstants.PAGE_SIZE * 2;

	private static final String	DEFAULT_PASSWORD		= "deployment123";
	private static final String	DEFAULT_USERNAME		= "deployment";
	private static final String	EMPTY_REPOSITORY_URL	= "";
	private static final String	DEFAULT_CACHE_DIR		= ".bnd" + File.separator + "cache";
	public static final String	PROP_CACHE				= "cache";
	public static final String	PROP_REPOSITORY_URL		= "repositoryUrl";
	public static final String	PROP_READONLY			= "readonly";
	public static final String	PROP_USERNAME			= "username";
	public static final String	PROP_PASSWORD			= "password";
	public static final String	PROP_PUTURL				= "puturl";
	protected File				cacheDir				= new File(
			System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);
	private String				nexusRepositoryUrl;
	private boolean				readOnly;
	private String				username				= DEFAULT_USERNAME;
	private String				password				= DEFAULT_PASSWORD;
	/**
	 * if null, nexusRepositoryUrl is used for puts.
	 */
	private String				puturl					= null;

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(map);
		readOnly = Boolean.parseBoolean(map.get(PROP_READONLY));
		if (map.containsKey(PROP_USERNAME)) {
			username = map.get(PROP_USERNAME);
		}
		if (map.containsKey(PROP_PASSWORD)) {
			password = map.get(PROP_PASSWORD);
		}
		nexusRepositoryUrl = map.get(PROP_REPOSITORY_URL);
		if (nexusRepositoryUrl != null && !nexusRepositoryUrl.endsWith("/")) {
			nexusRepositoryUrl = nexusRepositoryUrl + '/';
		}
		if (map.containsKey(PROP_PUTURL)) {
			puturl = map.get(PROP_PUTURL);
		}
		if (puturl != null && !puturl.endsWith("/")) {
			puturl = puturl + '/';
		}

		String cachePath = map.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory())
				try {
					throw new IllegalArgumentException(String.format(
							"Cache path '%s' does not exist, or is not a directory.", cacheDir.getCanonicalPath()));
				} catch (IOException e) {
					throw new IllegalArgumentException("Could not get cacheDir canonical path", e);
				}
		}
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	public File getCacheDirectory() {
		return cacheDir;
	}

	public String getLocation() {
		if (nexusRepositoryUrl == null)
			return EMPTY_REPOSITORY_URL;
		else
			return nexusRepositoryUrl;

	}

	@Override
	protected List<URI> loadIndexes() throws Exception {
		List<URI> result;
		if (nexusRepositoryUrl != null) {
			result = new ArrayList<>();

			result.add(new URL(nexusRepositoryUrl + ".meta/obr.xml").toURI());
		} else {
			result = Collections.emptyList();
		}
		return result;
	}

	public void setCacheDirectory(File cacheDir) {
		if (cacheDir == null)
			throw new IllegalArgumentException("null cache directory not permitted");
		this.cacheDir = cacheDir;
	}

	@Override
	public synchronized PutResult put(InputStream stream, PutOptions options) throws Exception {
		/* determine if the put is allowed */
		if (readOnly) {
			throw new IOException("Repository is read-only");
		}

		if (options == null)
			options = DEFAULTOPTIONS;

		/* both parameters are required */
		if (stream == null)
			throw new IllegalArgumentException("No stream and/or options specified");

		/*
		 * setup a new stream that encapsulates the stream and calculates (when
		 * needed) the digest
		 */
		DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"));

		File tmpFile = null;
		try {
			/*
			 * copy the artifact from the (new/digest) stream into a temporary
			 * file in the root directory of the repository
			 */
			tmpFile = IO.createTempFile(null, "put", ".bnd");
			IO.copy(dis, tmpFile);

			/* beforeGet the digest if available */
			byte[] disDigest = dis.getMessageDigest().digest();

			if (options.digest != null && !Arrays.equals(options.digest, disDigest))
				throw new IOException("Retrieved artifact digest doesn't match specified digest");

			/* put the artifact into the repository (from the temporary file) */
			URL url = putArtifact(tmpFile);

			PutResult result = new PutResult();
			if (url != null) {
				result.digest = disDigest;
				result.artifact = url.toURI();
			}

			return result;
		} finally {
			if (tmpFile != null && tmpFile.exists()) {
				IO.delete(tmpFile);
			}
		}
	}

	protected URL putArtifact(File tmpFile) throws Exception {
		assert (tmpFile != null);
		assert (tmpFile.isFile());

		init();

		Version version;
		String bsn;
		try (Jar jar = new Jar(tmpFile)) {
			bsn = jar.getBsn();
			if (bsn == null || !Verifier.isBsn(bsn))
				throw new IllegalArgumentException(
						"Jar does not have a " + Constants.BUNDLE_SYMBOLICNAME + " manifest header");

			String versionString = jar.getVersion();
			if (versionString == null)
				versionString = "0";
			else if (!Verifier.isVersion(versionString))
				throw new IllegalArgumentException("Invalid version " + versionString + " in file " + tmpFile);

			version = Version.parseVersion(versionString);
		}
		URL url = put(tmpFile, bsn, version);
		reset();
		return url;
	}

	protected URL put(File file, String bsn, Version version) throws IOException {
		URL url = getTargetURL(bsn, version);
		try (InputStream is = IO.stream(file)) {
			HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
			httpUrlConnection.setDoOutput(true);
			httpUrlConnection.setFixedLengthStreamingMode((int) file.length());
			httpUrlConnection.setRequestMethod("PUT");
			if (username != null && password != null) {
				String userPassword = username + ":" + password;
				httpUrlConnection.setRequestProperty("Authorization",
						"Basic " + Base64.encodeBase64(userPassword.getBytes(UTF_8)));
			}

			try (OutputStream out = httpUrlConnection.getOutputStream()) {
				byte[] buffer = new byte[BUFFER_SIZE];
				while (true) {
					int length = is.read(buffer);
					if (length < 0)
						break;
					out.write(buffer, 0, length);
				}
				int respondeCode = httpUrlConnection.getResponseCode();
				// response code will be 201 (Created) if new bundle is
				// successfully
				// added
				if (respondeCode < 200 || respondeCode > 300) {
					throw new IOException(httpUrlConnection.getResponseMessage());
				}
			} finally {
				httpUrlConnection.disconnect();
			}
		}
		return url;
	}

	@Override
	public synchronized String getName() {
		if (name != null && !name.equals(this.getClass().getName()))
			return name;

		return nexusRepositoryUrl;
	}

	private URL getTargetURL(String bsn, Version version) throws MalformedURLException {
		String url = puturl != null ? puturl : nexusRepositoryUrl;
		return new URL(url + bsn + "/" + bsn + "-" + version + ".jar");
	}

	public File getRoot() {
		return cacheDir;
	}

}
