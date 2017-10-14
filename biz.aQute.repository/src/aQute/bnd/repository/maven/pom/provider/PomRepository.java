package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRepository;

class PomRepository extends InnerRepository {
	static final String		BND_MAVEN						= "bnd.maven";
	static final String		BND_MAVEN_EXCEPTION_ATTRIBUTE	= "exception";
	static final String		BND_MAVEN_ARCHIVE_ATTRIBUTE		= "archive";
	static final String		BND_MAVEN_REVISION_ATTRIBUTE	= "revision";
	final List<Revision>	revisions;
	final List<URI>			uris;
	final HttpClient		client;
	final boolean			transitive;

	PomRepository(MavenRepository repo, HttpClient client, File location, boolean transitive) {
		super(repo, location);
		this.transitive = transitive;
		this.revisions = new ArrayList<>();
		this.uris = new ArrayList<>();
		this.client = client;
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

	void refresh() throws Exception {
		if (!uris.isEmpty())
			readUris();
		else
			readRevisons();
	}

	void readUris() throws Exception {
		save(new Traverser(getMavenRepository(), client, client.executor(), transitive).uris(uris));
	}

	void readRevisons() throws Exception {
		save(new Traverser(getMavenRepository(), client, client.executor(), transitive).revisions(revisions));
	}

	void save(Traverser traverser) throws Exception {
		Promise<Map<Archive,Resource>> p = traverser.getResources();
		Collection<Resource> resources = p.getValue().values();
		set(resources);
		save(getMavenRepository().getName(), resources, getLocation());
	}

	void save(String name, Collection< ? extends Resource> resources, File location) throws Exception, IOException {
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

	private boolean isStale() {
		if (!getLocation().isFile())
			return true;

		if (!uris.isEmpty()) {
			for (URI uri : uris) {
				if ("file".equalsIgnoreCase(uri.getScheme())) {
					File file = new File(uri);
					if (file.isFile() && file.lastModified() > getLocation().lastModified()) {
						return true;
					}
				}
			}
		} else {
			try {
				for (Revision revision : revisions) {
					File file = getMavenRepository().get(revision.getPomArchive()).getValue();
					if (file.isFile() && file.lastModified() > getLocation().lastModified()) {
						return true;
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return false;
	}
}
