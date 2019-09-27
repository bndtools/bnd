package aQute.bnd.deployer.repository;

import static aQute.bnd.deployer.repository.RepoConstants.DEFAULT_CACHE_DIR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.log.LogService;

import aQute.bnd.deployer.repository.api.IRepositoryContentProvider;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;

public class LocalIndexedRepo extends AbstractIndexedRepo implements Refreshable, Participant, Actionable {

	private final String			UPWARDS_ARROW			= " \u2191";
	private final String			DOWNWARDS_ARROW			= " \u2193";
	private static final Pattern	REPO_FILE				= Pattern
		.compile("([-.\\w]+)(-|_)([.\\d]+)(-[-\\w]+)?\\.(jar|lib)");
	private static final String		CACHE_PATH				= ".cache";
	public static final String		PROP_LOCAL_DIR			= "local";
	public static final String		PROP_READONLY			= "readonly";
	public static final String		PROP_PRETTY				= "pretty";
	public static final String		PROP_OVERWRITE			= "overwrite";
	public static final String		PROP_ONLYDIRS			= "onlydirs";

	@SuppressWarnings("deprecation")
	private boolean					readOnly;
	private boolean					pretty					= false;
	private boolean					overwrite				= true;
	private File					storageDir;
	private String					onlydirs				= null;

	// @GuardedBy("newFilesInCoordination")
	private final List<URI>			newFilesInCoordination	= new LinkedList<>();
	private static final String		EMPTY_LOCATION			= "";

	public static final String		PROP_LOCATIONS			= "locations";
	public static final String		PROP_CACHE				= "cache";

	private String					locations;
	protected File					cacheDir				= new File(
		System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);

