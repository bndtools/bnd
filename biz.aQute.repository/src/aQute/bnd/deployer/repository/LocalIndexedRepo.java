package aQute.bnd.deployer.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import aQute.bnd.filerepo.FileRepo;
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
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;

public class LocalIndexedRepo extends FixedIndexedRepo implements Refreshable, Participant, Actionable {
	private final String		UPWARDS_ARROW				= " \u2191";
	private final String		DOWNWARDS_ARROW				= " \u2193";
	private static final String	CACHE_PATH					= ".cache";
	public static final String	PROP_LOCAL_DIR				= "local";
	public static final String	PROP_READONLY				= "readonly";
	public static final String	PROP_PRETTY					= "pretty";
	public static final String	PROP_OVERWRITE				= "overwrite";
	public static final String	PROP_ONLYDIRS				= "onlydirs";
	public static final String	PROP_FILE_INCLUDE_POLICY	= "file.include.policy";

	static final String	VERSIONED_BUNDLE_PATTERN	= "([-a-zA-z0-9_\\.]+)-([0-9\\.]+)(-[-a-zA-z0-9_]+)?\\.(jar|lib)";
	static final String	ALL_JARS_PATTERN			= ".*jar$";
	static final String	ALL_JARS_AND_LIBS_PATTERN	= ".*(jar|lib)$";

	public enum FileIncludePolicy {
		VERSIONED_BUNDLE(VERSIONED_BUNDLE_PATTERN), ALL_JARS(ALL_JARS_PATTERN), ALL_JARS_AND_LIBS(
				ALL_JARS_AND_LIBS_PATTERN);

		private Pattern pattern;

		FileIncludePolicy(String p_regexp) {
			this.pattern = Pattern.compile(p_regexp);
		}
	}

	@SuppressWarnings("deprecation")
	private boolean	readOnly;
	private boolean	pretty		= false;
	private boolean	overwrite	= true;
	private File	storageDir;
	private String	onlydirs	= null;
	private Pattern	includePolicy;

	// @GuardedBy("newFilesInCoordination")
	private final List<URI> newFilesInCoordination = new LinkedList<URI>();

	@SuppressWarnings("deprecation")
	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(map);

