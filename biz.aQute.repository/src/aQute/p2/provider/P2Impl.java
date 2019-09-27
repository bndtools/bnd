package aQute.p2.provider;

import static aQute.lib.promise.PromiseCollectors.toPromise;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.xpath.XPathExpressionException;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;

import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.p2.api.Artifact;
import aQute.p2.api.ArtifactProvider;
import aQute.p2.api.P2Index;

public class P2Impl implements ArtifactProvider {
	private static final Logger		logger		= LoggerFactory.getLogger(P2Impl.class);
	private final HttpClient		client;
	private final URI				base;
	private final Set<URI>			defaults	= Collections.newSetFromMap(new ConcurrentHashMap<URI, Boolean>());
	private final PromiseFactory	promiseFactory;

	public P2Impl(HttpClient c, URI base, PromiseFactory promiseFactory) throws Exception {
		this.client = c;
		this.promiseFactory = promiseFactory;
		this.base = normalize(base);
	}

	private URI normalize(URI base) throws Exception {
		String path = base.getPath();
		if (path == null || path.endsWith("/"))
			return base;

		return new URI(base.toString() + "/");
	}

	@Override
	public List<Artifact> getAllArtifacts() throws Exception {
		Set<URI> cycles = Collections.newSetFromMap(new ConcurrentHashMap<URI, Boolean>());
		List<Artifact> value = getArtifacts(cycles, base).getValue();
		return value;
	}

	// For backward compatibility reasons
	// this method is now called getBundles(), it ignores non osgi.bundle
	// artifacts

	public List<Artifact> getArtifacts() throws Exception {
		return getBundles();
	}

	private Promise<List<Artifact>> getArtifacts(Set<URI> cycles, URI uri) {
		if (!cycles.add(uri)) {
			return promiseFactory.resolved(Collections.emptyList());
		}

		try {
			String type = uri.getPath();
			logger.info("getArtifacts type={}", uri);
			if (type.endsWith("/compositeArtifacts.xml")) {
				return parseCompositeArtifacts(cycles, hideAndSeek(uri), uri);
			} else if (type.endsWith("/artifacts.xml.xz")) {
				return parseArtifacts(hideAndSeek(uri), uri);
			} else if (type.endsWith("/artifacts.xml")) {
				return parseArtifacts(hideAndSeek(uri), uri);
			} else if (type.endsWith("/p2.index")) {
				return parseIndexArtifacts(cycles, uri);
			}
			uri = normalize(uri).resolve("p2.index");
			defaults.add(uri);
			return parseIndexArtifacts(cycles, uri);
		} catch (Exception e) {
			logger.error("getArtifacts", e);
			return promiseFactory.failed(e);
		}
	}

	private Promise<List<Artifact>> parseArtifacts(InputStream in, URI uri) throws Exception {
		if (in == null) {
			logger.info("No content for {}", uri);
			return promiseFactory.resolved(Collections.emptyList());
		}

		return promiseFactory.submit(() -> {
			try {
				ArtifactRepository ar = new ArtifactRepository(in, uri);
				return ar.getArtifacts();
			} finally {
				IO.close(in);
			}
		});
	}

