package aQute.maven.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.maven.api.Archive;
import aQute.maven.api.IPom;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;

/**
 * Parser and placeholder for POM information.
 */
public class POM implements IPom {
	static Logger						l						= LoggerFactory.getLogger(POM.class);

	static DocumentBuilderFactory		dbf						= DocumentBuilderFactory.newInstance();
	static XPathFactory					xpf						= XPathFactory.newInstance();
	private Revision					revision;
	private String						packaging;
	private final Properties			properties;
	private final POM					parent;
	private Map<Program, Dependency>	dependencies			= new LinkedHashMap<>();
	private Map<Program, Dependency>	dependencyManagement	= new LinkedHashMap<>();
	private XPath						xp;
	private String[]					JAR_PACKAGING			= {
		"bundle", "eclipse-plugin", "eclipse-test-plugin", Archive.POM_EXTENSION
	};

	private MavenRepository				repo;

	private boolean						ignoreParentIfAbsent;

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
		this(repo, in, false);
	}

	public POM(MavenRepository repo, InputStream in, boolean ignoreParentIfAbsent) throws Exception {
		this(repo, IO.work, getDocBuilder().parse(processEntities(in)), ignoreParentIfAbsent);
	}

	private static DocumentBuilder getDocBuilder() throws ParserConfigurationException {
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db;
	}

	private static InputStream processEntities(InputStream in) throws IOException {
		ByteBuffer bb = IO.copy(in, new ByteBufferOutputStream(in.available() + 1))
			.toByteBuffer();
		return processEntities(bb);
	}

	private static InputStream processEntities(ByteBuffer bb) {
		final byte[] array = bb.array();
		final int offset = bb.arrayOffset();
		final int limit = offset + bb.limit();
		for (int i = offset; i < limit; i++) {
			char c = (char) array[i];
			if (c == '&') {
				final int jlimit = Math.min(limit, i + 11);
				for (int j = i + 1; j < jlimit; j++) {
					c = (char) array[j];
					if (c == ';') {
						String entity = new String(array, i + 1, j - (i + 1), StandardCharsets.US_ASCII).toLowerCase();
						switch (entity) {
							case "lt" :
							case "gt" :
							case "amp" :
							case "quot" :
							case "apos" :
								break;
							default :
								array[i] = '?';
								break;
						}
						i = j;
						break;
					}
					if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')) {
						break;
					}
				}
			}
		}
		return new ByteBufferInputStream(bb);
	}

	public POM(MavenRepository repo, File file) throws Exception {
		this(repo, file, false);
	}

	public POM(MavenRepository repo, File file, boolean ignoreIfParentAbsent) throws Exception {
		this(repo, file.getParentFile(), getDocBuilder().parse(processEntities(file)), ignoreIfParentAbsent);
	}

	private static InputStream processEntities(File file) throws IOException {
		try (FileChannel in = IO.readChannel(file.toPath())) {
			ByteBuffer bb = ByteBuffer.allocate((int) in.size());
			while (in.read(bb) > 0) {}
			bb.flip();
			return processEntities(bb);
		}
	}

	public POM(MavenRepository repo, Document doc) throws Exception {
		this(repo, doc, false);
	}

	public POM(MavenRepository repo, Document doc, boolean ignoreIfParentAbsent) throws Exception {
		this(repo, IO.work, doc, ignoreIfParentAbsent);
	}

	private POM(MavenRepository repo, File base, Document doc, boolean ignoreIfParentAbsent) throws Exception {
		this.repo = repo;
		this.ignoreParentIfAbsent = ignoreIfParentAbsent;
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
			if (relativePath != null && !relativePath.isEmpty() && (fp = IO.getFile(base, relativePath)).isFile()) {
				this.parent = new POM(repo, fp);
			} else {
				Revision revision = program.version(v);
				POM pom = null;

				try {
					pom = repo.getPom(revision);
				} catch (Exception e) {
					if (!this.ignoreParentIfAbsent)
						throw e;
				}

				if (pom == null) {
					if (this.ignoreParentIfAbsent)
						pom = new POM(); // not found
					else
						throw new IllegalArgumentException("No parent for pom. Missing parent: " + revision);
				}
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

		String group = getOrSet("project.groupId", parentGroup);
		String artifact = getOrSetNoInheritance("project.artifactId", null);
		String version = getOrSet("project.version", parentVersion);
		this.packaging = getOrSetNoInheritance("project.packaging", "jar");

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
			properties.put("parent.version", parent.getVersion()
				.toString());
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
		d.type = extension;
		d.classifier = classifier;
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

	private String getOrSet(String key, String deflt) {
		String value = properties.getProperty(key);
		if (value == null) {
			value = deflt;
			if (value != null) {
				properties.setProperty(key, value);
			}
		}

		if (value == null)
			return null;

		return replaceMacros(value);
	}

	private String getOrSetNoInheritance(String key, String deflt) {
		String value = (String) properties.get(key);
		if (value == null) {
			value = deflt;
			if (value != null) {
				properties.setProperty(key, value);
			}
		}

		return replaceMacros(value);
	}

	private final static Pattern MACRO_P = Pattern.compile("\\$\\{(?<prop>(?<env>env\\.)?(?<key>[-.$\\w]+))\\}");

	private String replaceMacros(String value) {
		Matcher m = MACRO_P.matcher(value);
		StringBuilder sb = new StringBuilder();
		int start = 0;
		for (; m.find(); start = m.end()) {
			String key = m.group("key");
			String property = (m.group("env") != null) ? System.getenv(key) : this.properties.getProperty(key);
			if (property != null && property.indexOf('$') >= 0) {
				property = replaceMacros(property);
			}
			if (property == null) {
				l.info("Undefined property in {} : key {}", this, m.group("prop"));
				property = m.group(0);
			}
			sb.append(value, start, m.start())
				.append(property);
		}
		return (start == 0) ? value
			: sb.append(value, start, value.length())
				.toString();
	}

	private void index(Element node, String prefix, String... names) throws XPathExpressionException {
		String expr = "./*";
		if (names.length > 0) {
			StringBuilder sb = new StringBuilder("./*[");
			String del = "name()=";
			for (String name : names) {
				sb.append(del)
					.append('"')
					.append(name)
					.append('"');
				del = " or name()=";
			}
			sb.append("]");
			expr = sb.toString();
		}

		NodeList childNodes = (NodeList) xp.evaluate(expr, node, XPathConstants.NODESET);
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			String key = child.getNodeName();
			String value = child.getTextContent()
				.trim();
			properties.put(prefix + key, value);
		}
	}

	@Override
	public Revision getRevision() {
		return revision;
	}

	@Override
	public String getPackaging() {
		return packaging;
	}

	@Override
	public Archive binaryArchive() {
		return revision.archive(
			packaging == null || packaging.isEmpty() || Strings.in(JAR_PACKAGING, packaging) ? "jar" : packaging, null);
	}

	@Override
	public Map<Program, Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception {
		return getDependencies(EnumSet.of(scope), transitive);
	}

	public Map<Program, Dependency> getDependencies(EnumSet<MavenScope> scope, boolean transitive) throws Exception {
		Map<Program, Dependency> deps = new LinkedHashMap<>();
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

	private void getDependencies(Map<Program, Dependency> deps, EnumSet<MavenScope> scope, boolean transitive,
		Set<Program> visited) throws Exception {

		if (revision == null)
			return;

		if (!visited.add(revision.program))
			return;

		if (parent != null)
			parent.getDependencies(deps, scope, transitive, visited);

		List<Dependency> breadthFirst = new ArrayList<>();

		for (Map.Entry<Program, Dependency> e : dependencies.entrySet()) {
			Dependency d = e.getValue();

			resolve(d);

			if (deps.containsKey(d.program))
				continue;

			if (scope.contains(d.scope)) {
				d.bindToVersion(repo);
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
		return Archive.POM_EXTENSION.equals(packaging);
	}

	@Override
	public boolean hasValidGAV() {
		return isOk(properties.getProperty("project.version")) && isOk(properties.getProperty("project.groupId"))
			&& isOk(properties.getProperty("project.artifactId"));

	}

	private boolean isOk(String property) {
		// check for macros
		if (property != null && property.indexOf('$') >= 0)
			return false;

		return true;
	}

}
