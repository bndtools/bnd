package aQute.p2.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.osgi.util.function.Function;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;

import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.p2.api.Artifact;
import aQute.p2.api.Metadata;
import aQute.p2.api.P2;
import aQute.p2.api.P2Index;

public class P2Impl implements P2 {
	final DocumentBuilder							db			= DocumentBuilderFactory.newInstance()
			.newDocumentBuilder();
	final XPath										xpath		= XPathFactory.newInstance().newXPath();
	Logger											logger		= LoggerFactory.getLogger(P2Impl.class);
	final HttpClient								client;
	final URI										base;
	P2Index											index;
	static final List<Artifact>						emptyList	= Collections.emptyList();
	private static final Promise<List<Artifact>>	RESOLVED	= Promises.resolved(emptyList);
	final Set<URI>									defaults	= new HashSet<URI>();
	private Executor								executor;

	public P2Impl(HttpClient c, URI base, Executor executor) throws Exception {
		this.client = c;
		this.executor = executor;
		this.base = normalize(base);
	}

	private URI normalize(URI base) throws Exception {
		String path = base.getPath();
		if (path.endsWith("/"))
			return base;

		return new URI(base.toString() + "/");
	}

	public List<Artifact> getArtifacts() throws Exception {
		Set<URI> cycles = new LinkedHashSet<>();
		return getArtifacts(cycles, base).getValue();
	}

	private Promise<List<Artifact>> getArtifacts(Set<URI> cycles, URI uri) throws Exception {
		if (cycles.contains(uri))
			throw new IllegalStateException("There is a cycle in the p2 setup : " + cycles + " -> " + uri);

		cycles.add(uri);

		System.out.println(uri);
		String type = uri.getPath();

		if (type.endsWith("/compositeArtifacts.xml")) {
			return parseCompositeArtifacts(cycles, hideAndSeek(uri), uri);
		} else if (type.endsWith("/artifacts.xml.xz")) {
			return parseArtifacts(hideAndSeek(uri), uri);
		} else if (type.endsWith("/artifacts.xml")) {
			return parseArtifacts(hideAndSeek(uri), uri);
		} else if (type.endsWith("/p2.index"))
			return parseIndexArtifacts(cycles, uri);
		else {
			uri = normalize(uri).resolve("p2.index");
			defaults.add(uri);
			return parseIndexArtifacts(cycles, uri);
		}
	}

	private Promise<List<Artifact>> parseArtifacts(final InputStream in, final URI uri) throws Exception {
		if (in == null)
			return RESOLVED;

		final Deferred<List<Artifact>> deferred = new Deferred<>();

		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ArtifactRepository ar = new ArtifactRepository(in, uri);
					deferred.resolve(ar.getArtifacts());
				} catch (Throwable e) {
					e.printStackTrace();
					deferred.fail(e);
				} finally {
					IO.close(in);
				}
			}
		});

		return deferred.getPromise();
	}

	/**
	 * @param artifacts
	 * @param cycles
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	private Promise<List<Artifact>> parseCompositeArtifacts(final Set<URI> cycles, final InputStream in, final URI base)
			throws Exception {
		if (in == null)
			return RESOLVED;

		CompositeArtifacts ca = new CompositeArtifacts(in);
		ca.parse();
		System.out.println("  composite: " + ca.uris);

		return getArtifacts(cycles, ca.uris);
	}

	private Promise<List<Artifact>> getArtifacts(final Set<URI> cycles, final Collection<URI> uris) {
		final Deferred<List<Artifact>> deferred = new Deferred<>();

		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Map<URI,Promise<List<Artifact>>> promises = new HashMap<>();

					for (URI uri : uris) {
						URI nuri = base.resolve(uri);
						promises.put(uri, getArtifacts(cycles, nuri));
					}

					List<Artifact> artifacts = new ArrayList<>();
					for (Map.Entry<URI,Promise<List<Artifact>>> e : promises.entrySet()) {
						try {
							List<Artifact> value = e.getValue().getValue();
							artifacts.addAll(value);
						} catch (InvocationTargetException ee) {
							if (!defaults.contains(e.getKey())) {
								ee.getTargetException().printStackTrace();
								logger.info("Failed " + ee.getTargetException().getMessage());
							}
						} catch (Exception eee) {
							logger.info("Failed " + eee.getMessage());
						}
					}
					deferred.resolve(artifacts);
				} catch (Throwable e) {
					e.printStackTrace();
					deferred.fail(e);
				}
			}
		});
		return deferred.getPromise();
	}

	private InputStream hideAndSeek(URI uri) throws Exception {
		if (uri.getPath().endsWith(".xz")) {
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
			return new FileInputStream(f);

		if (!defaults.contains(uri))
			logger.error("Invalid uri {}", uri);
		return null;
	}

	File getFile(URI xzname) throws Exception {
		return client.build().useCache().go(xzname);
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
		return new XZInputStream(new FileInputStream(f));
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
	private Promise<List<Artifact>> parseIndexArtifacts(final Set<URI> cycles, final URI uri) throws Exception {
		Promise<File> file = client.build().useCache().get().async(uri.toURL());
		return file.flatMap(new Function<File,Promise< ? extends List<Artifact>>>() {

			@Override
			public Promise<List<Artifact>> apply(File t) {
				try {
					return parseIndexArtifacts(cycles, uri, t);
				} catch (Throwable e) {
					return Promises.failed(e);
				}
			}
		});
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

		System.out.println("  index: " + index.artifacts);
		return getArtifacts(cycles, index.artifacts);
	}

	private void canonicalize(List<URI> artifacts) throws URISyntaxException {
		if (artifacts.size() < 2)
			return;

		for (URI uri : new ArrayList<>(artifacts)) {
			if (uri.getPath().endsWith(".xml"))
				artifacts.remove(new URI(uri.toString() + ".xz"));
		}
	}

	P2Index getDefaultIndex(URI base) {
		P2Index index = new P2Index();
		index.artifacts.add(base.resolve("compositeArtifacts.xml"));
		index.artifacts.add(base.resolve("artifacts.xml"));
		index.content.add(base.resolve("compositeContent.xml"));
		index.content.add(base.resolve("content.xml"));
		defaults.addAll(index.artifacts);
		defaults.addAll(index.content);
		this.index = index;
		return index;
	}

	private P2Index parseIndex(File file, URI base) throws IOException {
		Properties p = new Properties();
		try (InputStream in = new FileInputStream(file)) {
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

	void addPaths(String p, List<URI> index, URI base) {
		Parameters content = new Parameters(p);
		for (String path : content.keySet()) {
			if ("!".equals(path)) {
				break;
			}
			URI sub = base.resolve(path);
			index.add(sub);
		}
	}

	@Override
	public Metadata getRepository() {
		return null;
	}

	public static void main(String args[]) throws Exception {
		HttpClient c = new HttpClient();
		ExecutorService pool = Executors.newCachedThreadPool();
		try {
			P2Impl p2 = new P2Impl(c, new URI("http://www.nodeclipse.org/updates/"), pool);
			System.out.println(p2.getArtifacts());
			System.out.println(p2.defaults);
		} finally {
			pool.shutdown();
		}
	}

}
