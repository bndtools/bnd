package aQute.bnd.repository.maven.provider;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.xml.XML;
import aQute.maven.api.Archive;

/**
 * Helper for reading and writing pom.xml with GAV coordinates with
 * {@link IndexFile}
 */
abstract class POMIndexFile {

	private final static Logger logger = LoggerFactory.getLogger(POMIndexFile.class);

	private POMIndexFile() {}

	static Set<Archive> readFromPom(File pomFile, Consumer<String> statusSetter) throws Exception {
		Document doc = XML.newDocumentBuilderFactory()
			.newDocumentBuilder()
			.parse(pomFile);
		Set<Archive> result = new HashSet<>();
		NodeList deps = doc.getElementsByTagName("dependency");
		for (int i = 0; i < deps.getLength(); i++) {
			Node dep = deps.item(i);
			if (dep.getNodeType() != Node.ELEMENT_NODE)
				continue;
			// Only process direct project/dependencies/dependency (skip
			// dependencyManagement)
			Node parent = dep.getParentNode();
			if (parent == null || !"dependencies".equals(parent.getNodeName()))
				continue;
			Node grandparent = parent.getParentNode();
			if (grandparent == null || !"project".equals(grandparent.getNodeName()))
				continue;

			Element depEl = (Element) dep;
			String groupId = pomChildText(depEl, "groupId");
			String artifactId = pomChildText(depEl, "artifactId");
			String version = pomChildText(depEl, "version");
			if (groupId == null || artifactId == null || version == null)
				continue;

			String type = pomChildText(depEl, "type");
			if (type == null)
				type = Archive.JAR_EXTENSION;
			String classifier = pomChildText(depEl, "classifier");

			Archive archive = Archive.valueOf(groupId, artifactId, version, type, classifier);
			if (archive != null) {
				result.add(archive);
			} else {
				statusSetter.accept("Invalid GAV in pom.xml index: " + groupId + ":" + artifactId + ":" + version);
			}
		}
		logger.debug("loaded pom.xml index {}", result);
		return result;
	}

	private static String pomChildText(Element element, String tagName) {
		NodeList nl = element.getElementsByTagName(tagName);
		if (nl.getLength() == 0)
			return null;
		String text = nl.item(0)
			.getTextContent();
		return text != null ? text.trim() : null;
	}

	static void savePom(File indexFile, Map<Archive, Resource> archives)
		throws Exception {
		// Read existing project-level metadata to preserve user values
		String projectGroupId = "bnd.index";
		String[] parts = Strings.extension(indexFile.getName());
		String projectArtifactId = (parts != null) ? parts[0] : indexFile.getName();
		String projectVersion = "1.0-SNAPSHOT";
		String projectName = null;
		String projectDescription = null;

		if (indexFile.isFile() && indexFile.length() > 0) {
			try {
				Document doc = XML.newDocumentBuilderFactory()
					.newDocumentBuilder()
					.parse(indexFile);
				Element root = doc.getDocumentElement();
				String g = pomDirectChildText(root, "groupId");
				String a = pomDirectChildText(root, "artifactId");
				String v = pomDirectChildText(root, "version");
				if (g != null)
					projectGroupId = g;
				if (a != null)
					projectArtifactId = a;
				if (v != null)
					projectVersion = v;
				projectName = pomDirectChildText(root, "name");
				projectDescription = pomDirectChildText(root, "description");
			} catch (Exception e) {
				logger.debug("Could not read existing pom.xml metadata, using defaults", e);
			}
		}

		// Compute archives from current state (update() already applied
		// changes)
		List<Archive> sortedArchives = archives.keySet()
			.stream()
			.sorted(Comparator.comparing((Archive a) -> a.revision.program.group)
				.thenComparing(a -> a.revision.program.artifact)
				.thenComparing(Comparator.comparing(a -> a.extension))
				.thenComparing(Comparator.comparing(a -> a.classifier))
				.thenComparing(a -> a.revision.version.toString()))
			.collect(toList());

		// Ensure parent directory exists
		File parent = indexFile.getParentFile();
		if (parent != null && !parent.isDirectory()) {
			IO.mkdirs(indexFile.getParentFile());
		}

		XMLOutputFactory xof = XMLOutputFactory.newFactory();
		xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);

