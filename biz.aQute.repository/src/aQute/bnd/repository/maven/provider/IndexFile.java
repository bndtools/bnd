package aQute.bnd.repository.maven.provider;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.MavenCapability;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.ResourceInfo;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.BundleCap;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.SupplierWithException;
import aQute.lib.io.IO;
import aQute.bnd.memoize.Memoize;
import aQute.lib.strings.Strings;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.Program;
import aQute.service.reporter.Reporter;

/**
 * Represents the index list that contains Maven GAVs. It maps the entries in
 * this file to an internal map and uses a BridgeRepository to do the OSGi and
 * bnd repo stuff.
 * <p>
 * Asynchronous refreshes are handled y replacing a Promise with a
 * BridgeRepository. This is a nice way to block any later comers when they need
 * results without having to wait for the operation to be done.
 */
class IndexFile {
	private final static Logger					logger		= LoggerFactory.getLogger(IndexFile.class);

	final File									indexFile;
	final IMavenRepo							repo;
	private final Processor						domain;
	private final Macro							replacer;

	final Reporter								reporter;
	final PromiseFactory						promiseFactory;
	final Map<Archive, Resource>				archives	= new ConcurrentHashMap<>();
	final Set<String>							multi;
	final String								source;

	private volatile long						lastModified;
	private long								last		= 0L;
	private volatile Memoize<BridgeRepository>	bridge;
	private volatile Promise<Boolean>			updateSerializer;

	private String								status;

	/*
	 * Constructor
	 */
	IndexFile(Processor domain, Reporter reporter, File file, String source, IMavenRepo repo,
		PromiseFactory promiseFactory, Set<String> multi) throws Exception {
		this.source = source;
		this.domain = (domain != null) ? domain : new Processor();
		this.replacer = this.domain.getReplacer();
		this.reporter = reporter;
		this.indexFile = file;
		this.repo = repo;
		this.promiseFactory = promiseFactory;
		this.multi = multi;
		this.updateSerializer = promiseFactory.resolved(Boolean.TRUE);
		this.bridge = Memoize.supplier(BridgeRepository::new);
	}

	/*
	 * Open, will refresh this repository, which will load the file
	 */
	void open() throws Exception {
		serialize(this::load);
	}

	/**
	 * We use chained promises to serialize any updates to the archives map and
	 * the bridge repository.
	 *
	 * @param updater A supplier for the promise that performs the update work.
	 *            We use a supplier so we can defer calling the method until
	 *            inside the flatMap operation.
	 * @return The promise for the current update operation. This can be used to
	 *         sync on the current update or to add additional callback to run
	 *         when the current update is done.
	 */
	private Promise<Boolean> serialize(SupplierWithException<Promise<Boolean>> updater) {
		Deferred<Boolean> latch = promiseFactory.deferred();
		try {
			synchronized (this) {
				return updateSerializer = updateSerializer.flatMap(b -> latch.getPromise())
					.flatMap(b -> updater.get())
					.recover(failed -> {
						Throwable failure = Exceptions.unrollCause(failed.getFailure(),
							InvocationTargetException.class);
						logger.debug("Failure occured updating index {}", getMessage(failure), failed.getFailure());
						return Boolean.FALSE;
					});
			}
		} finally {
			latch.resolve(Boolean.TRUE);
		}
	}

	/*
	 * Persistently add an archive to the index file and refresh. If the archive
	 * is not kept then this is a noop
	 */
	void add(Archive archive) throws Exception {
		if (archives.containsKey(archive))
			return;

		logger.debug("adding {}", archive);
		Set<Archive> toAdd = Collections.singleton(archive);
		Promise<Boolean> serializer = serialize(
			() -> update(toAdd).thenAccept(b -> save(toAdd, Collections.emptySet())));
		sync(serializer);
	}

	/*
	 * Remove an archive from the index file and refresh. If the archive is not
	 * kept then this is a noop
	 */
	void remove(Archive archive) throws Exception {
		Promise<Boolean> serializer = serialize(() -> {
			if (!removeWithDerived(archive)) {
				return promiseFactory.resolved(Boolean.TRUE);
			}
			logger.debug("remove {}", archive);
			Set<Archive> toRemove = Collections.singleton(archive);
			return update(null).thenAccept(b -> save(Collections.emptySet(), toRemove));
		});
		sync(serializer);
	}

