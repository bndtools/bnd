package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import aQute.lib.io.*;

public abstract class Pom {
	static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf	= XPathFactory.newInstance();

	static {
		dbf.setNamespaceAware(false);
	}
	
	public enum Scope {
		compile, runtime, system, import_, provided, test, ;
		
//		private boolean includes(Scope other) {
//			if (other == this) return true;
//			switch (this) {
//			case compile:
//				return other == provided || other == test;
//			default:
//				return false;
//			}
//		}
	}

	final Maven			maven;
	final URI			home;

	String				groupId;
	String				artifactId;
	String				version;
	List<Dependency>	dependencies	= new ArrayList<Dependency>();
	File				pomFile;
	String				description="";
	String				name;

	public String getDescription() {
		return description;
	}

	public class Dependency {
		Scope		scope;
		String		type;
		boolean		optional;
		String		groupId;
		String		artifactId;
		String		version;
		Set<String>	exclusions	= new HashSet<String>();

		public Scope getScope() {
			return scope;
		}

		public String getType() {
			return type;
		}

		public boolean isOptional() {
			return optional;
		}

		public String getGroupId() {
			return replace(groupId);
		}

		public String getArtifactId() {
			return replace(artifactId);
		}

		public String getVersion() {
			return replace(version);
		}

		public Set<String> getExclusions() {
			return exclusions;
		}

		public Pom getPom() throws Exception {
			return maven.getPom(groupId, artifactId, version);
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Dependency [");
			if (groupId != null)
				builder.append("groupId=").append(groupId).append(", ");
			if (artifactId != null)
				builder.append("artifactId=").append(artifactId).append(", ");
			if (version != null)
				builder.append("version=").append(version).append(", ");
			if (type != null)
				builder.append("type=").append(type).append(", ");
			if (scope != null)
				builder.append("scope=").append(scope).append(", ");
			builder.append("optional=").append(optional).append(", ");
			if (exclusions != null)
				builder.append("exclusions=").append(exclusions);
			builder.append("]");
			return builder.toString();
		}
	}

	public Pom(Maven maven, File pomFile, URI home) throws Exception {
		this.maven = maven;
		this.home = home;
		this.pomFile = pomFile;
	}

	void parse() throws Exception {
		DocumentBuilder db = dbf.newDocumentBuilder();
		System.err.println("Parsing " + pomFile.getAbsolutePath());
		Document doc = db.parse(pomFile);
		XPath xp = xpf.newXPath();
		parse(doc, xp);
	}

	protected void parse(Document doc, XPath xp) throws XPathExpressionException, Exception {

		this.artifactId = replace(xp.evaluate("project/artifactId", doc).trim(), this.artifactId);
		this.groupId = replace(xp.evaluate("project/groupId", doc).trim(), this.groupId);
		this.version = replace(xp.evaluate("project/version", doc).trim(), this.version);
		
		String nextDescription = xp.evaluate("project/description", doc).trim();
		if ( this.description.length() != 0 && nextDescription.length() != 0)
			this.description += "\n";
		this.description += replace(nextDescription);
		
		this.name = replace(xp.evaluate("project/name", doc).trim(), this.name);

		NodeList list = (NodeList) xp.evaluate("project/dependencies/dependency", doc,
				XPathConstants.NODESET);
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			Dependency dep = new Dependency();
			String scope = xp.evaluate("scope", node).trim();
			if (scope.length() == 0)
				dep.scope = Scope.compile;
			else
				dep.scope = Scope.valueOf(scope);
			dep.type = xp.evaluate("type", node).trim();

			String opt = xp.evaluate("optional", node).trim();
			dep.optional = opt != null && opt.equalsIgnoreCase("true");
			dep.groupId = replace(xp.evaluate("groupId", node));
			dep.artifactId = replace(xp.evaluate("artifactId", node).trim());

			dep.version = replace(xp.evaluate("version", node).trim());
			dependencies.add(dep);

			NodeList exclusions = (NodeList) xp
					.evaluate("exclusions", node, XPathConstants.NODESET);
			for (int e = 0; e < exclusions.getLength(); e++) {
				Node exc = exclusions.item(e);
				String exclGroupId = xp.evaluate("groupId", exc).trim();
				String exclArtifactId = xp.evaluate("artifactId", exc).trim();
				dep.exclusions.add(exclGroupId + "+" + exclArtifactId);
			}
		}

	}

