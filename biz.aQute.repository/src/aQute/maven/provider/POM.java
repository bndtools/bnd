package aQute.maven.provider;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.xml.DocumentBuilderFactory;
import aQute.maven.api.Archive;
import aQute.maven.api.IPom;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;

/**
 * Parser and placeholder for POM information.
 */
public class POM implements IPom {
	static Logger l = LoggerFactory.getLogger(POM.class);

	static XPathFactory				xpf				= XPathFactory.newInstance();
	private Revision				revision;
	private String					packaging;
	private final Properties		properties;
	private final POM				parent;
	private Map<Program,Dependency>	dependencies	= new LinkedHashMap<>();
	private Map<Program,Dependency>	dependencyManagement	= new LinkedHashMap<>();
	private XPath					xp;

	private MavenRepository repo;

	public static POM parse(MavenRepository repo, File file) throws Exception {
		try {
			return new POM(repo, file);
		} catch (Exception e) {
			l.error("Failed to parse POM file {}", file);
			throw e;
		}
	}

	public POM() {
		this.properties = System.getProperties();
		this.parent = null;
	}

	public POM(MavenRepository repo, InputStream in) throws Exception {
		this(repo, DocumentBuilderFactory.safeInstance().parse(in));
	}

	public POM(MavenRepository repo, File file) throws Exception {
		this(repo, DocumentBuilderFactory.safeInstance().parse(file));
	}

	public POM(MavenRepository repo, Document doc) throws Exception {
		this.repo = repo;
		xp = xpf.newXPath();

		String parentGroup = Strings.trim(xp.evaluate("project/parent/groupId", doc));
		String parentArtifact = Strings.trim(xp.evaluate("project/parent/artifactId", doc));
		String parentVersion = Strings.trim(xp.evaluate("project/parent/version", doc));
		String relativePath = Strings.trim(xp.evaluate("project/parent/relativePath", doc));
		if (!parentGroup.isEmpty() && !parentArtifact.isEmpty() && !parentVersion.isEmpty()) {

			Program program = Program.valueOf(parentGroup, parentArtifact);
			if (program == null)
				throw new IllegalArgumentException("Invalid parent program " + parentGroup + ":" + parentArtifact);

			MavenVersion v = MavenVersion.parseMavenString(parentVersion);
			if (v == null)
				throw new IllegalArgumentException("Invalid version for parent pom " + program + ":" + v);

			File fp;
			if (relativePath != null && !relativePath.isEmpty() && (fp = IO.getFile(relativePath)).isFile()) {
				this.parent = new POM(repo, fp);
			} else {
				Revision revision = program.version(v);
				POM pom = repo.getPom(revision);
				if (pom == null)
					throw new IllegalArgumentException("Parent not found" + revision.pomArchive());
				this.parent = pom;
			}
		} else {
			this.parent = new POM();
		}

		this.properties = new Properties(this.parent.properties);

		Element project = doc.getDocumentElement();
		index(project, "project.", "modelVersion", "groupId", "artifactId", "version", "packaging");

		NodeList props = (NodeList) xp.evaluate("properties", project, XPathConstants.NODESET);
		for (int i = 0; i < props.getLength(); i++)
			index((Element) props.item(i), "");

		String group = get("project.groupId", null);
		String artifact = getNoInheritance("project.artifactId", null);
		String version = get("project.version", null);
		this.packaging = getNoInheritance("project.packaging", "jar");

		Program program = Program.valueOf(group, artifact);
		if (program == null)
			throw new IllegalArgumentException("Invalid program for pom " + group + ":" + artifact);

		MavenVersion v = MavenVersion.parseMavenString(version);
		if (v == null)
			throw new IllegalArgumentException("Invalid version for pom " + group + ":" + artifact + ":" + version);

		this.revision = program.version(v);

		properties.put("pom.groupId", group);
		properties.put("pom.artifactId", artifact);
		properties.put("pom.version", version);
		if (parent.revision != null)
			properties.put("parent.version", parent.getVersion().toString());
		else
			properties.put("parent.version", "parent version from " + revision + " but not parent?");
		properties.put("version", version);
		properties.put("pom.currentVersion", version);
		properties.put("pom.packaging", this.packaging);

		NodeList dependencies = (NodeList) xp.evaluate("project/dependencies/dependency", doc, XPathConstants.NODESET);
		for (int i = 0; i < dependencies.getLength(); i++) {
			Element dependency = (Element) dependencies.item(i);
			Dependency d = dependency(dependency);
			this.dependencies.put(d.program, d);
		}

		NodeList dependencyManagement = (NodeList) xp.evaluate("project/dependencyManagement/dependencies/dependency",
				doc, XPathConstants.NODESET);
		for (int i = 0; i < dependencyManagement.getLength(); i++) {
			Element dependency = (Element) dependencyManagement.item(i);
			Dependency d = dependency(dependency);
			this.dependencyManagement.put(d.program, d);
		}
		xp = null;
	}

	private MavenVersion getVersion() {
		return revision.version;
	}

	private Dependency dependency(Element dependency) throws Exception {
		String groupId = get(dependency, "groupId", "<no group>");
		String artifactId = get(dependency, "artifactId", "<no artifact>");
		Dependency d = new Dependency();
		d.optional = isTrue(get(dependency, "optional", "true"));

		String version = get(dependency, "version", null);
		String extension = get(dependency, "type", "jar");
		String classifier = get(dependency, "classifier", null);
		String scope = get(dependency, "scope", "compile");

		Program program = Program.valueOf(groupId, artifactId);
		if (program == null)
			throw new IllegalArgumentException(
					"Invalid dependency in " + revision + " to " + groupId + ":" + artifactId);

		d.program = program;
		d.version = version;
		d.scope = MavenScope.getScope(scope);

		// TODO exclusions

		return d;
	}


