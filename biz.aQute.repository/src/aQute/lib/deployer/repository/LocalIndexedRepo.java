package aQute.lib.deployer.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
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
	
	public static final String PROP_LOCAL_DIR = "local";
	public static final String PROP_READONLY = "readonly";
	public static final String PROP_PRETTY = "pretty";
	
	private static final VersionRange RANGE_ANY = new VersionRange(Version.LOWEST.toString());

	private FileRepo storageRepo;
	private boolean readOnly;
	private boolean pretty = false;
	private File storageDir;

	// @GuardedBy("newFilesInCoordination")
	private final List<Pair<Jar, File>> newFilesInCoordination = new LinkedList<Pair<Jar,File>>();

	@Override
	public void setProperties(Map<String, String> map) {
		super.setProperties(map);
		
		// Load essential properties
		String localDirPath = map.get(PROP_LOCAL_DIR);
		if (localDirPath == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on %s plugin.", PROP_LOCAL_DIR, getClass().getName()));
		storageDir = new File(localDirPath);
		if (storageDir.exists() && !storageDir.isDirectory())
			throw new IllegalArgumentException(String.format("Local path '%s' exists and is not a directory.", localDirPath));
		readOnly = "true".equalsIgnoreCase(map.get(PROP_READONLY));
		pretty = "true".equalsIgnoreCase(map.get(PROP_PRETTY));
		
		// Configure the storage repository
		storageRepo = new FileRepo(storageDir);
		
		// Set the local index and cache directory locations
		cacheDir = new File(storageDir, ".obrcache");
		if (cacheDir.exists() && !cacheDir.isDirectory())
			throw new IllegalArgumentException(String.format("Cannot create repository cache: '%s' already exists but is not directory.", cacheDir.getAbsolutePath()));
	}
	
	@Override
	protected synchronized List<URL> loadIndexes() throws Exception {
		Collection<URL> remotes = super.loadIndexes();
		List<URL> indexes = new ArrayList<URL>(remotes.size() + contentProviders.size());
		
		for (IRepositoryContentProvider contentProvider : contentProviders) {
			File indexFile = getIndexFile(contentProvider);
			try {
				if (indexFile.exists()) {
					indexes.add(indexFile.toURI().toURL());
				} else {
					if (contentProvider.supportsGeneration()) {
						generateIndex(indexFile, contentProvider);
						indexes.add(indexFile.toURI().toURL());
					}
				}
			} catch (Exception e) {
				logService.log(LogService.LOG_ERROR, String.format("Unable to load/generate index file '%s' for repository type %s", contentProvider.getName()), e);
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
		for (IRepositoryContentProvider provider : contentProviders) {
			if (!provider.supportsGeneration())
				continue;
			File indexFile = getIndexFile(provider);
			try {
				generateIndex(indexFile, provider);
			} catch (Exception e) {
				logService.log(LogService.LOG_ERROR, String.format("Unable to regenerate index file '%s' for repository type %s", indexFile, provider.getName()), e);
			}
		}
	}
	
	private synchronized void generateIndex(File indexFile, IRepositoryContentProvider provider) throws Exception {
		if (indexFile.exists() && !indexFile.isFile())
			throw new IllegalArgumentException(String.format("Cannot create file: '%s' already exists but is not a plain file.", indexFile.getAbsoluteFile()));

		Set<File> allFiles = new HashSet<File>();
		gatherFiles(allFiles);
		
		FileOutputStream out = null;
		try {
			storageDir.mkdirs();
			out = new FileOutputStream(indexFile);
			
			String rootUrl = storageDir.getCanonicalFile().toURI().toURL().toString();
			provider.generateIndex(allFiles, out, this.getName(), rootUrl, pretty, registry, logService);
		} finally {
			IO.close(out);
		}
	}

	private void gatherFiles(Set<File> allFiles) throws Exception {
		List<String> bsns = storageRepo.list(null);
		if (bsns != null) for (String bsn : bsns) {
			File[] files = storageRepo.get(bsn, RANGE_ANY);
			if (files != null) for (File file : files) {
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
		String fileName = String.format("%s-%d.%d.%d.jar", bsn, version.getMajor(), version.getMinor(), version.getMicro());
		File file = new File(dir, fileName);
		jar.write(file);
		
		synchronized (newFilesInCoordination) {
			newFilesInCoordination.add(new Pair<Jar, File>(jar, file));
		}
		return file;
	}
	
	private synchronized void finishPut() throws Exception {
		reset();
		regenerateAllIndexes();
		
		List<Pair<Jar,File>> clone = new ArrayList<Pair<Jar, File>>(newFilesInCoordination);
		synchronized (newFilesInCoordination) {
			newFilesInCoordination.clear();
		}
		for (Pair<Jar, File> entry : clone) {
			fireBundleAdded(entry.a, entry.b);
		}
	}
	
	public synchronized void ended(Coordination coordination) throws Exception {
		finishPut();
	}
	
	public void failed(Coordination coordination) throws Exception {
		ArrayList<Pair<Jar,File>> clone;
		synchronized (newFilesInCoordination) {
			clone = new ArrayList<Pair<Jar, File>>(newFilesInCoordination);
			newFilesInCoordination.clear();
		}
		for (Pair<Jar, File> entry : clone) {
			try {
				entry.b.delete();
			} catch (Exception e) {
				reporter.warning("Failed to remove repository entry %s on coordination rollback: %s", entry.b, e);
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
			} catch (Exception e) {
				if (reporter != null)
					reporter.warning("Repository listener threw an unexpected exception: %s", e);
			}
		}
	}
}
