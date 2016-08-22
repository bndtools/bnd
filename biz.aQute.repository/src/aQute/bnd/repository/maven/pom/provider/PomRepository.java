package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
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
	final Revision			revision;
	final URI				revisionUrl;
	final HttpClient		client;

	PomRepository(MavenRepository repo, HttpClient client, File location, Revision revision) throws Exception {
		super(repo, location);
		this.revision = revision;
		this.revisionUrl = null;
		this.client = client;
		read();
	}

	PomRepository(MavenRepository repo, HttpClient client, File location, URI revision) throws Exception {
		super(repo, location);
		this.revisionUrl = revision;
		this.revision = null;
		this.client = client;
		read();
	}

	void refresh() throws Exception {
		if (revisionUrl != null)
			read(revisionUrl);
		else
			read(revision);
	}

	void read(URI revision) throws Exception {
		Traverser traverser = new Traverser(getMavenRepository(), revision, client, Processor.getExecutor());
		Promise<Map<Archive,Resource>> p = traverser.getResources();
		Collection<Resource> resources = p.getValue().values();
		set(resources);
		save(revision.toString(), resources, getLocation());
	}

	void read(Revision revision) throws Exception {
		Traverser traverser = new Traverser(getMavenRepository(), revision, client, Processor.getExecutor());
		Promise<Map<Archive,Resource>> p = traverser.getResources();
		Collection<Resource> resources = p.getValue().values();
		set(resources);
		save(revision.toString(), resources, getLocation());
	}

	void save(String revision, Collection< ? extends Resource> resources, File location) throws Exception, IOException {
		XMLResourceGenerator generator = new XMLResourceGenerator();
		generator.resources(resources);
		generator.name(revision);
		generator.save(location);
	}

	void read() throws Exception {
		if (isStale()) {
			refresh();
		} else {
			try (XMLResourceParser parser = new XMLResourceParser(getLocation());) {
				List<Resource> resources = parser.parse();
				addAll(resources);
			}
		}
	}

	private boolean isStale() {
		if (!getLocation().isFile())
			return true;

		if ( revisionUrl != null) {
			if ( "file".equalsIgnoreCase(revisionUrl.getScheme())) {
				File file = new File( revisionUrl);
				if (file.isFile() && file.lastModified() > getLocation().lastModified()) {
					return true;
				}
			}
		} else {
			try {
				File file = getMavenRepository().get(revision.getPomArchive(), false).getValue();
				if (file.isFile() && file.lastModified() > getLocation().lastModified()) {
					return true;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return false;
	}
}