	private Dependency getDirectDependency(Program program) {

		Dependency dependency = dependencies.get(program);
		if (dependency != null)
			return dependency;

		dependency = dependencyManagement.get(program);
		if (dependency != null)
			return dependency;

		if (parent != null)
			return parent.getDirectDependency(program);
		return null;
	}

	private boolean isTrue(String other) {
		return "true".equalsIgnoreCase(other);
	}

	private String get(Element dependency, String name, String deflt) throws XPathExpressionException {
		String value = xp.evaluate(name, dependency);
		if (value == null || value.isEmpty())
			return Strings.trim(deflt);

		return Strings.trim(replaceMacros(value));
	}

	private String get(String key, String deflt) {
		String value = properties.getProperty(key);
		if (value == null)
			value = deflt;

		return replaceMacros(value);
	}

	private String getNoInheritance(String key, String deflt) {
		String value = (String) properties.get(key);
		if (value == null)
			value = deflt;

		return replaceMacros(value);
	}

	final static Pattern MACRO_P = Pattern.compile("\\$\\{(?<env>env\\.)?(?<key>[.a-z0-9$_-]+)\\}",
			Pattern.CASE_INSENSITIVE);

	private String replaceMacros(String value) {
		Matcher m = MACRO_P.matcher(value);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String key = m.group("key");
			if (m.group("env") != null)
				m.appendReplacement(sb, replaceMacros(System.getenv(key)));
			else {
				String property = this.properties.getProperty(key);
				if (property != null && property.indexOf('$') >= 0)
					property = replaceMacros(property);

				if (property == null) {
					System.out.println("?? prop: " + key);
					m.appendReplacement(sb, Matcher.quoteReplacement("${" + key + "}"));
				} else
					m.appendReplacement(sb, Matcher.quoteReplacement(property));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private void index(Element node, String prefix, String... names) throws XPathExpressionException {
		String expr = "./*";
		if (names.length > 0) {
			StringBuilder sb = new StringBuilder("./*[");
			String del = "name()=";
			for (String name : names) {
				sb.append(del).append('"').append(name).append('"');
				del = " or name()=";
			}
			sb.append("]");
			expr = sb.toString();
		}

		NodeList childNodes = (NodeList) xp.evaluate(expr, node, XPathConstants.NODESET);
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			String key = child.getNodeName();
			String value = child.getTextContent().trim();
			properties.put(prefix + key, value);
		}
	}

	public Revision getRevision() {
		return revision;
	}

	public String getPackaging() {
		return packaging;
	}

	public Archive binaryArchive() {
		return revision.archive(
				packaging == null || packaging.isEmpty() || packaging.equals("bundle") || packaging.equals("pom")
						|| packaging.equals("eclipse-plugin")
						? "jar" : packaging,
				null);
	}

	@Override
	public Map<Program,Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception {
		return getDependencies(EnumSet.of(scope), transitive);
	}

	public Map<Program,Dependency> getDependencies(EnumSet<MavenScope> scope, boolean transitive) throws Exception {
		Map<Program,Dependency> deps = new LinkedHashMap<>();
		Set<Program> visited = new HashSet<>();
		getDependencies(deps, scope, transitive, visited);
		return deps;
	}

	private void resolve(Dependency d) {
		if (d.version == null) {
			Dependency dependency = dependencyManagement.get(d.program);
			if (dependency != null && dependency.version != null) {
				d.version = dependency.version;
				return;
			}
			Dependency directDependency = parent.getDirectDependency(d.program);
			if (directDependency == null) {
				d.error = "Cannot resolve ...";
			} else
				d.version = directDependency.version;
		}
	}
	private void getDependencies(Map<Program,Dependency> deps, EnumSet<MavenScope> scope, boolean transitive,
			Set<Program> visited) throws Exception {

		if (revision == null)
			return;

		if (!visited.add(revision.program))
			return;

		if (parent != null)
			parent.getDependencies(deps, scope, transitive, visited);

		List<Dependency> breadthFirst = new ArrayList<>();

		for (Map.Entry<Program,Dependency> e : dependencies.entrySet()) {
			Dependency d = e.getValue();
			
			resolve(d);

			if (deps.containsKey(d.program))
				continue;

			if (scope.contains(d.scope)) {

				deps.put(e.getKey(), d);
				if (transitive && d.scope.isTransitive())
					breadthFirst.add(d);
			}
		}

		for (Dependency d : breadthFirst)
			try {
				POM pom = repo.getPom(d.getRevision());
				if (pom == null) {
					continue;
				}
				pom.getDependencies(deps, scope, transitive, visited);
			} catch (Exception ee) {
				d.error = ee.toString();
			}
	}

	@Override
	public IPom getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "POM[" + revision + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((revision == null) ? 0 : revision.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		POM other = (POM) obj;
		if (revision == null) {
			if (other.revision != null)
				return false;
		} else if (!revision.equals(other.revision))
			return false;
		return true;
	}

	public boolean isPomOnly() {
		return "pom".equals(packaging);
	}

}