	/*
	 * Remove all resources with a given bsn. This function does not recognize
	 * GAV bsns. If the primary BSN has derived resources then they are also
	 * removed.
	 */
	void remove(String bsn) throws Exception {
		Promise<Boolean> serializer = serialize(() -> {
			logger.debug("remove bsn {}", bsn);

			Set<Archive> toRemove = MapStream.of(archives)
				.filterValue(v -> isBsn(bsn, v))
				.keys()
				.collect(toSet());
			boolean changed = false;

			for (Archive archive : toRemove) {
				changed |= removeWithDerived(archive);
			}

			if (!changed) {
				return promiseFactory.resolved(Boolean.TRUE);
			}
			return update(null).thenAccept(b -> save(Collections.emptySet(), toRemove));
		});
		sync(serializer);
	}

	/*
	 * Remove an archive and aby derived archive.
	 */
	private boolean removeWithDerived(Archive archive) {
		Resource remove = archives.remove(archive);
		if (remove != null) {
			archives.keySet()
				.removeIf(target -> isDerived(target, archive));
			return true;
		}
		return false;
	}

	/*
	 * A derived archive has the same revision but different classifier. We
	 * exclude the default resource. I.e. the one with an empty classifier.
	 */
	private boolean isDerived(Archive inMap, Archive parent) {
		boolean match = inMap.revision.equals(parent.revision) && !inMap.classifier.isEmpty();
		return match;
	}

	/*
	 * Load the index file file.
	 */
	private Promise<Boolean> load() throws Exception {
		lastModified = indexFile.lastModified();
		Set<Archive> toBeAdded = new HashSet<>();

		if (indexFile.isFile()) {
			toBeAdded.addAll(read(indexFile));
		}
		if (source != null) {
			toBeAdded.addAll(read(source, false));
		}

		Set<Archive> keySet = archives.keySet();
		Set<Archive> toBeDeleted = new HashSet<>(keySet);
		toBeDeleted.removeAll(toBeAdded);
		toBeAdded.removeAll(keySet);

		keySet.removeAll(toBeDeleted);
		return update(toBeAdded);
	}

	/*
	 * Update the set of archives. This will add the given archives and then
	 * create a new bridge. All newly added archives are scheduled to be
	 * download and after downloaded are parsed. This method should only be
	 * called via the serializer.
	 */
	private Promise<Boolean> update(Set<Archive> toAdd) {
		List<Promise<Map<Archive, Resource>>> promises;
		if (toAdd == null) {
			promises = Collections.emptyList();
		} else {
			promises = toAdd.stream()
				.map(archive -> {
					if (!archive.isSnapshot()) {
						File localFile = repo.toLocalFile(archive);
						if (localFile.isFile() && localFile.length() > 0) {
							return promiseFactory.submit(() -> parseSingleOrMultiFile(archive, localFile))
								.recover(p -> failed(archive, p.getFailure()));
						}
					}
					try {
						return repo.get(archive)
							.map(file -> (file == null) ? failed(archive, "Not found")
								: parseSingleOrMultiFile(archive, file))
							.recover(p -> failed(archive, p.getFailure()));
					} catch (Exception e) {
						return promiseFactory.resolved(failed(archive, e));
					}
				})
				.collect(toList());
		}
		return promiseFactory.all(promises)
			.map(maps -> {
				// update archives with results
				maps.forEach(archives::putAll);
				// snapshot archive resources
				ResourcesRepository resourcesRepository = new ResourcesRepository(archives.values());
				bridge = Memoize.supplier(() -> new BridgeRepository(resourcesRepository));
				return Boolean.TRUE;
			});
	}

	private Map<Archive, Resource> failed(Archive archive, Throwable t) {
		Throwable failure = Exceptions.unrollCause(t, InvocationTargetException.class);
		String message = getMessage(failure);
		logger.debug("Failed to get {}: {}", archive, message, t);
		return failed(archive, message);
	}

	private Map<Archive, Resource> failed(Archive archive, String msg) {
		return Collections.singletonMap(archive, info(archive, msg));
	}

	private String getMessage(Throwable failure) {
		failure = Exceptions.unrollCause(failure, InvocationTargetException.class);
		if (failure instanceof FileNotFoundException) {
			return "Not found";
		}
		return failure.getMessage();
	}

	private Map<Archive, Resource> parseSingleOrMultiFile(Archive archive, File file) {
		try {
			if (isMulti(file.getName())) {
				return parseMulti(archive, file);
			} else {
				return parseSingle(archive, file);
			}
		} catch (Exception e) {
			IO.delete(file);
			return failed(archive, e);
		}
	}

