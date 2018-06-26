package aQute.bnd.repository.maven.pom.provider;

import static aQute.bnd.osgi.repository.BridgeRepository.addInformationCapability;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

import java.io.File;
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

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
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
	static final String						ROOT		= "<>";
	final ConcurrentMap<Archive, Resource>	resources	= new ConcurrentHashMap<>();
	private final PromiseFactory			promiseFactory;
	final List<Revision>					revisions;
	final List<URI>							uris;
	final AtomicInteger						count		= new AtomicInteger(-1);
	final Deferred<Map<Archive, Resource>>	deferred;
	final MavenRepository					repo;
	final HttpClient						client;
	final boolean							transitive;

	Traverser(MavenRepository repo, HttpClient client, boolean transitive) {
		this.repo = repo;
		this.client = client;
		this.promiseFactory = client.promiseFactory();
		this.deferred = promiseFactory.deferred();
		this.transitive = transitive;
		this.revisions = new ArrayList<>();
		this.uris = new ArrayList<>();
	}

	Traverser revisions(Collection<Revision> revisions) {
		this.revisions.addAll(revisions);
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
						parsePom(pom, ROOT);
					}
				} else {
					for (Revision revision : revisions)
						parse(revision.archive("jar", null), ROOT);
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

	private void parse(final Archive archive, final String parent) {
		if (transitive || parent == ROOT) {
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
						parseArchive(archive);
					} catch (Throwable throwable) {
						logger.debug(" failed to parse archive {}: {}", archive, throwable);
						ResourceBuilder rb = new ResourceBuilder();
						String bsn = archive.revision.program.toString();
						Version version = toFrameworkVersion(archive.revision.version.getOSGiVersion());
						addReserveIdentity(rb, bsn, version);
						addInformationCapability(rb, archive.toString(), parent, throwable);
						resources.put(archive, rb.build());
					} finally {
						finish();
					}
				});
		}
	}

	private Version toFrameworkVersion(aQute.bnd.version.Version v) {
		return new Version(v.getMajor(), v.getMinor(), v.getMicro(), v.getQualifier());
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

	private void parseArchive(Archive archive) throws Exception {
		POM pom = repo.getPom(archive.getRevision());
		String parent = archive.getRevision()
			.toString();

		if (pom == null) {
			logger.debug("no pom found for archive {} ", archive);
			if (!archive.isPom())
				parseResource(archive, parent);
			return;
		}
		parsePom(pom, parent);

		if (!pom.isPomOnly())
			parseResource(archive, parent);
	}

	private void parsePom(POM pom, String parent) throws Exception {

		Map<Program, Dependency> dependencies = pom.getDependencies(EnumSet.of(MavenScope.compile, MavenScope.runtime),
			false);
		for (Dependency d : dependencies.values()) {

			d.bindToVersion(repo);
			Archive archive = d.getArchive();
			if (archive == null) {
				logger.debug("pom {} has bad dependency {}", pom.getRevision(), d);
			} else
				parse(archive, parent);
		}
	}

	private void parseResource(Archive archive, String parent) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		Version frameworkVersion = toFrameworkVersion(archive.revision.version.getOSGiVersion());
		String bsn = archive.revision.program.toString();

		try {
			File binary = repo.get(archive)
				.getValue();

			if (!rb.addFile(binary, binary.toURI())) {
				// no identity
				addReserveIdentity(rb, bsn, frameworkVersion);
			}
			addInformationCapability(rb, archive.toString(), parent);
		} catch (Exception e) {
			addReserveIdentity(rb, bsn, frameworkVersion);
			addInformationCapability(rb, archive.toString(), parent, e);
		}
		resources.put(archive, rb.build());
	}

	private void addReserveIdentity(ResourceBuilder rb, String bsn, Version version) {
		try {
			CapabilityBuilder c = new CapabilityBuilder(IDENTITY_NAMESPACE);
			c.addAttribute(IDENTITY_NAMESPACE, bsn);
			c.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
			c.addAttribute(CAPABILITY_TYPE_ATTRIBUTE, "application/java-archive");
			rb.addCapability(c);
		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

	public Traverser revision(Revision revision) {
		revisions.add(revision);
		return this;
	}

}
