package aQute.bnd.repository.maven.pom.provider;

import static aQute.bnd.osgi.repository.BridgeRepository.addInformationCapability;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.maven.MavenCapability;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.MavenVersion;
import aQute.maven.api.Archive;
import aQute.maven.api.IPom.Dependency;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRepository;
import aQute.maven.provider.POM;

class Traverser {
	private final static Logger				logger		= LoggerFactory.getLogger(Traverser.class);
	static final Resource					DUMMY		= new ResourceBuilder().build();
	final ConcurrentMap<Archive, Resource>	resources	= new ConcurrentHashMap<>();
	private final PromiseFactory			promiseFactory;
	final List<Archive>						archives;
	final List<URI>							uris;
	final AtomicInteger						count		= new AtomicInteger(-1);
	final Deferred<Map<Archive, Resource>>	deferred;
	final MavenRepository					repo;
	final HttpClient						client;
	final boolean							transitive;
	final boolean							dependencyManagement;

	Traverser(MavenRepository repo, HttpClient client, boolean transitive, boolean dependencyManagement) {
		this.repo = repo;
		this.client = client;
		this.dependencyManagement = dependencyManagement;
		this.promiseFactory = client.promiseFactory();
		this.deferred = promiseFactory.deferred();
		this.transitive = transitive;
		this.archives = new ArrayList<>();
		this.uris = new ArrayList<>();
	}

	Traverser revisions(Collection<Revision> revisions) {
		Collection<Archive> archives = revisions.stream()
			.map(r -> r.archive(Archive.JAR_EXTENSION, null))
			.collect(Collectors.toList());
		return archives(archives);
	}

	Traverser archives(Collection<Archive> archives) {
		this.archives.addAll(archives);
		return this;
	}

	Traverser uris(Collection<URI> uris) {
		this.uris.addAll(uris);
		return this;
	}

	Promise<Map<Archive, Resource>> getResources() throws Exception {
		/*
		 * We don't want to resolve until all the work is queued so we
		 * initialize the count to 1 and call finish after queuing all the work.
		 */
		if (count.compareAndSet(-1, 1)) {
			try {
				if (!uris.isEmpty()) {
					for (URI uri : uris) {
						File in = client.build()
							.useCache()
							.age(1, TimeUnit.DAYS)
							.go(uri);
						POM pom = new POM(repo, in);
						parsePom(pom, true, true);
					}
				} else {
					for (Archive archive : archives)
						parse(archive, true, true);
				}
			} finally {
				finish();
			}
		}
		return deferred.getPromise();
	}

	private void finish() {
		// If count goes to zero, then we resolve
		if (count.decrementAndGet() == 0) {
			deferred.resolve(prune(resources));
		}
	}

	/**
	 * Parse the specified Archive.
	 *
	 * @param archive The {@link Archive} to parse
	 * @param rootDependency The archive is a root dependency.
	 * @param parse Request parsing even when non-transitive.
	 */
	private void parse(Archive archive, boolean rootDependency, boolean parse) {
		if (parse || transitive) {
			//
			// Prune duplicates by adding the archive to a set. We
			// use a dummy for the resource, the resource is set later
			//

			Resource prev = resources.putIfAbsent(archive, DUMMY);
			if (prev != null)
				return;

			//
			// Every parse must be matched by a background
			// execution of parseArchive. So we count the enters
			// and then decrement at the end of the background task
			// if we go to 0 then we've done it all
			//

			count.incrementAndGet();
			promiseFactory.executor()
				.execute(() -> {
					try {
						logger.debug("parse archive {}", archive);
						parseArchive(archive, rootDependency);
					} catch (Throwable throwable) {
						logger.debug(" failed to parse archive {}: {}", archive, throwable);
						ResourceBuilder rb = new ResourceBuilder();
						String name = archive.getWithoutVersion();
						MavenVersion version = archive.revision.version;
						addInformationCapability(rb, name, version.getOSGiVersion(), archive.toString(),
							throwable.toString());
						MavenCapability.addMavenCapability(rb, archive.revision.group, archive.revision.artifact,
							version, archive.extension, archive.classifier, repo.getName());
						resources.put(archive, rb.build());
					} finally {
						finish();
					}
				});
		}
	}

	/*
	 * Remove the pom only archives, they've not overwritten the dummy value
	 * @param resources the resources parsed
	 * @return the pruned resources
	 */
	private Map<Archive, Resource> prune(Map<Archive, Resource> resources) {
		resources.entrySet()
			.removeIf(next -> next.getValue() == DUMMY);
		return resources;
	}

	/**
	 * Parse the specified Archive (asynchronously).
	 *
	 * @param archive The {@link Archive} to parse
	 * @param rootDependency The archive is a root dependency.
	 */
	private void parseArchive(Archive archive, boolean rootDependency) throws Exception {
		POM pom = repo.getPom(archive.getRevision());
		if (pom == null) {
			logger.debug("no pom found for archive {} ", archive);
			if (!archive.isPom())
				parseResource(archive);
			return;
		}
		// For a root dependency having "pom" packaging, we
		// need to parse its dependencies even if we don't want transitive
		// dependencies.
		boolean pomPackaging = pom.isPomOnly();
		parsePom(pom, false, pomPackaging && rootDependency);

		if (!pomPackaging) {
			parseResource(archive);
		}
	}

	/**
	 * Parse the specified POM.
	 *
	 * @param pom The {@link POM} to parse
	 * @param rootDependency The pom is a root dependency.
	 * @param parse Request parsing even when non-transitive.
	 */
	private void parsePom(POM pom, boolean rootDependency, boolean parse) throws Exception {
		Map<Program, Dependency> dependencies = pom.getDependencies(EnumSet.of(MavenScope.compile, MavenScope.runtime),
			transitive, dependencyManagement);
		for (Dependency d : dependencies.values()) {

			d.bindToVersion(repo);
			Archive archive = d.getArchive();
			if (archive == null) {
				logger.debug("pom {} has bad dependency {}", pom.getRevision(), d);
			} else
				parse(archive, rootDependency, parse);
		}
	}

	private void parseResource(Archive archive) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		String name = archive.getWithoutVersion();
		MavenVersion version = archive.revision.version;

		try {
			File binary = repo.get(archive)
				.getValue();

			boolean hasIdentity = rb.addFile(binary, binary.toURI());
			addInformationCapability(rb, name, version.getOSGiVersion(), archive.toString(),
				hasIdentity ? null : Constants.NOT_A_BUNDLE_S);
		} catch (Exception e) {
			Throwable theException = Exceptions.unrollCause(e, InvocationTargetException.class);
			addInformationCapability(rb, name, version.getOSGiVersion(), archive.toString(), theException.toString());
		}
		MavenCapability.addMavenCapability(rb, archive.revision.group, archive.revision.artifact,
			version, archive.extension, archive.classifier, repo.getName());
		resources.put(archive, rb.build());
	}

	public Traverser revision(Revision revision) {
		return archive(revision.archive(Archive.JAR_EXTENSION, null));
	}

	public Traverser archive(Archive archive) {
		archives.add(archive);
		return this;
	}

}
