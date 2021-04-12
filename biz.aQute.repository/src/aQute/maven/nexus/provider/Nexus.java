package aQute.maven.nexus.provider;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.osgi.dto.DTO;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.result.Result;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;
import aQute.lib.xml.XML;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;
import aQute.maven.api.Archive;

public class Nexus {
	private final static String			MAVEN_INDEX_S	= "href\\s*=\\s*([\"'])(?<uri>[^\\./][^\"'\r\n]+)\\1";
	private final static Pattern		MAVEN_INDEX_P	= Pattern.compile(MAVEN_INDEX_S);
	final static DocumentBuilderFactory	dbf				= XML.newDocumentBuilderFactory();
	final static XPathFactory			xpf				= XPathFactory.newInstance();

	final static Logger					logger			= LoggerFactory.getLogger(Nexus.class);
	private URI							uri;
	private HttpClient					client;
	private Executor					executor;

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

	public Nexus(URI uri, HttpClient client) throws URISyntaxException {
		this(uri, client, Processor.getExecutor());
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
			.timeout(5 * 60 * 1000)
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
							.flatMap(List::stream)
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

	public static class PromoteResponseData extends DTO {
		public String	stagedRepositoryId;
		public String	description;
	}

	public static class PromoteResponse extends DTO {
		PromoteResponseData data;
	}

	/**
	 * Create staging repo
	 * <p>
	 * https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
	 *
	 * @param profileId the profile id
	 * @param description a description
	 * @return a Result with the repository id
	 * @throws Exception
	 */

	public Result<String, String> createStagingRepository(String profileId, String description) throws Exception {
		Tag promoteRequest = new Tag("promoteRequest");
		Tag data = new Tag(promoteRequest, "data");
		if (description != null) {
			new Tag(data, "description", description);
		}
		String payload = promoteRequest.toString();

		TaggedData r = client.build()
			.upload(payload)
			.headers("Content-Type", "application/xml")
			.asTag()
			.post()
			.go(getUri().resolve("/service/local/staging/profiles/" + profileId + "/start"));

		if (r.isOk()) {
			String s = null;
			try {
				s = IO.collect(r.getInputStream());
				Document doc = dbf.newDocumentBuilder()
					.parse(new InputSource(new StringReader(s)));
				XPath xp = xpf.newXPath();
				String stagedRepositoryId = xp.evaluate("//stagedRepositoryId", doc);

				return Result.ok(stagedRepositoryId);
			} catch (Exception e) {
				logger.info("response in={} out={} {}", payload, s, e);
			}
		}
		return Result.err("failed the request %s", r);
	}

	public TaggedData uploadStaging(String repositoryId, File file, String remotePath) throws Exception {
		URI upload = getUri().resolve("service/local/staging/deployByRepositoryId/" + repositoryId + "/" + remotePath);
		TaggedData go = client.build()
			.upload(file)
			.headers("User-Agent", "Bnd")
			.headers("Content-Type", "application/xml")
			.asTag()
			.put()
			.go(upload);

		if (!go.isOk())
			return go;

		SHA1 sha1 = SHA1.digest(file);
		MD5 md5 = MD5.digest(file);

		try (TaggedData tag = client.build()
			.put()
			.upload(sha1.asHex())
			.asTag()
			.go(new URL(upload + ".sha1"))) {}
		try (TaggedData tag = client.build()
			.put()
			.upload(md5.asHex())
			.asTag()
			.go(new URL(upload + ".md5"))) {}

		return go;
	}

	public TaggedData fetchStaging(String repositoryId, String remotePath, boolean force) throws Exception {
		URI uri = getUri().resolve("service/local/staging/deployByRepositoryId/" + repositoryId + "/" + remotePath);
		return client.build()
			.headers("User-Agent", "Bnd")
			.asTag()
			.get()
			.go(uri);
	}

	public TaggedData deleteStaging(String repositoryId, String remotePath) throws Exception {
		URI uri = getUri().resolve("service/local/staging/deployByRepositoryId/" + repositoryId + "/" + remotePath);
		return client.build()
			.headers("User-Agent", "Bnd")
			.asTag()
			.delete()
			.go(uri);
	}

	public URI getUri() {
		return uri;
	}

	/**
	 * Path is either a file path (slashed) or a GAV
	 *
	 * @param pathOrGAV
	 * @return Remote path
	 */
	public String remotePath(String pathOrGAV) {
		if (Archive.isValid(pathOrGAV)) {
			return Archive.valueOf(pathOrGAV).remotePath;
		}
		return pathOrGAV;
	}

}
