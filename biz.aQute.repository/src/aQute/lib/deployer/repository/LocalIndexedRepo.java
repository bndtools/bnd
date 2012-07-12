package aQute.lib.deployer.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.log.LogService;

import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.libg.filerepo.FileRepo;
import aQute.libg.tuple.Pair;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class LocalIndexedRepo extends FixedIndexedRepo implements Refreshable, Participant {

	private static final String	CACHE_PATH	= ".cache";
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
	private final List<Pair<Jar,File>>	newFilesInCoordination	= new LinkedList<Pair<Jar,File>>();

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(map);

		// Load essential properties
		String localDirPath = map.get(PROP_LOCAL_DIR);
		if (localDirPath == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on %s plugin.", PROP_LOCAL_DIR, getClass().getName()));
		
		storageDir = new File(localDirPath);
		if (storageDir.exists() && !storageDir.isDirectory())
			throw new IllegalArgumentException(String.format("Local path '%s' exists and is not a directory.", localDirPath));
		
		readOnly = Boolean.parseBoolean(map.get(PROP_READONLY));
		pretty = Boolean.parseBoolean(map.get(PROP_PRETTY));
		overwrite = map.get(PROP_OVERWRITE) == null ? true : Boolean.parseBoolean(map.get(PROP_OVERWRITE));

		// Configure the storage repository
		storageRepo = new FileRepo(storageDir);

		// Set the local index and cache directory locations
		cacheDir = new File(storageDir, CACHE_PATH);
		if (cacheDir.exists() && !cacheDir.isDirectory())
			throw new IllegalArgumentException(String.format("Cannot create repository cache: '%s' already exists but is not directory.", cacheDir.getAbsolutePath()));
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
		try {
			storageDir.mkdirs();
			out = new FileOutputStream(indexFile);

			URI rootUri = storageDir.getCanonicalFile().toURI();
			provider.generateIndex(allFiles, out, this.getName(), rootUri, pretty, registry, logService);
		}
		finally {
			IO.close(out);
		}
	}

	private void gatherFiles(Set<File> allFiles) throws Exception {
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

	private File beginPut(Jar jar) throws Exception {
		String bsn = jar.getBsn();
		if (bsn == null)
			throw new IllegalArgumentException("Jar does not have a Bundle-SymbolicName manifest header");

		File dir = new File(storageDir, bsn);
		if (dir.exists() && !dir.isDirectory())
			throw new IllegalArgumentException("Path already exists but is not a directory: " + dir.getAbsolutePath());
		dir.mkdirs();

		Version version = Version.parseVersion(jar.getVersion());
		String fileName = String.format("%s-%d.%d.%d.jar", bsn, version.getMajor(), version.getMinor(),
				version.getMicro());
		File file = new File(dir, fileName);
		
		// check overwrite policy
		if (!overwrite && file.exists()) return file;
		
		jar.write(file);

		synchronized (newFilesInCoordination) {
			newFilesInCoordination.add(new Pair<Jar,File>(jar, file));
		}
		return file;
	}

	private synchronized void finishPut() throws Exception {
		reset();
		regenerateAllIndexes();

		List<Pair<Jar,File>> clone = new ArrayList<Pair<Jar,File>>(newFilesInCoordination);
		synchronized (newFilesInCoordination) {
			newFilesInCoordination.clear();
		}
		for (Pair<Jar,File> entry : clone) {
			fireBundleAdded(entry.getFirst(), entry.getSecond());
		}
	}

	public synchronized void ended(Coordination coordination) throws Exception {
		finishPut();
	}

	public void failed(Coordination coordination) throws Exception {
		ArrayList<Pair<Jar,File>> clone;
		synchronized (newFilesInCoordination) {
			clone = new ArrayList<Pair<Jar,File>>(newFilesInCoordination);
			newFilesInCoordination.clear();
		}
		for (Pair<Jar,File> entry : clone) {
			try {
				entry.getSecond().delete();
			}
			catch (Exception e) {
				reporter.warning("Failed to remove repository entry %s on coordination rollback: %s", entry.getSecond(), e);
			}
		}
	}

	@Override
	public synchronized File put(Jar jar) throws Exception {
		init();

		Coordinator coordinator = (registry != null) ? registry.getPlugin(Coordinator.class) : null;
		File newFile;
		if (coordinator != null && coordinator.addParticipant(this)) {
			newFile = beginPut(jar);
		} else {
			newFile = beginPut(jar);
			finishPut();
		}
		return newFile;
	}

	public boolean refresh() {
		reset();
		return true;
	}

	public File getRoot() {
		return storageDir;
	}

	protected void fireBundleAdded(Jar jar, File file) {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		for (RepositoryListenerPlugin listener : listeners) {
			try {
				listener.bundleAdded(this, jar, file);
			}
			catch (Exception e) {
				if (reporter != null)
					reporter.warning("Repository listener threw an unexpected exception: %s", e);
			}
		}
	}

	@Override
	public String getLocation() {
		StringBuilder builder = new StringBuilder();
		builder.append(storageDir.getAbsolutePath());

		String otherPaths = super.getLocation();
		if (otherPaths != null && otherPaths.length() > 0)
			builder.append(", ").append(otherPaths);

		return builder.toString();
	}

}
