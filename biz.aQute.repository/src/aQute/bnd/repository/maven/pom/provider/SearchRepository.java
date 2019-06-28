package aQute.bnd.repository.maven.pom.provider;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;

class SearchRepository extends InnerRepository {
	private final static Logger	logger				= LoggerFactory.getLogger(SearchRepository.class);

	final static long			DEFAULT_MAX_STALE	= TimeUnit.HOURS.toMillis(1);
	final String				query;
	final String				queryUrl;
	final Reporter				reporter;
	final HttpClient			client;
	final File					cacheFile;
	final boolean				transitive;

	SearchRepository(MavenRepository repo, File location, String query, String queryUrl, Reporter reporter,
		HttpClient client, boolean transitive) throws Exception {
		super(repo, location);
		this.query = query;
		this.queryUrl = queryUrl;
		this.reporter = reporter;
		this.client = client;
		this.transitive = transitive;
		cacheFile = new File(location.getParentFile(), "pom-" + repo.getName() + ".query");
		read();
	}

	@Override
	void refresh() throws Exception {
		SearchResult result = query();
		Traverser traverser = new Traverser(getMavenRepository(), client, transitive)
			.revisions(result.response.docsToRevisions());
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
	boolean isStale() {
		if (!getLocation().isFile())
			return true;

		try {
			SearchResult result = query();
			if (result.getLastModified() > getLocation().lastModified()) {
				return true;
			}
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	SearchResult query() throws Exception {
		URL url = new URL(queryUrl + '?' + query);
		int n = 0;
		while (true) {
			try {
				logger.debug("Searching {}", query);

				SearchResult result = client.build()
					.headers("User-Agent", "Bnd")
					.useCache(cacheFile, DEFAULT_MAX_STALE)
					.get(SearchResult.class)
					.go(url);

				logger.debug("Searched {}", result);

				return result;
			} catch (Exception e) {
				n++;
				if (n > 3)
					throw e;
				Thread.sleep(1000 * n);
			}
		}
	}

	public static class SearchResult {
		public SearchResult() throws Exception {}

		public long getLastModified() {
			return Arrays.stream(response.docs)
				.mapToLong(doc -> doc.timestamp)
				.max()
				.orElse(-1);
		}

		public ResponseHeader	responseHeader;
		public Response			response;
	}

	public static class ResponseHeader {
		public ResponseHeader() throws Exception {}

		public Map<String, String> params;
		/*
		 * "responseHeader":{ "params":{ "spellcheck":"true", "fl":
		 * "id,g,a,latestVersion,p,ec,repositoryId,text,timestamp,versionCount",
		 * "sort":"score desc,timestamp desc,g asc,a asc", "indent":"off",
		 * "q":"g:com.liferay", "spellcheck.count":"5", "wt":"json", "rows":"1",
		 * "version":"2.2" } }
		 */
	}

	public static class Response {
		public Response() throws Exception {}

		public long		numFound;
		public long		start;
		public Doc[]	docs;
		List<Revision>	list;

		public List<Revision> docsToRevisions() {
			if (list != null)
				return list;
			return list = Arrays.stream(docs)
				.map(Doc::toRevision)
				.collect(toList());
		}
		/*
		 * "response":{ "numFound":648, "start":0, "docs":[ ... ] }
		 */
	}

	public static class Doc {
		public Doc() throws Exception {}

		public String	id;
		public String	g;
		public String	a;
		public String	v;
		public String	latestVersion;
		public String	repositoryId;
		public String	p;
		public long		timestamp;
		public int		versionCount;
		public String[]	ec;

		public String getVersion() {
			return (v != null) ? v : latestVersion;
		}

		public Revision toRevision() {
			return Program.valueOf(g, a)
				.version(getVersion());
		}

		@Override
		public String toString() {
			return String.format("%s:%s:%s", g, a, getVersion());
		}
		/*
		 * "id":"com.liferay:com.liferay.poshi.runner", "g":"com.liferay",
		 * "a":"com.liferay.poshi.runner", "latestVersion":"1.0.44",
		 * "repositoryId":"central", "p":"jar", "timestamp":1471476046000,
		 * "versionCount":45, "ec":[ "-sources.jar", ".jar", ".pom" ]
		 */
	}

}
