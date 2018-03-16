package aQute.bnd.repository.maven.provider;

import static aQute.bnd.osgi.repository.ResourcesRepository.toResourcesRepository;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.repository.Phase;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.Program;
import aQute.service.reporter.Reporter;

/**
 * Maintains an index of descriptors of archives. These descriptors are stored
 * in the maven repository
 */
class IndexFile {

	private static final JSONCodec CODEC = new JSONCodec();

	public class BundleDescriptor extends ResourceDescriptor {
		public long		lastModified;
		public Archive	archive;
		public boolean	merged;
		String			error;
		Promise<File>	promise;
		Resource		resource;

		public synchronized Resource getResource() {
			if (resource == null) {

				try {
					File f = promise.getValue();
					ResourceBuilder rb = new ResourceBuilder();
					rb.addFile(f, f.toURI());
					resource = rb.build();
				} catch (Exception e) {
					reporter.exception(e, "Failed to get file for %s", archive);
					resource = ResourceUtils.DUMMY_RESOURCE;
				}
			}
			return resource;
		}
	}

	private final ConcurrentMap<Archive, Promise<File>>	promises	= new ConcurrentHashMap<>();
	final ConcurrentMap<Archive, BundleDescriptor>		descriptors	= new ConcurrentHashMap<>();
	final File											indexFile;
	final File											cacheDir;
	private final IMavenRepo							repo;
	private final Reporter								reporter;
	private long										lastModified;
	private AtomicBoolean								refresh		= new AtomicBoolean();
	private final ReadWriteLock							lock		= new ReentrantReadWriteLock();
	private final PromiseFactory						promiseFactory;

	IndexFile(Reporter reporter, File file, IMavenRepo repo, PromiseFactory promiseFactory) throws Exception {
		this.reporter = reporter;
		this.indexFile = file;
		this.repo = repo;
		this.cacheDir = new File(indexFile.getParentFile(), indexFile.getName() + ".info");
		this.promiseFactory = promiseFactory;
	}

	void open() throws Exception {
		loadIndexFile();
		sync();
	}

	private void sync() throws Exception {
		List<Promise<File>> sync = new ArrayList<>(promises.size());
		for (Iterator<Entry<Archive, Promise<File>>> i = promises.entrySet()
			.iterator(); i.hasNext();) {
			Entry<Archive, Promise<File>> entry = i.next();
			i.remove();
			sync.add(entry.getValue()
				.onFailure(failure -> reporter.exception(failure, "Failed to sync %s", entry.getKey())));
		}
		promiseFactory.all(sync)
			.getFailure(); // block until all promises resolved
	}

	BundleDescriptor add(Archive archive) throws Exception {
		BundleDescriptor old = descriptors.putIfAbsent(archive, createInitialDescriptor(archive));
		BundleDescriptor descriptor = descriptors.get(archive);
		updateDescriptor(descriptor, repo.get(archive)
			.getValue());
		if (old == null || !Arrays.equals(descriptor.id, old.id)) {
			saveIndexFile();
			return descriptor;
		}
		return null;
	}

	BundleDescriptor remove(Archive archive) throws Exception {
		BundleDescriptor descriptor = descriptors.remove(archive);
		if (descriptor != null) {
			saveIndexFile();
		}
		return descriptor;
	}

	public void remove(String bsn) throws Exception {
		descriptors.values()
			.removeIf(bundleDescriptor -> isBsn(bsn, bundleDescriptor));
		saveIndexFile();
	}

	Collection<String> list() {
		return descriptors.values()
			.stream()
			.map(descriptor -> descriptor.bsn)
			.collect(toSet());
	}

	Collection<Version> list(String bsn) {
		return descriptors.values()
			.stream()
			.filter(descriptor -> isBsn(bsn, descriptor))
			.map(descriptor -> descriptor.version)
			.collect(toSet());
	}

	boolean isBsn(String bsn, BundleDescriptor descriptor) {
		return bsn.equals(descriptor.bsn) || bsn.equals(descriptor.archive.getWithoutVersion());
	}

	public synchronized BundleDescriptor getDescriptor(String bsn, Version version) throws Exception {
		sync();
		return descriptors.values()
			.stream()
			.filter(descriptor -> isBsn(bsn, descriptor) && version.equals(descriptor.version))
			.findFirst()
			.orElse(null);
	}

	int getErrors(String name) {
		return (int) descriptors.values()
			.stream()
			.filter(descriptor -> name == null || name.equals(descriptor.bsn))
			.filter(descriptor -> descriptor.error != null)
			.count();
	}

	Set<Program> getProgramsForBsn(String name) {
		return descriptors.values()
			.stream()
			.filter(descriptor -> name == null || name.equals(descriptor.bsn))
			.map(descriptor -> descriptor.archive.revision.program)
			.collect(toSet());
	}

	long last = 0L;

	boolean refresh() throws Exception {

		refresh.getAndSet(false);

		if (indexFile.lastModified() != lastModified && last + 10000 < System.currentTimeMillis()) {
			loadIndexFile();
			last = System.currentTimeMillis();
			return true;
		}
		return descriptors.values()
			.stream()
			.filter(descriptor -> {
				try {
					if ((descriptor.promise != null) && descriptor.promise.isDone()
						&& (descriptor.promise.getFailure() == null)) {
						File f = descriptor.promise.getValue();
						if ((f != null) && f.isFile() && (f.lastModified() != descriptor.lastModified)) {
							updateDescriptor(descriptor, f);
							return true;
						}
					}
				} catch (InvocationTargetException | InterruptedException e) {
					throw Exceptions.duck(e);
				}
				return false;
			})
			.count() > 0L;
	}

