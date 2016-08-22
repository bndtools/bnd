package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;

public class SearchRepository extends InnerRepository {

	final static long			DEFAULT_MAX_STALE	= TimeUnit.HOURS.toMillis(1);
	final static DocComparator	DOC_COMPARATOR		= new DocComparator();
	final static DocToRevision	DOC_TO_REVISION		= new DocToRevision();
	final String				query;
	final String				queryUrl;
	final Reporter				reporter;
	final HttpClient			client;
	final File					cacheFile;

	SearchRepository(MavenRepository repo, File location, String query, String queryUrl, Reporter reporter,
			HttpClient client) throws Exception {
		super(repo, location);
		this.query = query;
		this.queryUrl = queryUrl;
		this.reporter = reporter;
		this.client = client;
		cacheFile = new File(location.getParentFile(), "query.txt");
		read();
	}

	void refresh() throws Exception {
		SearchResult result = query();
		Traverser traverser = new Traverser(getMavenRepository(), result.response.docsToRevisions(), null,
				Processor.getExecutor());
		Promise<Map<Archive,Resource>> p = traverser.getResources();
		Collection<Resource> resources = p.getValue().values();
		set(resources);
		save(query, resources, getLocation());
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
			try (XMLResourceParser parser = new XMLResourceParser(getLocation())) {
				List<Resource> resources = parser.parse();
				addAll(resources);
			}
		}
	}

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
				reporter.trace("Searching %s", query);

				SearchResult result = client.build()
						.headers("User-Agent", "Bnd")
						.useCache(cacheFile, DEFAULT_MAX_STALE)
						.get(SearchResult.class)
						.go(url);

				reporter.trace("Searched %s", result);

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
		public SearchResult() throws Exception {};

		public long getLastModified() {
			Optional<Doc> doc = Arrays.asList(response.docs).stream().max(DOC_COMPARATOR);
			if (doc.isPresent()) {
				return doc.get().timestamp;
			}
			return -1;
		}

		public ResponseHeader	responseHeader;
		public Response			response;
	}

	public static class ResponseHeader {
		public ResponseHeader() throws Exception {}

		public Map<String,String> params;
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
			return list = Arrays.asList(docs).stream().map(DOC_TO_REVISION).collect(Collectors.<Revision> toList());
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
			return Revision.valueOf(toString());
		}

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

	public static class DocComparator implements Comparator<Doc> {

		@Override
		public int compare(Doc doc1, Doc doc2) {
			return (doc1.timestamp < doc2.timestamp) ? (-1) : (doc1.timestamp > doc2.timestamp ? 1 : 0);
		}
	}

	public static class DocToRevision implements Function<Doc,Revision> {

		@Override
		public Revision apply(Doc t) {
			return t.toRevision();
		}
	}

}
