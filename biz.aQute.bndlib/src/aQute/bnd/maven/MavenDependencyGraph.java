package aQute.bnd.maven;

import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

public class MavenDependencyGraph {
	final static DocumentBuilderFactory	docFactory		= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpathFactory	= XPathFactory.newInstance();
	final List<Artifact>				dependencies	= new ArrayList<Artifact>();
	final List<URL>						repositories	= new ArrayList<URL>();
	final XPath							xpath			= xpathFactory.newXPath();
	final Map<URL,Artifact>				cache			= new HashMap<URL,Artifact>();
	Artifact							root;

	enum Scope {
		COMPILE, RUNTIME, TEST, PROVIDED, SYSTEM, IMPORT,
	}

	public class Artifact {

		String			groupId;
		String			artifactId;
		String			version;
		Scope			scope			= Scope.COMPILE;
		boolean			optional;
		String			type;
		URL				url;
		List<Artifact>	dependencies	= new ArrayList<Artifact>();

		public Artifact(URL url) throws Exception {
			if (url != null) {
				this.url = url;
				DocumentBuilder db = docFactory.newDocumentBuilder();
				Document doc = db.parse(url.toString());
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

	/**
	 * @param xp
	 * @param node
	 * @param d
	 * @throws XPathExpressionException
	 */

	public Artifact getArtifact(String groupId, String artifactId, String version) {
		for (URL repository : repositories) {
			String path = getPath(repository.toString(), groupId, artifactId, version);

			try {
				URL url = new URL(path + ".pom");
				if (cache.containsKey(url)) {
					return cache.get(url);
				} else {
					return new Artifact(url);
				}
			}
			catch (Exception e) {
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
