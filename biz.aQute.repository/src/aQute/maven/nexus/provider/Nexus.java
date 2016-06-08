package aQute.maven.nexus.provider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;

public class Nexus {
	private URI			uri;
	private HttpClient	client;

	public Nexus(URI uri, HttpClient client) throws URISyntaxException {
		this.uri = client.makeDir(uri);
		this.client = client;
	}

	public HttpRequest<Object> request() {
		return client.build().headers("Accept", "application/json").headers("User-Agent", "bnd");
	}

	public List<URI> files() throws Exception {
		URI uri = new URI(this.uri + "/content/");
		List<URI> uris = new ArrayList<URI>();

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
		ContentDTO content = request().get(ContentDTO.class).go(uri);
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
		return !(uri.getPath().endsWith(".sha1") || uri.getPath().endsWith(".md5"));
	}

	public void sign() {
	}

	private void sign(URI uri2) {

	}

}