		final String POM_NS = "http://maven.apache.org/POM/4.0.0";
		final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

		try (FileOutputStream fos = new FileOutputStream(indexFile)) {
			XMLStreamWriter xsw = xof.createXMLStreamWriter(fos, "UTF-8");
			try {
				xsw.writeStartDocument("UTF-8", "1.0");
				xsw.writeCharacters("\n");

				xsw.setDefaultNamespace(POM_NS);
				xsw.setPrefix("xsi", XSI_NS);
				xsw.writeStartElement(POM_NS, "project");
				xsw.writeDefaultNamespace(POM_NS);
				xsw.writeNamespace("xsi", XSI_NS);
				xsw.writeAttribute(XSI_NS, "schemaLocation", POM_NS + " http://maven.apache.org/xsd/maven-4.0.0.xsd");

				writePomElement(xsw, POM_NS, "modelVersion", "4.0.0", 1);
				writePomElement(xsw, POM_NS, "groupId", projectGroupId, 1);
				writePomElement(xsw, POM_NS, "artifactId", projectArtifactId, 1);
				writePomElement(xsw, POM_NS, "version", projectVersion, 1);
				writePomElement(xsw, POM_NS, "packaging", "pom", 1);
				if (projectName != null)
					writePomElement(xsw, POM_NS, "name", projectName, 1);
				if (projectDescription != null)
					writePomElement(xsw, POM_NS, "description", projectDescription, 1);

				if (!sortedArchives.isEmpty()) {
					writePomIndent(xsw, 1);
					xsw.writeStartElement(POM_NS, "dependencies");
					for (Archive arch : sortedArchives) {
						writePomIndent(xsw, 2);
						xsw.writeStartElement(POM_NS, "dependency");
						writePomElement(xsw, POM_NS, "groupId", arch.revision.program.group, 3);
						writePomElement(xsw, POM_NS, "artifactId", arch.revision.program.artifact, 3);
						writePomElement(xsw, POM_NS, "version", arch.revision.version.toString(), 3);
						if (!Archive.JAR_EXTENSION.equals(arch.extension))
							writePomElement(xsw, POM_NS, "type", arch.extension, 3);
						if (!arch.classifier.isEmpty())
							writePomElement(xsw, POM_NS, "classifier", arch.classifier, 3);
						writePomIndent(xsw, 2);
						xsw.writeEndElement(); // dependency
					}
					writePomIndent(xsw, 1);
					xsw.writeEndElement(); // dependencies
				}

				xsw.writeCharacters("\n");
				xsw.writeEndElement(); // project
				xsw.writeCharacters("\n");
				xsw.writeEndDocument();
			} finally {
				xsw.flush();
				xsw.close();
			}
		}
	}

	private static void writePomElement(XMLStreamWriter xsw, String ns, String localName, String text, int depth)
		throws XMLStreamException {
		writePomIndent(xsw, depth);
		xsw.writeStartElement(ns, localName);
		xsw.writeCharacters(text);
		xsw.writeEndElement();
	}

	private static void writePomIndent(XMLStreamWriter xsw, int depth) throws XMLStreamException {
		xsw.writeCharacters("\n" + "  ".repeat(depth));
	}

	/**
	 * Get the text content of the first direct child element with the given tag
	 * name. Unlike {@link #pomChildText} which uses
	 * {@code getElementsByTagName} (recursive), this method only searches
	 * immediate children, making it suitable for reading project-level metadata
	 * without accidentally picking up nested element values.
	 */
	private static String pomDirectChildText(Element parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
				String text = child.getTextContent();
				return text != null ? text.trim() : null;
			}
		}
		return null;
	}


}
