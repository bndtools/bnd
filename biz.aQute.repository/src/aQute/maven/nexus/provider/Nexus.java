package aQute.maven.nexus.provider;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.service.url.TaggedData;

public class Nexus {
	private URI			uri;
	private HttpClient	client;

	public Nexus(URI uri, HttpClient client) throws URISyntaxException {
		this.uri = client.makeDir(uri);
		this.client = client;
	}

	public HttpRequest<Object> request() {
		return client.build()
			.headers("Accept", "application/json")
			.headers("User-Agent", "bnd");
	}

	public List<URI> files() throws Exception {
		URI uri = new URI(this.uri + "/content/");
		List<URI> uris = new ArrayList<>();

		files(uris, uri);
		return uris;
	}

	/*
	 * <pre> <content> <data> <content-item> <resourceURI>
	 * https://oss.sonatype.org/service/local/repositories/orgosgi-1073/content/
	 * org/osgi/osgi.enroute.authenticator.github.provider/ </resourceURI>
	 * <relativePath>/org/osgi/osgi.enroute.authenticator.github.provider/</
	 * relativePath> <text>osgi.enroute.authenticator.github.provider</text>
	 * <leaf>false</leaf> <lastModified>2016-06-03 17:05:14.0 UTC</lastModified>
	 * <sizeOnDisk>-1</sizeOnDisk> </content-item> </data> </content> </pre>
	 */
	public void files(List<URI> list, URI uri) throws Exception {
		ContentDTO content = request().get(ContentDTO.class)
			.go(uri);
		for (ContentDTO.ItemDTO item : content.data) {
			if (item.sizeOnDisk < 0) {
				files(list, item.resourceURI);
			} else {
				if (isReal(item.resourceURI))
					list.add(item.resourceURI);
			}
		}
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
}
