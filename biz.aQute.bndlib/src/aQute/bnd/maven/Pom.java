package aQute.bnd.maven;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import aQute.lib.io.*;

public class Pom {
	static DocumentBuilderFactory	dbf				= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf				= XPathFactory.newInstance();

	static {
		dbf.setNamespaceAware(false);
	}

	String							groupId;
	String							artifactId;
	String							version;
	List<Dependency>				dependencies	= new ArrayList<Dependency>();
	List<URL>						repositories	= new ArrayList<URL>();
	Maven							maven;
	Exception						exception;
	Properties						properties		= new Properties();
	URL								repository;
	File							artifact;
	
	public enum Scope {
		compile, runtime, provided, system, import_, test
	};

	public class Dependency {
		Scope		scope;
		String		type;
		boolean		optional;
		String		groupId;
		String		artifactId;
		String		version;
		Set<String>	exclusions	= new HashSet<String>();
	}

	public Pom(Maven maven) {
		this.maven = maven;
	}

	public void setFile(File pomFile) throws Exception {
		DocumentBuilder db = dbf.newDocumentBuilder();
		System.out.println("PArsing " + pomFile.getAbsolutePath());
		Document doc = db.parse(pomFile);
		XPath xp = xpf.newXPath();

		Node parent = (Node) xp.evaluate("project/parent", doc, XPathConstants.NODE);
		if (parent != null && parent.hasChildNodes()) {
			String parentGroupId = xp.evaluate("groupId", parent);
			String parentArtifactId = xp.evaluate("artifactId", parent);
			String parentVersion = xp.evaluate("version", parent);
			String parentPath = xp.evaluate("relativePath", parent);
			Pom parentPom;
			if (parentPath != null && !parentPath.isEmpty()) {
				File parentFile = IO.getFile(pomFile.getParentFile(), parentPath);
				if (!parentFile.isFile())
					throw new IllegalArgumentException("Pom " + pomFile
							+ " specifies parent that does not exist: " + parentFile);
				parentPom = new Pom(maven);
				parentPom.setFile(parentFile);
			} else {
				parentPom = maven.getPom(parentGroupId, parentArtifactId, parentVersion);
			}

			// inherit
			dependencies.addAll(parentPom.dependencies);
			properties.putAll(parentPom.properties);
			repositories.addAll(parentPom.repositories);
		}

		NodeList list = (NodeList) xp.evaluate("project/dependencies/dependency", doc,
				XPathConstants.NODESET);
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			Dependency dep = new Dependency();
			String scope = xp.evaluate("scope", node).trim();
			if (scope.isEmpty())
				dep.scope = Scope.compile;
			else
				dep.scope = Scope.valueOf(scope);
			dep.type = xp.evaluate("type", node);

			String opt = xp.evaluate("optional", node);
			dep.optional = opt != null && opt.equalsIgnoreCase("true");
			dep.groupId = xp.evaluate("groupId", node);
			dep.artifactId = xp.evaluate("artifactId", node);
			dep.version = xp.evaluate("version", node);
			dependencies.add(dep);

			NodeList exclusions = (NodeList) xp
					.evaluate("exclusions", node, XPathConstants.NODESET);
			for (int e = 0; e < exclusions.getLength(); e++) {
				Node exc = exclusions.item(e);
				String exclGroupId = xp.evaluate("groupId", exc);
				String exclArtifactId = xp.evaluate("artifactId", exc);
				dep.exclusions.add(exclGroupId + "+" + exclArtifactId);
			}
		}

		NodeList propNodes = (NodeList) xp.evaluate("project/properties/*", doc,
				XPathConstants.NODESET);
		for (int i = 0; i < propNodes.getLength(); i++) {
			Node node = propNodes.item(i);
			properties.setProperty(node.getNodeName(), node.getNodeValue());
		}

		NodeList repos = (NodeList) xp.evaluate("project/repositories/repository", doc,
				XPathConstants.NODESET);
		for (int i = 0; i < repos.getLength(); i++) {
			Node node = repos.item(i);
			String urlString = node.getNodeValue().trim();
			repositories.add(new URL(urlString));
		}

		synchronized (this) {
			groupId = xp.evaluate("project/groupId", doc);
			artifactId = xp.evaluate("project/artifactId", doc);
			version = xp.evaluate("project/version", doc);
			notifyAll();
		}
	}

	public String getArtifactId() throws Exception {
		block();
		return artifactId;
	}

	public String getGroupId() throws Exception {
		block();
		return groupId;
	}

	public String getVersion() throws Exception {
		block();
		return version;
	}

	public List<Dependency> getDependencies() throws Exception {
		block();
		return dependencies;
	}

	private synchronized void block() throws Exception {
		while (groupId == null && exception == null)
			wait();

		if (exception != null)
			throw exception;
	}

	class Rover {

		public Rover(Rover rover, Dependency d) {
			this.previous = rover;
			this.dependency = d;
		}

		final Rover			previous;
		final Dependency	dependency;

		public boolean excludes(String name) {
			return dependency.exclusions.contains(name) && previous != null
					&& previous.excludes(name);
		}
	}

	public List<Pom> getDependencies(Scope scope) throws Exception {
		List<Pom> result = new ArrayList<Pom>();
		result.add(this);

		List<Rover> queue = new ArrayList<Rover>();
		for (Dependency d : dependencies) {
			queue.add(new Rover(null, d));
		}

		while (!queue.isEmpty()) {
			Rover rover = queue.remove(0);
			Dependency dep = rover.dependency;

			String name = dep.groupId + "+" + dep.artifactId;
			if (rover.excludes(name))
				continue;

			if (dep.scope == scope && !dep.optional) {
				Pom sub = maven.getPom(dep.groupId, dep.artifactId, dep.version, repository);

				if (!result.contains(sub)) {
					result.add(sub);
					for (Dependency subd : sub.dependencies) {
						queue.add(new Rover(rover, subd));
					}
				}
			}
		}
		return result;
	}

	void setRepository(URL repo) {
		this.repository = repo;
	}

	public String toString() {
		return groupId + "+" + artifactId + "-" + version;
	}


	public synchronized File getArtifact() throws Exception {
		return maven.getArtifact(groupId, artifactId, version, repository);
	}
}