	private void loadIndexFile() throws Exception {
		lastModified = indexFile.lastModified();
		Set<Archive> toBeDeleted = new HashSet<>(descriptors.keySet());
		if (indexFile.isFile()) {
			lock.readLock()
				.lock();
			try (BufferedReader rdr = IO.reader(indexFile)) {
				String line;
				while ((line = rdr.readLine()) != null) {
					line = Strings.trim(line);
					if (line.startsWith("#") || line.isEmpty())
						continue;

					Archive a = Archive.valueOf(line);
					if (a == null) {
						reporter.error("MavenBndRepository: invalid entry %s in file %s", line, indexFile);
					} else {
						toBeDeleted.remove(a);
						loadDescriptorAsync(a);
					}
				}
			} finally {
				lock.readLock()
					.unlock();
			}

			this.descriptors.keySet()
				.removeAll(toBeDeleted);
			this.promises.keySet()
				.removeAll(toBeDeleted);
		}
	}

	void updateDescriptor(final Archive archive, File file) {
		BundleDescriptor descriptor = descriptors.get(archive);
		updateDescriptor(descriptor, file);
	}

	void updateDescriptor(BundleDescriptor descriptor, File file) {
		try {
			if (file == null || !file.isFile()) {
				File descriptorFile = getDescriptorFile(descriptor.archive);
				IO.delete(descriptorFile);
				reporter.error("Could not find file %s", descriptor.archive);
				descriptor.error = "File not found";
			} else {
				if (descriptor.lastModified != file.lastModified()) {

					Domain m = Domain.domain(file);
					if (m == null)
						m = Domain.domain(Collections.emptyMap());

					Entry<String, Attrs> bsn = m.getBundleSymbolicName();
					descriptor.bsn = bsn != null ? bsn.getKey() : null;
					descriptor.version = m.getBundleVersion() == null ? null
						: Version.parseVersion(m.getBundleVersion());

					if (descriptor.bsn == null) {
						descriptor.bsn = descriptor.archive.getWithoutVersion();
						descriptor.version = descriptor.archive.revision.version.getOSGiVersion();
					} else if (descriptor.version == null)
						descriptor.version = Version.LOWEST;
					descriptor.description = m.getBundleDescription();
					descriptor.id = SHA1.digest(file)
						.digest();
					descriptor.included = false;
					descriptor.lastModified = file.lastModified();
					descriptor.sha256 = SHA256.digest(file)
						.digest();
					saveDescriptor(descriptor);
					refresh.set(true);
				}
				if (descriptor.promise == null && file != null)
					descriptor.promise = promiseFactory.resolved(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
			descriptor.error = e.toString();
			refresh.set(true);

		}
	}

	private void saveDescriptor(BundleDescriptor descriptor) throws IOException, Exception {
		File df = getDescriptorFile(descriptor.archive);
		IO.mkdirs(df.getParentFile());
		CODEC.enc()
			.to(df)
			.put(descriptor);
	}

	private void loadDescriptorAsync(final Archive archive) throws Exception {
		if (updateLocal(archive))
			return;

		updateAsync(archive);
	}

	private boolean updateLocal(final Archive archive) {
		File archiveFile = repo.toLocalFile(archive);
		if (archiveFile.isFile()) {
			File descriptorFile = getDescriptorFile(archive);
			if (descriptorFile.isFile()) {
				try {
					BundleDescriptor descriptor = CODEC.dec()
						.from(descriptorFile)
						.get(BundleDescriptor.class);
					descriptor.promise = promiseFactory.resolved(archiveFile);
					descriptor.resource = null;
					descriptors.put(archive, descriptor);
					return true;
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return false;
	}

	Promise<File> updateAsync(Archive archive) throws Exception {
		Promise<File> promise = repo.get(archive);
		return updateAsync(archive, promise);
	}

	private Promise<File> updateAsync(Archive archive, Promise<File> promise) throws Exception {
		BundleDescriptor descriptor = createInitialDescriptor(archive);
		promise = updateAsync(descriptor, promise);
		descriptors.put(archive, descriptor);
		return promise;
	}

	Promise<File> updateAsync(final BundleDescriptor descriptor, Promise<File> promise) throws Exception {
		descriptor.promise = promise.thenAccept(file -> updateDescriptor(descriptor, file));
		descriptor.resource = null;
		promises.put(descriptor.archive, descriptor.promise);
		return descriptor.promise;
	}

	File getDescriptorFile(Archive archive) {
		File dir = new File(repo.toLocalFile(archive)
			.getParentFile(), ".bnd");
		return new File(dir, archive.getName());
	}

	private BundleDescriptor createInitialDescriptor(Archive archive) throws Exception {
		BundleDescriptor descriptor = new BundleDescriptor();
		descriptor.archive = archive;
		descriptor.phase = archive.isSnapshot() ? Phase.STAGING : Phase.MASTER;
		descriptor.url = repo.toRemoteURI(archive);
		descriptor.bsn = archive.getWithoutVersion();
		descriptor.version = archive.revision.version.getOSGiVersion();
		return descriptor;
	}

	private void saveIndexFile() throws Exception {
		lock.writeLock()
			.lock();
		try {
			File tmp = File.createTempFile("index", null);
			try (PrintWriter pw = new PrintWriter(tmp)) {
				descriptors.keySet()
					.stream()
					.sorted()
					.forEachOrdered(archive -> pw.println(archive));
			}
			Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			lock.writeLock()
				.unlock();
		}

		lastModified = indexFile.lastModified();
	}

	public void save() throws Exception {
		saveIndexFile();
	}

	public Collection<Archive> getArchives() {
		return descriptors.keySet();
	}

	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return descriptors.values()
			.stream()
			.map(BundleDescriptor::getResource)
			.collect(toResourcesRepository())
			.findProviders(requirements);
	}
}