	private String replace(String key, String dflt) {
		if ( key == null || key.length() == 0)
			return dflt;
		
		return replace(key);
	}

	public String getArtifactId() throws Exception {
		return replace(artifactId);
	}

	public String getGroupId() throws Exception {
		return replace(groupId);
	}

	public String getVersion() throws Exception {
		if ( version == null)
			return "<not set>";
		return replace(version);
	}

	public List<Dependency> getDependencies() throws Exception {
		return dependencies;
	}

	static class Rover {

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

	public Set<Pom> getDependencies(Scope scope, URI... urls) throws Exception {
		Set<Pom> result = new LinkedHashSet<Pom>();

		List<Rover> queue = new ArrayList<Rover>();
		for (Dependency d : dependencies) {
			queue.add(new Rover(null, d));
		}

		while (!queue.isEmpty()) {
			Rover rover = queue.remove(0);
			Dependency dep = rover.dependency;
			String groupId = replace(dep.groupId);
			String artifactId = replace(dep.artifactId);
			String version = replace(dep.version);

			String name = groupId + "+" + artifactId;

			if (rover.excludes(name) || dep.optional)
				continue;
			
			if (dep.scope == scope && !dep.optional) {
				try {
					Pom sub = maven.getPom(groupId, artifactId, version, urls);
					if (sub != null) {
						if (!result.contains(sub)) {
							result.add(sub);
							for (Dependency subd : sub.dependencies) {
								queue.add(new Rover(rover, subd));
							}
						}
					} else
						if (rover.previous != null)
							System.err.println("Cannot find " + dep + " from "
									+ rover.previous.dependency);
						else
							System.err.println("Cannot find " + dep + " from top");
				} catch (Exception e) {
					if (rover.previous != null)
						System.err.println("Cannot find " + dep + " from "
								+ rover.previous.dependency);
					else
						System.err.println("Cannot find " + dep + " from top");

//			boolean include = false;
//			if (dep.scope == Scope.compile) {
//				include = true;
//			} else if (dep.scope == Scope.test) {
//				include = rover.previous == null && (action == Action.compile || action == Action.test);
//			} else if (dep.scope == Scope.runtime) {
//				include = action == Action.run;
//			}
//			if (include) {
//				Pom sub = maven.getPom(groupId, artifactId, version, urls);
//				if (!result.contains(sub)) {
//					result.add(sub);
//					for (Dependency subd : sub.dependencies) {
//						queue.add(new Rover(rover, subd));
//					}
					
				}
			}
		}
		return result;
	}

	protected String replace(String in) {
		System.err.println("replace: " + in);
		if (in == null)
			return "null";

		in = in.trim();
		if ("${pom.version}".equals(in) || "${version}".equals(in)
				|| "${project.version}".equals(in))
			return version;

		if ("${basedir}".equals(in))
			return pomFile.getParentFile().getAbsolutePath();

		if ("${pom.name}".equals(in) || "${project.name}".equals(in))
			return name;

		if ("${pom.artifactId}".equals(in) || "${project.artifactId}".equals(in))
			return artifactId;
		if ("${pom.groupId}".equals(in) || "${project.groupId}".equals(in))
			return groupId;

		return in;
	}

	public String toString() {
		return groupId + "+" + artifactId + "-" + version;
	}

	public File getLibrary(Scope action, URI... repositories) throws Exception {
		MavenEntry entry = maven.getEntry(this);
		File file = new File(entry.dir, action + ".lib");

		if (file.isFile() && file.lastModified() >= getPomFile().lastModified())
			return file;

		file.delete();

		Writer writer = IO.writer(file);
		try {
			doEntry(writer, this);
			for (Pom dep : getDependencies(action, repositories)) {
				doEntry(writer, dep);
			}
		} finally {
			writer.close();
		}
		return file;
	}

	/**
	 * @param writer
	 * @param dep
	 * @throws IOException
	 * @throws Exception
	 */
	private void doEntry(Writer writer, Pom dep) throws IOException, Exception {
		writer.append(dep.getGroupId());
		writer.append("+");
		writer.append(dep.getArtifactId());
		writer.append(";version=\"");
		writer.append(dep.getVersion());
		writer.append("\"\n");
	}

	public File getPomFile() {
		return pomFile;
	}

	public String getName() {
		return name;
	}

	public abstract java.io.File getArtifact() throws Exception;
}
