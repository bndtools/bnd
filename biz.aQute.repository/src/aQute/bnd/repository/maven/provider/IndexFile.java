package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
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
	private final static Logger			logger		= LoggerFactory.getLogger(IndexFile.class);

	final File							indexFile;
	final IMavenRepo					repo;
	private final Processor				domain;
	private final Macro					replacer;

	final Reporter						reporter;
	final PromiseFactory				promiseFactory;
	final Map<Archive, Resource>		archives	= new ConcurrentHashMap<>();
	final String[]						multi;
	final String						source;

	private long						lastModified;
	private long						last		= 0L;
	volatile Promise<BridgeRepository>	bridge;

	/*
	 * Constructor
	 */
	IndexFile(Processor domain, Reporter reporter, File file, String source, IMavenRepo repo,
		PromiseFactory promiseFactory, String... multi) throws Exception {
		this.source = source;
		this.domain = (domain != null) ? domain : new Processor();
		this.replacer = this.domain.getReplacer();
		this.reporter = reporter;
		this.indexFile = file;
		this.repo = repo;
		this.promiseFactory = promiseFactory;
		this.multi = multi;
		this.bridge = null;
	}

	/*
	 * Open, will refresh this repository, which will load the file
	 */
	void open() throws Exception {
		this.bridge = load();
	}

	/*
	 * Persistently add an archive to the index file and refresh. If the archive
	 * is not kept then this is a noop
	 */
	synchronized void add(Archive archive) throws Exception {
		if (archives.containsKey(archive))
			return;

		logger.debug("adding {}", archive);
		this.bridge = update(Arrays.asList(archive));
		save(Collections.singleton(archive), Collections.emptySet());
	}

	/*
	 * Remove an archive from the index file and refresh. If the archive is not
	 * kept then this is a noop
	 */
	synchronized void remove(Archive archive) throws Exception {
		if (removeWithDerived(archive)) {
			logger.debug("remove {}", archive);
			this.bridge = update(null);
			save(Collections.emptySet(), Collections.singleton(archive));
		}
	}

	/*
	 * Remove all resources with a given bsn. This function does not recognize
	 * GAV bsns. If the primary BSN has derived resources then they are also
	 * removed.
	 */
	synchronized void remove(String bsn) throws Exception {
		logger.debug("remove bsn {}", bsn);

		Set<Archive> toRemove = archives.entrySet()
			.stream()
			.filter(entry -> {
				return isBsn(bsn, entry.getValue());
			})
			.map(entry -> {
				return entry.getKey();
			})
			.collect(Collectors.toSet());

		boolean changed = false;

		for (Archive archive : toRemove) {
			changed |= removeWithDerived(archive);
		}

		if (changed) {
			this.bridge = update(null);
			save(Collections.emptySet(), toRemove);
		}
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
		boolean match = inMap.revision.equals(parent.revision) && !"".equals(inMap.classifier);
		return match;
	}

	/*
	 * Load the index file file. This method ensures the bridge
	 * @throws Exception
	 */
	private synchronized Promise<BridgeRepository> load() throws Exception {
		lastModified = indexFile.lastModified();
		Set<Archive> toBeAdded = new HashSet<>();

		if (indexFile.isFile()) {
			toBeAdded.addAll(read(indexFile));
		}
		if (source != null) {
			toBeAdded.addAll(read(source, false));
		}

		Set<Archive> older = this.archives.keySet();
		Set<Archive> toBeDeleted = new HashSet<>(older);
		toBeDeleted.removeAll(toBeAdded);
		toBeAdded.removeAll(older);

		archives.keySet()
			.removeAll(toBeDeleted);
		return update(toBeAdded);

	}

	/*
	 * Update the set of archives. This will add the given archives and then
	 * create a new bridge promise. All newly added archives are scheduled to be
	 * download and after downloaded are parsed. This method should only be
	 * called when synchronized.
	 */
	private Promise<BridgeRepository> update(Collection<Archive> toAdd) throws Exception {

		List<Promise<Boolean>> results = new ArrayList<>();
		if (toAdd != null) {
			for (Archive a : toAdd) {
				put(a, info(a, "Loading ..."));

				if (!a.isSnapshot()) {
					File localFile = repo.toLocalFile(a);
					if (localFile.isFile() && localFile.length() > 0) {
						parseSingleOrMultiFile(a, localFile);
						continue;
					}
				}

				Promise<Boolean> promise = repo.get(a)
					.map(file -> {
						if (file == null) {
							return failed(a, "Not found");

						} else {
							parseSingleOrMultiFile(a, file);
							return true;
						}
					})
					.recover(p -> {
						// TODO log
						return failed(a, getMessage(p.getFailure()));
					});

				results.add(promise);
			}
		}
		// snapshot archive resources
		ResourcesRepository resourcesRepository = promiseFactory.all(results)
				.map(ignore -> new ResourcesRepository(archives.values()))
				.getValue();

		return promiseFactory.submit(() -> new BridgeRepository(resourcesRepository));
	}

	private Boolean failed(Archive a, String msg) throws InterruptedException {
		put(a, info(a, msg));
		return false;
	}

	private void put(Archive a, Resource resource) {
		archives.put(a, resource);
	}

	private String getMessage(Throwable failure) {
		failure = Exceptions.unrollCause(failure, InvocationTargetException.class);
		if (failure instanceof FileNotFoundException) {
			return "Not found";
		}
		return failure.getMessage();
	}

	private void parseSingleOrMultiFile(Archive archive, File file) {
		try {
			if (isMulti(file.getName())) {
				parseMulti(archive, file);
			} else {
				parseSingle(archive, file);
			}
		} catch (Exception e) {
			logger.warn("parse ", e);
		}
	}

	private boolean isMulti(String name) {
		if (multi == null)
			return false;

		String[] extension = Strings.extension(name.toLowerCase());
		return extension.length == 2 && Strings.in(multi, extension[1]);
	}

	private void parseSingle(Archive archive, File single) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		boolean hasIdentity = rb.addFile(single, single.toURI());

		if (!hasIdentity) {
			String name = archive.getWithoutVersion();
			MavenVersion version = archive.revision.version;

			BridgeRepository.addInformationCapability(rb, name, version.getOSGiVersion(), repo.toString(),
				"Not a bundle");
		}

		Resource resource = rb.build();
		put(archive, resource);
	}

	private void parseMulti(Archive archive, File multi) throws Exception {
		try (Jar jar = new Jar(multi)) {
			int n = 1000;
			for (Entry<String, aQute.bnd.osgi.Resource> entry : jar.getResources()
				.entrySet()) {
				String path = entry.getKey()
					.toLowerCase();
				if (path.endsWith(".jar")) {
					Archive nextArchive = new Archive(archive.revision, null, "jar",
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
					parseSingleOrMultiFile(nextArchive, dest);
				}
			}
		}
	}

	boolean refresh(Runnable refreshAction) throws Exception {
		if (indexFile.lastModified() != lastModified && last + 10000 < System.currentTimeMillis()) {
			last = System.currentTimeMillis();
			this.bridge = load().onResolve(refreshAction);
			return true;
		}
		return false;
	}

	private Set<Archive> read(File file) throws IOException {
		assert file.isFile();
		return read(IO.collect(file), true);
	}

	private Set<Archive> read(String source, boolean macro) {
		Set<Archive> archives = Strings.splitLinesAsStream(source)
			.map(s -> toArchive(s, macro))
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
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

	private synchronized void save(Set<Archive> add, Set<Archive> remove) throws Exception {

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
			s = Strings.trim(s.substring(0, n));
		}
		return Archive.valueOf(s);
	}

	BridgeRepository getBridge() {
		try {
			return bridge.getValue();
		} catch (InvocationTargetException e) {
			reporter.exception(Exceptions.unrollCause(e, InvocationTargetException.class), "Getting bridge failed");
		} catch (InterruptedException e) {
			logger.info("Interrupted");
			Thread.currentThread()
				.interrupt();
		}
		return new BridgeRepository();
	}

	/*
	 * Create a resource with error information
	 */
	private Resource info(Archive a, String msg) {
		ResourceBuilder rb = new ResourceBuilder();
		String bsn = a.getWithoutVersion();
		MavenVersion version = a.revision.version;
		BridgeRepository.addInformationCapability(rb, bsn, version.getOSGiVersion(), repo.toString(), msg);
		return rb.build();
	}

	Collection<Archive> getArchives() {
		return archives.keySet();
	}

	Archive find(String bsn, Version version) throws Exception {

		//
		// First try if this is a BSN, not a GAV
		//

		String[] parts = bsn.split(":");
		if (parts.length == 1) {
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

		return archives.keySet()
			.stream()
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

			String[] parts = bsn.split(":");

			if (parts.length >= 2) {
				String classifier = parts.length > 2 ? parts[parts.length - 1] : "";

				Program p = Program.valueOf(parts[0], parts[1]);
				if (p != null) {
					Set<Version> collect = archives.keySet()
						.stream()
						.filter(archive -> archive.revision.program.equals(p) && archive.classifier.equals(classifier))
						.map(archive -> archive.revision.version.getOSGiVersion())
						.collect(Collectors.toSet());
					return new TreeSet<>(collect);
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

		Archive archive = archives.entrySet()
			.stream()
			.filter((Map.Entry<Archive, Resource> e) -> e.getValue() == resource)
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
		return archive;
	}

	void sync() {
		getBridge();
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

}