	@SuppressWarnings("deprecation")
	@Override
	public synchronized void setProperties(Map<String, String> map) {
		super.setProperties(map);
		locations = map.get(PROP_LOCATIONS);
		String cachePath = map.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory())
				try {
					throw new IllegalArgumentException(String
						.format("Cache path '%s' does not exist, or is not a directory.", cacheDir.getCanonicalPath()));
				} catch (IOException e) {
					throw new IllegalArgumentException("Could not get cacheDir canonical path", e);
				}
		}

		// Load essential properties
		String localDirPath = map.get(PROP_LOCAL_DIR);
		if (localDirPath == null)
			throw new IllegalArgumentException(
				String.format("Attribute '%s' must be set on %s plugin.", PROP_LOCAL_DIR, getClass().getName()));

		storageDir = new File(localDirPath);
		if (storageDir.exists() && !storageDir.isDirectory())
			throw new IllegalArgumentException(
				String.format("Local path '%s' exists and is not a directory.", localDirPath));

		readOnly = Boolean.parseBoolean(map.get(PROP_READONLY));
		pretty = Boolean.parseBoolean(map.get(PROP_PRETTY));
		overwrite = map.get(PROP_OVERWRITE) == null ? true : Boolean.parseBoolean(map.get(PROP_OVERWRITE));
		onlydirs = map.get(PROP_ONLYDIRS);

		// Set the local index and cache directory locations
		cacheDir = new File(storageDir, CACHE_PATH);
		if (cacheDir.exists() && !cacheDir.isDirectory())
			throw new IllegalArgumentException(
				String.format("Cannot create repository cache: '%s' already exists but is not directory.",
					cacheDir.getAbsolutePath()));
	}

	@Override
	protected synchronized List<URI> loadIndexes() throws Exception {
		List<URI> remotes;
		try {
			if (locations != null)
				remotes = parseLocations(locations);
			else
				remotes = Collections.emptyList();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(
				String.format("Invalid location, unable to parse as URL list: %s", locations), e);
		}

		List<URI> indexes = new ArrayList<>(remotes.size() + generatingProviders.size());

		for (IRepositoryContentProvider contentProvider : generatingProviders) {
			File indexFile = getIndexFile(contentProvider);
			try {
				if (indexFile.exists()) {
					indexes.add(indexFile.toURI());
				} else {
					if (contentProvider.supportsGeneration()) {
						generateIndex(indexFile, contentProvider);
						indexes.add(indexFile.toURI());
					}
				}
			} catch (Exception e) {
				logService.log(LogService.LOG_ERROR,
					String.format("Unable to load/generate index file '%s' for repository type %s", indexFile,
						contentProvider.getName()),
					e);
			}
		}

		indexes.addAll(remotes);
		return indexes;
	}

	@Override
	public synchronized File getCacheDirectory() {
		return cacheDir;
	}

	public void setCacheDirectory(File cacheDir) {
		if (cacheDir == null)
			throw new IllegalArgumentException("null cache directory not permitted");
		this.cacheDir = cacheDir;
	}

	@Override
	public synchronized String getName() {
		if (name != null && !name.equals(this.getClass()
			.getName()))
			return name;

		return locations;
	}

	/**
	 * @param contentProvider the repository content provider
	 * @return the filename of the index on local storage
	 */
	private File getIndexFile(IRepositoryContentProvider contentProvider) {
		String indexFileName = contentProvider.getDefaultIndexName(pretty);
		File indexFile = new File(storageDir, indexFileName);
		return indexFile;
	}

	synchronized void regenerateAllIndexes() {
		for (IRepositoryContentProvider provider : generatingProviders) {
			if (!provider.supportsGeneration()) {
				logService.log(LogService.LOG_WARNING,
					String.format("Repository type '%s' does not support index generation.", provider.getName()));
				continue;
			}
			File indexFile = getIndexFile(provider);
			try {
				generateIndex(indexFile, provider);
			} catch (Exception e) {
				logService.log(LogService.LOG_ERROR, String.format(
					"Unable to regenerate index file '%s' for repository type %s", indexFile, provider.getName()), e);
			}
		}
	}

	private final static Pattern INCREMENT_P = Pattern.compile("increment\\s*=\\s*\"(\\d+)\"");

	private synchronized void generateIndex(File indexFile, IRepositoryContentProvider provider) throws Exception {
		if (indexFile.exists() && !indexFile.isFile())
			throw new IllegalArgumentException(String.format(
				"Cannot create file: '%s' already exists but is not a plain file.", indexFile.getAbsoluteFile()));

		Set<File> allFiles = new HashSet<>();
		gatherFiles(allFiles);

		IO.mkdirs(storageDir);
		File shaFile = new File(indexFile.getPath() + REPO_INDEX_SHA_EXTENSION);
		IO.delete(shaFile);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			URI rootUri = storageDir.getCanonicalFile()
				.toURI();
			provider.generateIndex(allFiles, out, this.getName(), rootUri, pretty, registry, logService);

			byte[] data = out.toByteArray();
			if (pretty && indexFile.isFile()) {
				String newer = new String(data, StandardCharsets.UTF_8);
				String older = new String(IO.read(indexFile), StandardCharsets.UTF_8);

				if (Strings.compareExcept(older, newer, INCREMENT_P)) {
					logService.log(LogService.LOG_INFO,
						getName() + " not saving because files are identical except increment");
					return;
				}
			}
			IO.delete(indexFile);
			IO.write(data, indexFile);
			MessageDigest md = MessageDigest.getInstance(SHA256.ALGORITHM);
			md.update(data);
			IO.store(Hex.toHexString(md.digest())
				.toLowerCase(), shaFile);
		}
	}

	@SuppressWarnings("deprecation")
	private void gatherFiles(Set<File> allFiles) throws Exception {
		if (!storageDir.isDirectory())
			return;

		LinkedList<File> files = new LinkedList<>();
		String[] onlydirsFiles = null;
		if (onlydirs != null) {
			String[] onlydirs2 = onlydirs.split(",");
			onlydirsFiles = new String[onlydirs2.length];
			for (int i = 0; i < onlydirs2.length; i++) {
				onlydirsFiles[i] = new File(storageDir.getAbsolutePath(), onlydirs2[i]).getAbsolutePath();
			}
		}

		listRecurse(REPO_FILE, onlydirsFiles, storageDir, storageDir, files);

		allFiles.addAll(files);
	}

	private void listRecurse(final Pattern pattern, final String[] onlydirsFiles, File root, File dir,
		LinkedList<File> files) {
		final LinkedList<File> dirs = new LinkedList<>();
		File[] moreFiles = dir.listFiles((FileFilter) f -> {
			if (f.isDirectory()) {
				boolean addit = true;
				if (onlydirsFiles != null) {
					String fabs = f.getAbsolutePath();
					addit = false;
					for (String dirtest : onlydirsFiles) {
						if (dirtest.startsWith(fabs) || fabs.startsWith(dirtest)) {
							addit = true;
							break;
						}
					}
				}
				if (addit) {
					dirs.add(f);
				}
			} else if (f.isFile()) {
				Matcher matcher = pattern.matcher(f.getName());
				return matcher.matches();
			}
			return false;
		});
		// Add the files that we found.
		files.addAll(Arrays.asList(moreFiles));

		// keep recursing
		for (File d : dirs) {
			listRecurse(pattern, onlydirsFiles, root, d, files);
		}
	}

	@Override
	public boolean canWrite() {
		return !readOnly;
	}

	private synchronized void finishPut() throws Exception {
		reset();
		regenerateAllIndexes();

		List<URI> clone = new ArrayList<>(newFilesInCoordination);
		synchronized (newFilesInCoordination) {
			newFilesInCoordination.clear();
		}
		for (URI entry : clone) {
			File file = new File(entry);
			fireBundleAdded(file);
		}
	}

	@Override
	public synchronized void ended(Coordination coordination) throws Exception {
		finishPut();
	}

	@Override
	public void failed(Coordination coordination) throws Exception {
		ArrayList<URI> clone;
		synchronized (newFilesInCoordination) {
			clone = new ArrayList<>(newFilesInCoordination);
			newFilesInCoordination.clear();
		}
		for (URI entry : clone) {
			try {
				IO.deleteWithException(new File(entry));
			} catch (Exception e) {
				reporter.warning("Failed to remove repository entry %s on coordination rollback: %s", entry, e);
			}
		}
	}

	protected File putArtifact(File tmpFile) throws Exception {
		assert (tmpFile != null);
		assert (tmpFile.isFile());

		init();

		try (Jar jar = new Jar(tmpFile)) {
			String bsn = jar.getBsn();
			if (bsn == null || !Verifier.isBsn(bsn))
				throw new IllegalArgumentException("Jar does not have a symbolic name");

			File dir = new File(storageDir, bsn);
			if (dir.exists() && !dir.isDirectory())
				throw new IllegalArgumentException(
					"Path already exists but is not a directory: " + dir.getAbsolutePath());
			IO.mkdirs(dir);

			String versionString = jar.getVersion();
			if (versionString == null)
				versionString = "0";
			else if (!Verifier.isVersion(versionString))
				throw new IllegalArgumentException("Invalid version " + versionString + " in file " + tmpFile);

			Version version = Version.parseVersion(versionString);
			String fName = bsn + "-" + version.getWithoutQualifier() + ".jar";
			File file = new File(dir, fName);

			// check overwrite policy
			if (!overwrite && file.exists())
				return null;

			// An open jar on file will fail rename on windows
			jar.close();

			IO.rename(tmpFile, file);

			synchronized (newFilesInCoordination) {
				newFilesInCoordination.add(file.toURI());
			}

			Coordinator coordinator = (registry != null) ? registry.getPlugin(Coordinator.class) : null;
			if (!(coordinator != null && coordinator.addParticipant(this))) {
				finishPut();
			}
			return file;
		}
	}

	/* NOTE: this is a straight copy of FileRepo.put */
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

		/* the root directory of the repository has to be a directory */
		if (!storageDir.isDirectory()) {
			throw new IOException("Repository directory " + storageDir + " is not a directory");
		}

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
			tmpFile = IO.createTempFile(storageDir, "put", ".bnd");
			IO.copy(dis, tmpFile);

			/* beforeGet the digest if available */
			byte[] disDigest = dis.getMessageDigest()
				.digest();

			if (options.digest != null && !Arrays.equals(options.digest, disDigest))
				throw new IOException("Retrieved artifact digest doesn't match specified digest");

			/* put the artifact into the repository (from the temporary file) */
			File file = putArtifact(tmpFile);

			PutResult result = new PutResult();
			if (file != null) {
				result.digest = disDigest;
				result.artifact = file.toURI();
			}

			return result;
		} finally {
			if (tmpFile != null && tmpFile.exists()) {
				IO.delete(tmpFile);
			}
		}
	}

	@Override
	public boolean refresh() {
		reset();
		regenerateAllIndexes();
		return true;
	}

	@Override
	public synchronized File getRoot() {
		return storageDir;
	}

	protected void fireBundleAdded(File file) throws Exception {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		if (listeners.isEmpty())
			return;
		try (Jar jar = new Jar(file)) {
			for (RepositoryListenerPlugin listener : listeners) {
				try {
					listener.bundleAdded(this, jar, file);
				} catch (Exception e) {
					if (reporter != null)
						reporter.warning("Repository listener threw an unexpected exception: %s", e);
				}
			}
		}
	}

	@Override
	public synchronized String getLocation() {
		StringBuilder builder = new StringBuilder();
		builder.append(storageDir.getAbsolutePath());

		String otherPaths = (locations == null) ? EMPTY_LOCATION : locations.toString();
		if (otherPaths != null && otherPaths.length() > 0)
			builder.append(", ")
				.append(otherPaths);

		return builder.toString();
	}

	public void setLocations(String locations) throws MalformedURLException, URISyntaxException {
		parseLocations(locations); // for verification right syntax
		this.locations = locations;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = new HashMap<>();
		map.put("Refresh", () -> regenerateAllIndexes());
		if (target.length == 3) {
			String bsn = (String) target[1];
			String version = (String) target[2];

			@SuppressWarnings("deprecation")
			aQute.bnd.filerepo.FileRepo storageRepo = new aQute.bnd.filerepo.FileRepo(storageDir);
			@SuppressWarnings("deprecation")
			final File f = storageRepo.get(bsn, new VersionRange(version, version), 0);
			if (f != null) {
				map.put("Delete", new Runnable() {

					@Override
					public void run() {
						deleteEntry(f);
						regenerateAllIndexes();
					}

					private void deleteEntry(final File f) {
						File parent = f.getParentFile();
						IO.delete(f);
						File[] listFiles = parent.listFiles();
						if (listFiles.length == 1 && listFiles[0].getName()
							.endsWith("-latest.jar"))
							IO.delete(listFiles[0]);

						listFiles = parent.listFiles();
						if (listFiles.length == 0)
							IO.delete(parent);
					}

				});
			}

		}
		return map;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (target == null || target.length == 0)
			return "LocalIndexedRepo @ " + getLocation();

		if (target.length == 2) {
			ResourceHandle h = getHandle(target);
			if (h == null) {
				regenerateAllIndexes();
				refresh();
				return null;
			}
			if (h.getLocation() == Location.remote) {
				return h.getName() + " (remote, not yet cached)";
			}

			return h.request()
				.getAbsolutePath() + "\n"
				+ SHA1.digest(h.request())
					.asHex()
				+ "\n" + h.getLocation();
		}
		return null;
	}

	private ResourceHandle getHandle(Object... target) throws Exception {
		String bsn = (String) target[0];
		Version v = (Version) target[1];
		VersionRange r = new VersionRange("[" + v.getWithoutQualifier() + "," + v.getWithoutQualifier() + "]");
		ResourceHandle[] handles = getHandles(bsn, r.toString());
		if (handles == null || handles.length == 0) {
			return null;
		}
		ResourceHandle h = handles[0];
		return h;
	}

	@Override
	public String title(Object... target) throws Exception {
		if (target == null)
			return null;

		if (target.length == 2) {
			ResourceHandle handle = getHandle(target);
			if (handle != null) {
				String where = "";
				switch (handle.getLocation()) {
					case local :
						where = "";
						break;

					case remote :
						where = UPWARDS_ARROW;
						break;

					case remote_cached :
						where = DOWNWARDS_ARROW;
						break;
					default :
						where = "?";
						break;

				}
				return target[1] + " " + where;
			}
		}

		return null;
	}

	public void close() {}

}