		// Load essential properties
		String localDirPath = map.get(PROP_LOCAL_DIR);
		if (localDirPath == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on %s plugin.",
					PROP_LOCAL_DIR, getClass().getName()));

		storageDir = new File(localDirPath);
		if (storageDir.exists() && !storageDir.isDirectory())
			throw new IllegalArgumentException(String.format("Local path '%s' exists and is not a directory.",
					localDirPath));

		readOnly = Boolean.parseBoolean(map.get(PROP_READONLY));
		pretty = Boolean.parseBoolean(map.get(PROP_PRETTY));
		overwrite = map.get(PROP_OVERWRITE) == null ? true : Boolean.parseBoolean(map.get(PROP_OVERWRITE));
		onlydirs = map.get(PROP_ONLYDIRS);

		// Set the local index and cache directory locations
		cacheDir = new File(storageDir, CACHE_PATH);
		if (cacheDir.exists() && !cacheDir.isDirectory())
			throw new IllegalArgumentException(String.format(
					"Cannot create repository cache: '%s' already exists but is not directory.",
					cacheDir.getAbsolutePath()));

		setIncludePolicy(map);
	}

	private void setIncludePolicy(Map<String,String> props) {
		String policy = props.get(PROP_FILE_INCLUDE_POLICY);
		if (policy != null && !policy.isEmpty()) {
			try {
				FileIncludePolicy spec = FileIncludePolicy.valueOf(policy);
				includePolicy = spec.pattern;
			}
			// this means the policy was not recognized, so try to interpret it
			// as a regex
			catch (IllegalArgumentException e) {
				includePolicy = Pattern.compile(policy);
			}
			// use the default
		} else {
			includePolicy = FileIncludePolicy.VERSIONED_BUNDLE.pattern;
		}
	}

	Pattern includePolicy() {
		return this.includePolicy;
	}

	@Override
	protected synchronized List<URI> loadIndexes() throws Exception {
		Collection<URI> remotes = super.loadIndexes();
		List<URI> indexes = new ArrayList<URI>(remotes.size() + generatingProviders.size());

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
			}
			catch (Exception e) {
				logService.log(LogService.LOG_ERROR,
						String.format("Unable to load/generate index file '%s' for repository type %s", indexFile,
								contentProvider.getName()),
						e);
			}
		}

		indexes.addAll(remotes);
		return indexes;
	}

	/**
	 * <<<<<<< HEAD
	 * 
	 * @param contentProvider
	 *            the repository content provider
	 * @return the filename of the index on local storage =======
	 * @param contentProvider
	 *            the repository content provider @return the filename of the
	 *            index on local storage >>>>>>> stash
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
			}
			catch (Exception e) {
				logService.log(LogService.LOG_ERROR,
						String.format("Unable to regenerate index file '%s' for repository type %s", indexFile,
								provider.getName()),
						e);
			}
		}
	}

	private synchronized void generateIndex(File indexFile, IRepositoryContentProvider provider) throws Exception {
		if (indexFile.exists() && !indexFile.isFile())
			throw new IllegalArgumentException(String.format(
					"Cannot create file: '%s' already exists but is not a plain file.", indexFile.getAbsoluteFile()));

		Set<File> allFiles = new HashSet<File>();
		gatherFiles(allFiles);

		FileOutputStream out = null;
		File shaFile = new File(indexFile.getPath() + REPO_INDEX_SHA_EXTENSION);
		try {
			if (!storageDir.exists() && !storageDir.mkdirs()) {
				throw new IOException("Could not create directory " + storageDir);
			}
			out = new FileOutputStream(indexFile);

			URI rootUri = storageDir.getCanonicalFile().toURI();
			provider.generateIndex(allFiles, out, this.getName(), rootUri, pretty, registry, logService);
		}
		finally {
			IO.close(out);
			out = null;
			shaFile.delete();
		}

		MessageDigest md = MessageDigest.getInstance(SHA256.ALGORITHM);
		IO.copy(indexFile, md);

		try {
			out = new FileOutputStream(shaFile);
			out.write(Hex.toHexString(md.digest()).toLowerCase().toString().getBytes());
		}
		finally {
			if (out != null) {
				out.close();
			}
		}
	}

	@SuppressWarnings("deprecation")
	void gatherFiles(Set<File> allFiles) throws Exception {
		if (!storageDir.isDirectory())
			return;

		LinkedList<File> files = new LinkedList<File>();
		String[] onlydirsFiles = null;
		if (onlydirs != null) {
			String[] onlydirs2 = onlydirs.split(",");
			onlydirsFiles = new String[onlydirs2.length];
			for (int i = 0; i < onlydirs2.length; i++) {
				onlydirsFiles[i] = new File(storageDir.getAbsolutePath(), onlydirs2[i]).getAbsolutePath();
			}
		}

		listRecurse(includePolicy, onlydirsFiles, storageDir, storageDir, files);

		allFiles.addAll(files);
	}

	private void listRecurse(final Pattern pattern, final String[] onlydirsFiles, File root, File dir,
			LinkedList<File> files) {
		final LinkedList<File> dirs = new LinkedList<File>();
		File[] moreFiles = dir.listFiles(new FileFilter() {

			public boolean accept(File f) {
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
			}
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

		List<URI> clone = new ArrayList<URI>(newFilesInCoordination);
		synchronized (newFilesInCoordination) {
			newFilesInCoordination.clear();
		}
		for (URI entry : clone) {
			File file = new File(entry);
			fireBundleAdded(file);
		}
	}

	public synchronized void ended(Coordination coordination) throws Exception {
		finishPut();
	}

	public void failed(Coordination coordination) throws Exception {
		ArrayList<URI> clone;
		synchronized (newFilesInCoordination) {
			clone = new ArrayList<URI>(newFilesInCoordination);
			newFilesInCoordination.clear();
		}
		for (URI entry : clone) {
			try {
				new File(entry).delete();
			}
			catch (Exception e) {
				reporter.warning("Failed to remove repository entry %s on coordination rollback: %s", entry, e);
			}
		}
	}

	protected File putArtifact(File tmpFile) throws Exception {
		assert(tmpFile != null);
		assert(tmpFile.isFile());

		init();

		Jar jar = new Jar(tmpFile);
		try {
			String bsn = jar.getBsn();
			if (bsn == null || !Verifier.isBsn(bsn))
				throw new IllegalArgumentException("Jar does not have a symbolic name");

			File dir = new File(storageDir, bsn);
			if (dir.exists() && !dir.isDirectory())
				throw new IllegalArgumentException(
						"Path already exists but is not a directory: " + dir.getAbsolutePath());
			if (!dir.exists() && !dir.mkdirs()) {
				throw new IOException("Could not create directory " + dir);
			}

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
		finally {
			jar.close();
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
			byte[] disDigest = dis.getMessageDigest().digest();

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
		}
		finally {
			if (tmpFile != null && tmpFile.exists()) {
				IO.delete(tmpFile);
			}
		}
	}

	public boolean refresh() {
		reset();
		regenerateAllIndexes();
		return true;
	}

	public synchronized File getRoot() {
		return storageDir;
	}

	protected void fireBundleAdded(File file) {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		Jar jar = null;
		for (RepositoryListenerPlugin listener : listeners) {
			try {
				if (jar == null)
					jar = new Jar(file);
				listener.bundleAdded(this, jar, file);
			}
			catch (Exception e) {
				if (reporter != null)
					reporter.warning("Repository listener threw an unexpected exception: %s", e);
			}
			finally {
				if (jar != null)
					jar.close();
			}
		}
	}

	@Override
	public synchronized String getLocation() {
		StringBuilder builder = new StringBuilder();
		builder.append(storageDir.getAbsolutePath());

		String otherPaths = super.getLocation();
		if (otherPaths != null && otherPaths.length() > 0)
			builder.append(", ").append(otherPaths);

		return builder.toString();
	}

	public Map<String,Runnable> actions(Object... target) throws Exception {
		Map<String,Runnable> map = new HashMap<String,Runnable>();
		map.put("Refresh", new Runnable() {

			public void run() {
				regenerateAllIndexes();
			}

		});
		if (target.length == 3) {
			String bsn = (String) target[1];
			String version = (String) target[2];

			@SuppressWarnings("deprecation")
			FileRepo storageRepo = new FileRepo(storageDir);
			@SuppressWarnings("deprecation")
			final File f = storageRepo.get(bsn, new VersionRange(version, version), 0);
			if (f != null) {
				map.put("Delete", new Runnable() {

					public void run() {
						deleteEntry(f);
						regenerateAllIndexes();
					}

					private void deleteEntry(final File f) {
						File parent = f.getParentFile();
						f.delete();
						File[] listFiles = parent.listFiles();
						if (listFiles.length == 1 && listFiles[0].getName().endsWith("-latest.jar"))
							listFiles[0].delete();

						listFiles = parent.listFiles();
						if (listFiles.length == 0)
							IO.delete(parent);
					}

				});
			}

		}
		return map;
	}

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

			return h.request().getAbsolutePath() + "\n" + SHA1.digest(h.request()).asHex() + "\n" + h.getLocation();
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
}
