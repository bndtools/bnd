package aQute.maven.nexus.provider;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.osgi.dto.DTO;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.glob.Glob;

public class Nexus {
	private final static String		MAVEN_INDEX_S	= "href\\s*=\\s*([\"'])(?<uri>[^\\./][^\"'\r\n]+)\\1";
	private final static Pattern	MAVEN_INDEX_P	= Pattern.compile(MAVEN_INDEX_S);
	final static Logger				logger			= LoggerFactory.getLogger(Nexus.class);
	private URI						uri;
	private HttpClient				client;
	private Executor				executor;

	public static class Asset extends DTO {
		public URI					downloadUrl;
		public String				path;
		public String				id;
		public String				repository;
		public String				format;
		public Map<String, String>	checksum;
	}

	public static class Assets extends DTO {
		public List<Asset>	items;
		public String		continuationToken;
	}

	@Deprecated
	public Nexus(URI uri, HttpClient client) throws URISyntaxException {
		this(uri, client, null);
	}

	public Nexus(URI uri, HttpClient client, Executor executor) throws URISyntaxException {
		this.executor = executor;
		this.uri = uri.toString()
			.endsWith("/") ? uri : new URI(uri.toString() + "/");
		this.client = client;
	}

	public HttpRequest<Object> request() {
		return client.build()
			.headers("Accept", "application/json")
			.headers("User-Agent", "bnd");
	}

	public List<URI> files() throws Exception {
		URI uri = this.uri.resolve("content/");
		List<URI> uris = new ArrayList<>();

		if (hasFiles(uris, uri))
			return uris;

		return null;
	}

	/**
	 * <pre>
	 *  <content> <data> <content-item> <resourceURI>
	 * https://oss.sonatype.org/service/local/repositories/orgosgi-1073/content/
	 * org/osgi/osgi.enroute.authenticator.github.provider/ </resourceURI>
	 * <relativePath>/org/osgi/osgi.enroute.authenticator.github.provider/</
	 * relativePath> <text>osgi.enroute.authenticator.github.provider</text>
	 * <leaf>false</leaf> <lastModified>2016-06-03 17:05:14.0 UTC</lastModified>
	 * <sizeOnDisk>-1</sizeOnDisk> </content-item> </data> </content>
	 * </pre>
	 */
	public boolean hasFiles(List<URI> list, URI uri) throws Exception {
		ContentDTO content = request().get(ContentDTO.class)
			.go(uri);
		if (content != null) {

			for (ContentDTO.ItemDTO item : content.data) {
				if (item.sizeOnDisk < 0) {
					files(list, item.resourceURI);
				} else {
					if (isReal(item.resourceURI))
						list.add(item.resourceURI);
				}
			}
			return true;
		}
		return false;
	}

	public void files(List<URI> list, URI uri) throws Exception {
		hasFiles(list, uri);
	}

	private boolean isReal(URI uri) {
		return !(uri.getPath()
			.endsWith(".sha1")
			|| uri.getPath()
				.endsWith(".asc")
			|| uri.getPath()
				.endsWith(".md5"));
	}

	public File download(URI uri) throws Exception {
		return request().useCache()
			.age(30, TimeUnit.SECONDS)
			.go(uri);
	}

	public void upload(URI uri, byte[] data) throws Exception {
		try (TaggedData tag = request().put()
			.upload(data)
			.asTag()
			.go(uri)) {
			switch (tag.getState()) {
				case NOT_FOUND :
				case OTHER :
				default :
					tag.throwIt();
					break;

				case UNMODIFIED :
				case UPDATED :
					break;
			}
		}
	}

	public List<Asset> getAssets(String repository) throws Exception {
		String continuationToken = null;
		List<Asset> assets = new ArrayList<>();
		do {
			try (Formatter f = new Formatter()) {
				f.format("beta/assets?repository=%s", URLEncoder.encode(repository, "UTF-8"));
				if (continuationToken != null) {
					f.format("&continuationToken=%s", URLEncoder.encode(continuationToken, "UTF-8"));
				}
				Assets go = client.build()
					.get(Assets.class)
					.go(uri.resolve(f.toString()));
				if (go != null) {
					assets.addAll(go.items);
					continuationToken = go.continuationToken;
				} else {
					System.out.println("could not find " + uri.resolve(f.toString()));
					continuationToken = null;
				}
			}
		} while (continuationToken != null);
		return assets;
	}

	public List<URI> crawl(String string) throws Exception {
		Glob glob;
		if (string == null)
			glob = Glob.ALL;
		else
			glob = new Glob(string);

		Promise<List<URI>> crawl = crawl(uri, glob);
		return crawl.getValue();
	}

	private Promise<List<URI>> crawl(URI base, Glob glob) throws Exception {
		assert glob != null;

		logger.info("crawl = {}", base);
		Deferred<List<URI>> deferred = new Deferred<>();

		String content = client.build()
			.get(String.class)
			.go(base);
		Matcher m = MAVEN_INDEX_P.matcher(content);
		String normalized = base.normalize()
			.toString();

		executor.execute(() -> {
			try {
				List<Promise<List<URI>>> promises = new ArrayList<>();
				List<URI> jars = new ArrayList<>();

				while (m.find()) {
					String link = m.group("uri");
					URI uri;
					try {
						uri = new URI(link).normalize();
						if (!uri.toString()
							.startsWith(normalized))
							continue;
						String path = uri.getPath();
						int n = path.lastIndexOf('.');
						String ext = path.substring(n + 1);

						if (path.endsWith("/")) {
							promises.add(crawl(uri, glob));
						} else if (ext.equals("jar") || ext.equals("zip") || ext.equals("par")) {
							logger.info("jar = {}  | {}", link, ext);
							jars.add(uri);
						} else {
							logger.info("skip = {} | {}", link, ext);
						}
					} catch (java.net.URISyntaxException e) {
						logger.error("Invalid URL {}", link);
					} catch (Exception e) {
						logger.error("failed to crawl ", e);
					}

				}
				Promise<List<URI>> result = Promises.all(promises)
					.map(ll -> {
						List<URI> collect = ll.stream()
							.flatMap(l -> l.stream())
							.collect(Collectors.toList());
						jars.addAll(collect);
						return jars;
					});
				deferred.resolveWith(result);
			} catch (Throwable e) {
				deferred.fail(e);
				Exceptions.duck(e);
			}
		});

		return deferred.getPromise();
	}

	public URI getUri() {
		return uri;
	}
}
