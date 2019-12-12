package aQute.bnd.maven.support;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class ProjectPom extends Pom {

	final List<URI>		repositories	= new ArrayList<>();
	final Properties	properties		= new UTF8Properties();
	final Reporter		reporter;
	String				packaging;
	String				url;

	ProjectPom(Maven maven, File pomFile) throws Exception {
		this(maven, pomFile, new ReporterAdapter());
	}

	ProjectPom(Maven maven, File pomFile, Reporter reporter) throws Exception {
		super(maven, pomFile, pomFile.toURI(), reporter);
		this.reporter = reporter;
	}

	@Override
	protected void parse(Document doc, XPath xp) throws Exception {

		packaging = xp.evaluate("project/packaging", doc);
		url = xp.evaluate("project/url", doc);

		Node parent = (Node) xp.evaluate("project/parent", doc, XPathConstants.NODE);
		if (parent != null && parent.hasChildNodes()) {
			File parentFile = IO.getFile(getPomFile().getParentFile(), "../pom.xml");

			String parentGroupId = xp.evaluate("groupId", parent)
				.trim();
			String parentArtifactId = xp.evaluate("artifactId", parent)
				.trim();
			String parentVersion = xp.evaluate("version", parent)
				.trim();
			String parentPath = xp.evaluate("relativePath", parent)
				.trim();
			if (parentPath != null && parentPath.length() != 0) {
				parentFile = IO.getFile(getPomFile().getParentFile(), parentPath);
			}
			if (parentFile.isFile()) {
				ProjectPom parentPom = new ProjectPom(maven, parentFile, reporter);
				parentPom.parse();
				dependencies.addAll(parentPom.dependencies);
				for (Enumeration<?> e = parentPom.properties.propertyNames(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					if (!properties.contains(key))
						properties.put(key, parentPom.properties.get(key));
				}
				repositories.addAll(parentPom.repositories);

				setNames(parentPom);
			} else {
				// This seems to be a bit bizarre, extending an external pom?
				CachedPom parentPom = maven.getPom(parentGroupId, parentArtifactId, parentVersion);
				dependencies.addAll(parentPom.dependencies);
				setNames(parentPom);
			}
		}

		NodeList propNodes = (NodeList) xp.evaluate("project/properties/*", doc, XPathConstants.NODESET);
		for (int i = 0; i < propNodes.getLength(); i++) {
			Node node = propNodes.item(i);
			String key = node.getNodeName();
			String value = node.getTextContent();
			if (key == null || key.length() == 0)
				throw new IllegalArgumentException("Pom has an empty or null key");
			if (value == null || value.length() == 0)
				throw new IllegalArgumentException("Pom has an empty or null value for property " + key);
			properties.setProperty(key, value.trim());
		}

		NodeList repos = (NodeList) xp.evaluate("project/repositories/repository/url", doc, XPathConstants.NODESET);
		for (int i = 0; i < repos.getLength(); i++) {
			Node node = repos.item(i);
			String URIString = node.getTextContent()
				.trim();
			URI uri = new URI(URIString);
			if (uri.getScheme() == null)
				uri = IO.getFile(pomFile.getParentFile(), URIString)
					.toURI();
			repositories.add(uri);
		}

		super.parse(doc, xp);
	}

	// private void print(Node node, String indent) {
	// System.err.print(indent);
	// System.err.println(node.getNodeName());
	// Node rover = node.getFirstChild();
	// while ( rover != null) {
	// print( rover, indent+" ");
	// rover = rover.getNextSibling();
	// }
	// }

	/**
	 * @param parentArtifactId
	 * @param parentGroupId
	 * @param parentVersion
	 * @throws Exception
	 */
	private void setNames(Pom pom) throws Exception {
		if (artifactId == null || artifactId.length() == 0)
			artifactId = pom.getArtifactId();
		if (groupId == null || groupId.length() == 0)
			groupId = pom.getGroupId();
		if (version == null || version.length() == 0)
			version = pom.getVersion();
		if (description == null)
			description = pom.getDescription();
		else
			description = pom.getDescription() + "\n" + description;

	}

	static class Rover {

		public Rover(Rover rover, Dependency d) {
			this.previous = rover;
			this.dependency = d;
		}

		final Rover			previous;
		final Dependency	dependency;

		public boolean excludes(String name) {
			return dependency.exclusions.contains(name) && previous != null && previous.excludes(name);
		}
	}

	public Set<Pom> getDependencies(Scope action) throws Exception {
		return getDependencies(action, repositories.toArray(new URI[0]));
	}

	// Match any macros
	private final static Pattern MACRO = Pattern.compile("(\\$\\{\\s*([^}\\s]+)\\s*\\})");

	@Override
	protected String replace(String in) {
		reporter.trace("Replace: %s", in);
		if (in == null) {
			reporter.error("No input to replace. Setting it to <<???>>");
			in = "<<???>>";
		}
		Matcher matcher = MACRO.matcher(in);
		int last = 0;
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			int n = matcher.start();
			sb.append(in, last, n);
			String replacement = get(matcher.group(2));
			if (replacement == null)
				sb.append(matcher.group(1));
			else
				sb.append(replacement);
			last = matcher.end();
		}
		if (last == 0)
			return in;

		sb.append(in, last, in.length());
		return sb.toString();
	}

	private String get(String key) {
		if (key.equals("pom.artifactId"))
			return artifactId;
		if (key.equals("pom.groupId"))
			return groupId;
		if (key.equals("pom.version"))
			return version;

		if (key.equals("pom.name"))
			return name;

		String prop = properties.getProperty(key);
		if (prop != null)
			return prop;

		return System.getProperty(key);
	}

	public Properties getProperties() {
		return properties;
	}

	public String getPackaging() {
		return packaging;
	}

	public String getUrl() {
		return url;
	}

	public String getProperty(String key) {
		String s = properties.getProperty(key);
		return replace(s);
	}

	@Override
	public File getArtifact() throws Exception {
		return null;
	}
}
