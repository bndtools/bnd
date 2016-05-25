package aQute.p2.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;

import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.lib.strings.Strings;
import aQute.p2.api.Artifact;
import aQute.p2.api.Metadata;
import aQute.p2.api.P2;
import aQute.p2.api.P2Index;

public class P2AsyncImpl2 implements P2 {
	final DocumentBuilder	db		= DocumentBuilderFactory.newInstance().newDocumentBuilder();
	final XPath				xpath	= XPathFactory.newInstance().newXPath();
	Logger					logger	= LoggerFactory.getLogger(P2AsyncImpl2.class);
	final HttpClient		client;
	final URI				base;
	P2Index					index;

	public P2AsyncImpl2(HttpClient c, URI base) throws Exception {
		this.client = c;
		this.base = normalize(base);
	}

	private URI normalize(URI base) throws Exception {
		String path = base.getPath();
		if (path.endsWith("/"))
			return base;

		return new URI(base.toString() + "/");
	}

	List<Artifact> getArtifacts() throws Exception {
		Set<URI> cycles = new LinkedHashSet<>();
		List<Artifact> artifacts = new ArrayList<>();
		getArtifacts(artifacts, cycles, base);
		return artifacts;
	}

	private void getArtifacts(List<Artifact> artifacts, Set<URI> cycles, URI uri) throws Exception {
		if (cycles.contains(uri))
			throw new IllegalStateException("There is a cycle in the p2 setup : " + cycles + " -> " + uri);

		cycles.add(uri);

		System.out.println(uri);
		String type = uri.getPath();

		if (type.endsWith("/compositeArtifacts.xml")) {
			parseCompositeArtifacts(artifacts, cycles, hideAndSeek(uri), uri);
		} else if (type.endsWith("/artifacts.xml.xz")) {
			parseArtifacts(artifacts, hideAndSeek(uri), uri);
		} else if (type.endsWith("/artifacts.xml")) {
			parseArtifacts(artifacts, hideAndSeek(uri), uri);
		} else if (type.endsWith("/p2.index"))
			parseIndexArtifacts(artifacts, cycles, uri);
		else {
			uri = normalize(uri).resolve("p2.index");
			parseIndexArtifacts(artifacts, cycles, uri);
		}
	}

	private boolean parseArtifacts(List<Artifact> artifacts, InputStream in, URI uri) throws Exception {
		if (in == null)
			return false;

		if (artifacts == null) {
			System.out.println("Duh?");
			return false;
		}
		try {

			ArtifactRepository ar = new ArtifactRepository(in, uri);

			artifacts.addAll(ar.getArtifacts());

			return true;
		} finally {
			in.close();
		}
	}

	/**
	 * @param artifacts
	 * @param cycles
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	private boolean parseCompositeArtifacts(List<Artifact> artifacts, Set<URI> cycles, InputStream in, URI base)
			throws Exception {
		if (in == null)
			return false;

		try {
			CompositeArtifacts ca = new CompositeArtifacts(in);
			ca.parse();
			System.out.println("  composite: " + ca.uris);
			for (URI uri : ca.uris) {
				URI nuri = base.resolve(uri);
				getArtifacts(artifacts, cycles, nuri);
			}
			return true;
		} finally {
			in.close();
		}
	}

	private InputStream hideAndSeek(URI uri) throws Exception {
		if (uri.getPath().endsWith(".xz")) {
			File f = client.build().useCache().go(uri);
			if (f != null)
				return tzStream(f);
			else
				return null;
		}

		File f = client.build().useCache().go(replace(uri, "$", ".xz"));
		if (f != null)
			return tzStream(f);

		f = client.build().useCache().go(replace(uri, ".xml$", ".jar"));
		if (f != null)
			return jarStream(f, Strings.getLastSegment(uri.getPath(), '/'));

		f = client.build().useCache().go(uri);
		if (f != null)
			return new FileInputStream(f);

		logger.error("Invalid uri {}", uri);
		return null;
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
	private void parseIndexArtifacts(List<Artifact> artifacts, Set<URI> cycles, URI uri) throws Exception {
		File file = client.build().useCache().get().go(uri);
		P2Index index;

		if (file == null) {
			index = getDefaultIndex(uri);
		} else {
			index = parseIndex(file, uri);
		}

		canonicalize(index.artifacts);
		canonicalize(index.content);

		System.out.println("  index: " + index.artifacts);
		for (URI artifactURI : index.artifacts) {
			getArtifacts(artifacts, cycles, artifactURI);
		}
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
		P2AsyncImpl2 p2 = new P2AsyncImpl2(c, new URI("http://www.nodeclipse.org/updates/"));
		System.out.println(p2.getArtifacts());
	}

}
