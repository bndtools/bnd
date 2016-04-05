package aQute.maven.api;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import aQute.bnd.version.MavenVersion;

/**
 * Parser and placeholder for POM information.
 */
public class POM {
	static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf	= XPathFactory.newInstance();
	private Revision				revision;
	private String					packaging;

	public POM(InputStream in) throws Exception {
		this(dbf.newDocumentBuilder().parse(in));
	}

	public POM(File file) throws Exception {
		this(dbf.newDocumentBuilder().parse(file));
	}

	public POM(Document doc) throws Exception {
		XPath xp = xpf.newXPath();
		String group = xp.evaluate("project/groupId", doc);
		String artifact = xp.evaluate("project/artifactId", doc);
		String version = xp.evaluate("project/version", doc);
		this.packaging = xp.evaluate("project/packaging", doc);
		if (this.packaging == null)
			this.packaging = "jar";

		Program p = Program.valueOf(group, artifact);
		this.revision = p.version(new MavenVersion(version));
	}

	public Revision getRevision() {
		return revision;
	}

	public String getPackaging() {
		return packaging;
	}

	public Archive binaryArchive() {
		return revision.archive(packaging == null || packaging.isEmpty() ? "jar" : packaging, null);
	}
}
