package aQute.bnd.maven;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenDependencyGraph {
	final static DocumentBuilderFactory	docFactory		= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpathFactory	= XPathFactory.newInstance();
	final List<Artifact>				dependencies	= new ArrayList<>();
	final List<URL>						repositories	= new ArrayList<>();
	final Map<URI, Artifact>			cache			= new HashMap<>();
	Artifact							root;

	enum Scope {
		COMPILE,
		RUNTIME,
		TEST,
		PROVIDED,
		SYSTEM,
		IMPORT,
	}

	public class Artifact {

		String			groupId;
		String			artifactId;
		String			version;
		Scope			scope			= Scope.COMPILE;
		boolean			optional;
		String			type;
		URL				url;
		List<Artifact>	dependencies	= new ArrayList<>();

		public Artifact(URL url) throws Exception {
			if (url != null) {
				this.url = url;
				DocumentBuilder db = docFactory.newDocumentBuilder();
				Document doc = db.parse(url.toString());
				XPath xpath = xpathFactory.newXPath();
				Node node = (Node) xpath.evaluate("/project", doc, XPathConstants.NODE);

				groupId = xpath.evaluate("groupId", node);
				artifactId = xpath.evaluate("artifactId", node);
				version = xpath.evaluate("version", node);
				type = xpath.evaluate("type", node);
				optional = (Boolean) xpath.evaluate("optinal", node, XPathConstants.BOOLEAN);
				String scope = xpath.evaluate("scope", node);
				if (scope != null && scope.length() > 0) {
					this.scope = Scope.valueOf(scope.toUpperCase());
				}
				NodeList evaluate = (NodeList) xpath.evaluate("//dependencies/dependency", doc, XPathConstants.NODESET);

				for (int i = 0; i < evaluate.getLength(); i++) {
					Node childNode = evaluate.item(i);
					Artifact artifact = getArtifact(xpath.evaluate("groupId", childNode),
						xpath.evaluate("artifactId", childNode), xpath.evaluate("version", childNode));
					add(artifact);
				}
			}
		}

		public void add(Artifact artifact) {
			dependencies.add(artifact);
		}

		@Override
		public String toString() {
			return groupId + "." + artifactId + "-" + version + "[" + scope + "," + optional + "]";
		}

		public String getPath() throws URISyntaxException {
			return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;
		}

	}

	public void addRepository(URL repository) {
		repositories.add(repository);
	}

	public Artifact getArtifact(String groupId, String artifactId, String version) {
		for (URL repository : repositories) {
			String path = getPath(repository.toString(), groupId, artifactId, version);

			try {
				URI url = new URL(path + ".pom").toURI();
				if (cache.containsKey(url)) {
					return cache.get(url);
				}
				return new Artifact(url.toURL());
			} catch (Exception e) {
				System.err.println("Failed to get " + artifactId + " from " + repository);
			}
		}
		return null;
	}

	private String getPath(String path, String groupId, String artifactId, String version) {
		StringBuilder sb = new StringBuilder();
		sb.append(path);
		if (!path.endsWith("/"))
			sb.append("/");

		sb.append(groupId.replace('.', '/'));
		sb.append('/');
		sb.append(artifactId);
		sb.append('/');
		sb.append(version);
		sb.append('/');
		sb.append(artifactId);
		sb.append('-');
		sb.append(version);
		return null;
	}

	public void addArtifact(Artifact artifact) throws Exception {
		if (root == null)
			root = new Artifact(null);
		root.add(artifact);
	}

}
