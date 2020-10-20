package aQute.bnd.repository.maven.provider;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.http.HttpClient;
import aQute.libg.glob.PathSet;

class Crawler {
	private final static Pattern	HREF_P	= Pattern.compile("href\\s*=\\s*\"(?<uri>[^\"]+)\"",
		Pattern.CASE_INSENSITIVE);

	final PromiseFactory			pf;
	final HttpClient				client;

	public Crawler(HttpClient client, PromiseFactory pf) {
		this.client = client;
		this.pf = pf;
	}

	public List<URI> getURIs(URI uri, Predicate<URI> predicate) throws Exception {
		assert uri != null;
		assert predicate != null;

		String page = client.build()
			.get(String.class)
			.go(uri);

		List<Promise<List<URI>>> promises = new ArrayList<>();
		List<URI> uris = new ArrayList<>();

		Matcher m = HREF_P.matcher(page);
		while (m.find()) {
			String path = m.group("uri");
			URI next = uri.resolve(path)
				.normalize();

			if (next.getPath()
				.endsWith("/")) {

				URI relativize = uri.relativize(next);
				if (relativize.isAbsolute())
					continue; // not recursive, points outside

				Promise<List<URI>> downstream = pf.submit(() -> getURIs(next, predicate));
				promises.add(downstream);
			} else {
				if (predicate.test(next))
					uris.add(next);
			}
		}
		for (Promise<List<URI>> p : promises) {
			uris.addAll(p.getValue());
		}
		return uris;
	}

	public List<URI> getURIs(URI uri) throws Exception {
		return getURIs(uri, u -> true);
	}

	public static Predicate<URI> predicate(String include, String exclude) {
		PathSet pathSet = new PathSet().include(include)
			.exclude(exclude);
		Predicate<String> find = pathSet.find("**");
		return uri -> find.test(uri.getPath());
	}
}