	/**
	 * @param artifacts
	 * @param cycles
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	private Promise<List<Artifact>> parseCompositeArtifacts(Set<URI> cycles, InputStream in, URI base)
		throws Exception {
		if (in == null) {
			logger.info("No such composite {}", base);
			return promiseFactory.resolved(Collections.emptyList());
		}

		CompositeArtifacts ca = new CompositeArtifacts(in, base);
		ca.parse();

		return getArtifacts(cycles, ca.uris);
	}

	private Promise<List<Artifact>> getArtifacts(Set<URI> cycles, final Collection<URI> uris) {
		Deferred<List<Artifact>> deferred = promiseFactory.deferred();
		promiseFactory.executor()
			.execute(() -> {
				try {
					deferred.resolveWith(uris.stream()
						.map(uri -> getArtifacts(cycles, base.resolve(uri)).recover(failed -> {
							if (!defaults.contains(uri)) {
								logger.info("Failed to get artifacts for %s", uri, failed.getFailure());
							}
							return Collections.emptyList();
						}))
						.collect(toPromise(promiseFactory))
						.map(ll -> ll.stream()
							.flatMap(List::stream)
							.collect(toList())));
				} catch (Throwable e) {
					deferred.fail(e);
				}
			});
		return deferred.getPromise();
	}

	private InputStream hideAndSeek(URI uri) throws Exception {
		if (uri.getPath()
			.endsWith(".xz")) {
			File f = getFile(uri);
			if (f != null)
				return tzStream(f);
			else
				return null;
		}

		URI xzname = replace(uri, "$", ".xz");
		File f = getFile(xzname);
		if (f != null)
			return tzStream(f);

		f = getFile(replace(uri, ".xml$", ".jar"));
		if (f != null)
			return jarStream(f, Strings.getLastSegment(uri.getPath(), '/'));

		f = getFile(uri);
		if (f != null)
			return IO.stream(f);

		if (!defaults.contains(uri))
			logger.error("Invalid uri {}", uri);
		return null;
	}

	private File getFile(URI xzname) throws Exception {
		return client.build()
			.useCache()
			.go(xzname);
	}

	private InputStream jarStream(File f, String name) throws IOException {
		final JarFile jaf = new JarFile(f);
		ZipEntry entry = jaf.getEntry(name);
		final InputStream inputStream = jaf.getInputStream(entry);

		return new FilterInputStream(inputStream) {
			@Override
			public void close() throws IOException {
				jaf.close();
			}
		};
	}

	private InputStream tzStream(File f) throws Exception {
		return new XZInputStream(IO.stream(f));
	}

	private URI replace(URI uri, String where, String replacement) {
		String path = uri.getRawPath();
		return uri.resolve(path.replaceAll(where, replacement));
	}

	/**
	 * @formatter:off
	 *  version = 1
 	 *  metadata.repository.factory.order = compositeContent.xml,\!
 	 *  artifact.repository.factory.order = compositeArtifacts.xml,\!
	 * @formatter:on
	 * @param artifacts
	 * @param cycles
	 * @param base
	 * @throws Exception
	 */
	private Promise<List<Artifact>> parseIndexArtifacts(Set<URI> cycles, final URI uri) throws Exception {
		Promise<File> file = client.build()
			.useCache()
			.get()
			.async(uri.toURL());
		return file.flatMap(f -> parseIndexArtifacts(cycles, uri, f));
	}

	private Promise<List<Artifact>> parseIndexArtifacts(Set<URI> cycles, URI uri, File file) throws Exception {
		P2Index index;

		if (file == null) {
			index = getDefaultIndex(uri);
		} else {
			index = parseIndex(file, uri);
		}

		canonicalize(index.artifacts);
		canonicalize(index.content);

		return getArtifacts(cycles, index.artifacts);
	}

	private void canonicalize(List<URI> artifacts) throws URISyntaxException {
		if (artifacts.size() < 2)
			return;

		for (URI uri : new ArrayList<>(artifacts)) {
			if (uri.getPath()
				.endsWith(".xml"))
				artifacts.remove(new URI(uri.toString() + ".xz"));
		}
	}

	private P2Index getDefaultIndex(URI base) {
		P2Index index = new P2Index();
		index.artifacts.add(base.resolve("compositeArtifacts.xml"));
		index.artifacts.add(base.resolve("artifacts.xml"));
		index.content.add(base.resolve("compositeContent.xml"));
		index.content.add(base.resolve("content.xml"));
		defaults.addAll(index.artifacts);
		defaults.addAll(index.content);
		return index;
	}

	private P2Index parseIndex(File file, URI base) throws IOException {
		Properties p = new Properties();
		try (InputStream in = IO.stream(file)) {
			p.load(in);
		}

		String version = p.getProperty("version");
		if (version == null || Integer.parseInt(version) != 1)
			throw new UnsupportedOperationException(
				"The repository " + base + " specifies an index file with an incompatible version " + version);

		P2Index index = new P2Index();

		addPaths(p.getProperty("metadata.repository.factory.order"), index.content, base);
		addPaths(p.getProperty("artifact.repository.factory.order"), index.artifacts, base);

		index.modified = file.lastModified();
		return index;
	}

	private void addPaths(String p, List<URI> index, URI base) {
		Parameters content = new Parameters(p);
		for (String path : content.keySet()) {
			if ("!".equals(path)) {
				break;
			}
			URI sub = base.resolve(path);
			index.add(sub);
		}
	}
}
