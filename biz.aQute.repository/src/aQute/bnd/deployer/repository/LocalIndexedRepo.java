package aQute.bnd.deployer.repository;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.osgi.service.coordinator.*;
import org.osgi.service.log.*;

import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.filerepo.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;

public class LocalIndexedRepo extends FixedIndexedRepo implements Refreshable, Participant {

	private static final String			CACHE_PATH				= ".cache";
	public static final String			PROP_LOCAL_DIR			= "local";
	public static final String			PROP_READONLY			= "readonly";
	public static final String			PROP_PRETTY				= "pretty";
	public static final String			PROP_OVERWRITE			= "overwrite";

	private static final VersionRange	RANGE_ANY				= new VersionRange(Version.LOWEST.toString());

	private FileRepo					storageRepo;
	private boolean						readOnly;
	private boolean						pretty					= false;
	private boolean						overwrite				= true;
	private File						storageDir;

	// @GuardedBy("newFilesInCoordination")
	private final List<URI>				newFilesInCoordination	= new LinkedList<URI>();

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

		// Configure the storage repository
		storageRepo = new FileRepo(storageDir);

		// Set the local index and cache directory locations
		cacheDir = new File(storageDir, CACHE_PATH);
		if (cacheDir.exists() && !cacheDir.isDirectory())
			throw new IllegalArgumentException(String.format(
					"Cannot create repository cache: '%s' already exists but is not directory.",
					cacheDir.getAbsolutePath()));
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
				logService.log(LogService.LOG_ERROR, String.format(
						"Unable to load/generate index file '%s' for repository type %s", indexFile,
						contentProvider.getName()), e);
			}
		}

		indexes.addAll(remotes);
		return indexes;
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

	private synchronized void regenerateAllIndexes() {
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
				logService.log(LogService.LOG_ERROR, String.format(
						"Unable to regenerate index file '%s' for repository type %s", indexFile, provider.getName()),
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

	private void gatherFiles(Set<File> allFiles) throws Exception {
		if (!storageDir.isDirectory())
			return;
		
		List<String> bsns = storageRepo.list(null);
		if (bsns != null)
			for (String bsn : bsns) {
				File[] files = storageRepo.get(bsn, RANGE_ANY);
				if (files != null)
					for (File file : files) {
						allFiles.add(file.getCanonicalFile());
					}
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
		assert (tmpFile != null);
		assert (tmpFile.isFile());

		init();

		Jar jar = new Jar(tmpFile);
		try {
			String bsn = jar.getBsn();
			if (bsn == null || !Verifier.isBsn(bsn))
				throw new IllegalArgumentException("Jar does not have a Bundle-SymbolicName manifest header");

			File dir = new File(storageDir, bsn);
			if (dir.exists() && !dir.isDirectory())
				throw new IllegalArgumentException("Path already exists but is not a directory: "
						+ dir.getAbsolutePath());
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

	@Override
	public void refreshCacheMetadata() {
		regenerateAllIndexes();
	}
	
}
