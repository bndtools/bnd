package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.service.url.TaggedData;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRepository;

class PomRepository extends InnerRepository {
	private final static Logger		logger							= LoggerFactory.getLogger(PomRepository.class);
	static final String				BND_MAVEN						= "bnd.maven";
	static final String				BND_MAVEN_EXCEPTION_ATTRIBUTE	= "exception";
	static final String				BND_MAVEN_ARCHIVE_ATTRIBUTE		= "archive";
	static final String				BND_MAVEN_REVISION_ATTRIBUTE	= "revision";
	final List<Revision>			revisions;
	final List<URI>					uris;
	private final HttpClient		client;
	private final PromiseFactory	promiseFactory;
	final boolean					transitive;

	PomRepository(MavenRepository repo, HttpClient client, File location, boolean transitive) {
		super(repo, location);
		this.transitive = transitive;
		this.revisions = new ArrayList<>();
		this.uris = new ArrayList<>();
		this.client = client;
		this.promiseFactory = client.promiseFactory();
	}

	public PomRepository(MavenRepository repo, HttpClient client, File location) {
		this(repo, client, location, true);
	}

	PomRepository revisions(Collection<Revision> revisions) throws Exception {
		this.revisions.addAll(revisions);
		read();
		return this;
	}

	PomRepository uris(Collection<URI> uris) throws Exception {
		this.uris.addAll(uris);
		read();
		return this;
	}

	@Override
	void refresh() throws Exception {
		if (!uris.isEmpty())
			readUris();
		else
			readRevisons();
	}

	void readUris() throws Exception {
		save(new Traverser(getMavenRepository(), client, transitive).uris(uris));
	}

	void readRevisons() throws Exception {
		save(new Traverser(getMavenRepository(), client, transitive).revisions(revisions));
	}

	void save(Traverser traverser) throws Exception {
		Promise<Map<Archive, Resource>> p = traverser.getResources();
		Collection<Resource> resources = p.getValue()
			.values();
		set(resources);
		save(getMavenRepository().getName(), resources, getLocation());
	}

	void save(String name, Collection<? extends Resource> resources, File location) throws Exception, IOException {
		XMLResourceGenerator generator = new XMLResourceGenerator();
		generator.resources(resources);
		generator.name(name);
		generator.save(location);
	}

	void read() throws Exception {
		if (isStale()) {
			refresh();
		} else {
			try (XMLResourceParser parser = new XMLResourceParser(getLocation())) {
				List<Resource> resources = parser.parse();
				addAll(resources);
			}
		}
	}

	@Override
	boolean isStale() throws Exception {
		if (!getLocation().isFile()) {
			return true;
		}

		long lastModified = getLocation().lastModified();
		Deferred<List<Void>> freshness = promiseFactory.deferred();
		List<Promise<Void>> promises = new ArrayList<>();
		if (!uris.isEmpty()) {
			for (URI uri : uris) {
				if (freshness.getPromise()
					.isDone()) {
					break; // early exit if staleness already detected
				}
				try {
					if ("file".equalsIgnoreCase(uri.getScheme())) {
						File file = new File(uri);
						if (file.isFile() && file.lastModified() > lastModified) {
							return true;
						}
					} else {
						// check for staleness of non-file-URI
						Promise<TaggedData> async = client.build()
							.useCache()
							.asTag()
							.async(uri);
						promises.add(async.then(resolved -> {
							switch (resolved.getValue()
								.getState()) {
								case OTHER :
									// in the offline case
									// ignore might be best here
									logger.debug("Could not verify {}", uri);
									break;

								case UNMODIFIED :
									break;

								case NOT_FOUND :
								case UPDATED :
								default :
									logger.debug("Found {} to be stale", uri);
									freshness.fail(new Exception("stale"));
							}
							return null;
						}, resolved -> {
							logger.debug("Could not verify {}: {}", uri, resolved.getFailure());
							freshness.fail(resolved.getFailure());
						}));
					}
				} catch (Exception e) {
					logger.debug("Checking stale status: {}: {}", uri, e);
				}
			}
		} else {
			for (Revision revision : revisions) {
				if (freshness.getPromise()
					.isDone()) {
					break; // early exit if staleness already detected
				}
				Promise<File> file = getMavenRepository().get(revision.getPomArchive());
				promises.add(file.then(resolved -> {
					File f = resolved.getValue();
					if (f.isFile() && f.lastModified() > lastModified) {
						logger.debug("Found {} to be stale", revision);
						freshness.fail(new Exception("stale"));
					}
					return null;
				}, resolved -> {
					logger.debug("Could not verify {}: {}", revision, resolved.getFailure());
					freshness.fail(resolved.getFailure());
				}));
			}
		}

		// Resolve when all promises checked
		if (!freshness.getPromise()
			.isDone()) {
			Promise<List<Void>> all = promiseFactory.all(promises);
			freshness.resolveWith(all);
		}

		// Block until freshness is resolved
		return freshness.getPromise()
			.getFailure() != null;
	}
}