	private boolean isMulti(String name) {
		if (multi.isEmpty())
			return false;

		String[] extension = Strings.extension(name.toLowerCase());
		return extension.length == 2 && multi.contains(extension[1]);
	}

	private Map<Archive, Resource> parseSingle(Archive archive, File single) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		boolean hasIdentity = rb.addFile(single, single.toURI());
		if (!hasIdentity) {
			String name = archive.getWithoutVersion();
			MavenVersion version = archive.revision.version;

			BridgeRepository.addInformationCapability(rb, name, version.getOSGiVersion(), repo.toString(),
				Constants.NOT_A_BUNDLE_S);
		}
		MavenCapability.addMavenCapability(rb, archive.revision.group, archive.revision.artifact,
			archive.revision.version, archive.classifier, repo.toString());
		Resource resource = rb.build();
		return Collections.singletonMap(archive, resource);
	}

	private Map<Archive, Resource> parseMulti(Archive archive, File multi) throws Exception {
		Map<Archive, Resource> result = new HashMap<>();
		try (Jar jar = new Jar(multi)) {
			int n = 1000;
			for (Entry<String, aQute.bnd.osgi.Resource> entry : jar.getResources()
				.entrySet()) {
				String path = entry.getKey()
					.toLowerCase();
				if (path.endsWith(".jar")) {
					Archive nextArchive = new Archive(archive.revision, null, Archive.JAR_EXTENSION,
						String.format("%s%04d", archive.classifier, n));
					n++;
					File dest = repo.toLocalFile(nextArchive);
					if (!dest.isFile() || dest.lastModified() < multi.lastModified()) {
						try (OutputStream out = IO.outputStream(dest)) {
							entry.getValue()
								.write(out);
						}
						dest.setLastModified(multi.lastModified());
					}
					result.putAll(parseSingleOrMultiFile(nextArchive, dest));
				}
			}
		}
		result.putAll(parseSingle(archive, multi));
		return result;
	}

	boolean refresh(Runnable refreshAction) throws Exception {
		if (indexFile.lastModified() != lastModified && last + 10000 < System.currentTimeMillis()) {
			last = System.currentTimeMillis();
			Promise<Boolean> serializer = serialize(this::load).onResolve(refreshAction);
			sync(serializer);
			return true;
		}
		return false;
	}

	Promise<Boolean> refresh(Archive archive) {
		return serialize(() -> repo.get(archive, true)
			.map(file -> {
				removeWithDerived(archive);
				if (file == null) {
					Map<Archive, Resource> failed = failed(archive, "Not found");
					archives.putAll(failed);
					return Boolean.FALSE;
				}
				try {
					Map<Archive, Resource> result = parseSingleOrMultiFile(archive, file);
					archives.putAll(result);
					return Boolean.TRUE;
				} catch (Exception e) {
					Map<Archive, Resource> failed = failed(archive, e);
					archives.putAll(failed);
					return Boolean.FALSE;
				}
			}));
	}

	private Set<Archive> read(File file) throws IOException {
		if (!file.isFile()) {
			this.status = "Not an index file: " + file;
			return Collections.emptySet();
		}
		return read(IO.collect(file), true);
	}

	private Set<Archive> read(String source, boolean macro) {
		Set<Archive> archives = Strings.splitLinesAsStream(source)
			.map(s -> toArchive(s, macro))
			.filter(Objects::nonNull)
			.collect(toSet());
		logger.debug("loaded index {}", archives);
		return archives;
	}

	private boolean isBsn(String bsn, Resource resource) {
		BundleCap bundle = ResourceUtils.getBundleCapability(resource);
		if (bundle == null)
			return false;

		String osgi_wiring_bundle = bundle.osgi_wiring_bundle();
		return bsn.equals(osgi_wiring_bundle);
	}

	private void save(Set<Archive> add, Set<Archive> remove) throws Exception {
		try (Formatter f = new Formatter()) {
			if (indexFile.isFile()) {
				String content = IO.collect(indexFile);
				Strings.splitLinesAsStream(content)
					.filter(s -> {
						Archive archive = toArchive(s, true);
						if (archive == null)
							return true;

						return !remove.contains(archive);
					})
					.forEach(archive -> f.format("%s\n", archive));
			}

			add.forEach(archive -> f.format("%s\n", archive));
			if (!indexFile.getParentFile()
				.isDirectory())
				IO.mkdirs(indexFile.getParentFile());
			IO.store(f.toString(), indexFile);
		}

		lastModified = indexFile.lastModified();
	}

	private Archive toArchive(String s, boolean macro) {
		s = Strings.trim(s);
		if (s.startsWith("#") || s.isEmpty())
			return null;

		if (macro)
			s = replacer.process(s);
		int n = s.indexOf("#");
		if (n > 0) {
			s = s.substring(0, n);
		}
		s = Strings.trim(s);
		Archive archive = Archive.valueOf(s);
		if (archive != null)
			return archive;

		if (status == null)
			status = "Invalid GAV " + s;
		return null;
	}

	BridgeRepository getBridge() {
		sync(updateSerializer);
		return bridge.get();
	}

	/*
	 * Create a resource with error information
	 */
	private Resource info(Archive archive, String msg) {
		ResourceBuilder rb = new ResourceBuilder();
		String bsn = archive.getWithoutVersion();
		MavenVersion version = archive.revision.version;
		BridgeRepository.addInformationCapability(rb, bsn, version.getOSGiVersion(), repo.toString(), msg);
		MavenCapability.addMavenCapability(rb, archive.revision.group, archive.revision.artifact,
			archive.revision.version, archive.classifier, repo.toString());
		return rb.build();
	}

	Set<Archive> getArchives() {
		sync(updateSerializer);
		return archives.keySet();
	}

	Archive find(String bsn, Version version) throws Exception {

		//
		// First try if this is a BSN, not a GAV
		//

		if (bsn.indexOf(':') < 0) {
			ResourceInfo info = getBridge().getInfo(bsn, version);
			if (info == null)
				return null;

			Resource resource = info.getResource();
			return getArchive(resource);
		}

		//
		// Handle the GAV. Notice that the GAV can also contain extension +
		// classifier
		//

		Archive tmp = Archive.valueOf(bsn + ":" + version);
		if (tmp == null)
			return null;

		return getArchives().stream()
			.filter(archive -> archive.revision.program.equals(tmp.revision.program)
				&& archive.revision.version.getOSGiVersion()
					.equals(version)
				&& tmp.classifier.equals(archive.classifier))
			.findFirst()
			.orElse(null);
	}

	/*
	 * Answer the versions of a bsn If the bsn is a GA then we get the versions
	 * from the archives we have in our index. Otherwise, we use the repository
	 * bridge
	 */

	SortedSet<Version> versions(String bsn) throws Exception {

		if (bsn.indexOf(':') > 0) {

			//
			// Is a GAV
			//

			String[] parts = Program.split(bsn);

			if (parts.length >= 2) {
				String classifier = parts.length > 2 ? parts[parts.length - 1] : "";

				Program p = Program.valueOf(parts[0], parts[1]);
				if (p != null) {
					SortedSet<Version> collect = getArchives().stream()
						.filter(archive -> archive.revision.program.equals(p) && archive.classifier.equals(classifier))
						.map(archive -> archive.revision.version.getOSGiVersion())
						.collect(toCollection(TreeSet::new));
					return collect;
				}
			}
		}

		//
		// Non GAVs are left to the bridge
		//

		return getBridge().versions(bsn);
	}

	String tooltip(Object[] target) throws Exception {

		String tooltip = getBridge().tooltip(target);
		switch (target.length) {
			case 2 :
				ResourceInfo info = getBridge().getInfo((String) target[0], (Version) target[1]);

				Archive archive = getArchive(info.getResource());
				if (archive != null) {
					tooltip += "Coordinates: " + archive + "\n";
				}
		}
		return tooltip;
	}

	private Archive getArchive(Resource resource) {
		if (resource == null)
			return null;

		Archive archive = MapStream.of(archives)
			.filterValue(v -> v == resource)
			.keys()
			.findFirst()
			.orElse(null);
		return archive;
	}

	private void sync(Promise<?> promise) {
		try {
			promise.getFailure();
		} catch (InterruptedException e) {
			logger.info("Interrupted");
			Thread.currentThread()
				.interrupt();
		}
	}

	//
	// Deprecated to not have to increase the version with major
	//
	@Deprecated
	public class BundleDescriptor extends ResourceDescriptor {
		public long		lastModified;
		public Archive	archive;
		public boolean	merged;

		public Resource getResource() {
			throw new UnsupportedOperationException();
		}
	}

	public String getStatus() {
		if (status != null)
			return status;

		BridgeRepository peek = bridge.peek();
		if (peek != null) {
			return peek.getStatus();
		}
		return null;
	}
}
