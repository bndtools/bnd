package aQute.bnd.maven;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.WriteResource;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class PomFromManifest extends WriteResource {
	final Manifest					manifest;
	private List<String>			scm			= new ArrayList<>();
	private List<String>			developers	= new ArrayList<>();
	private final static Pattern	NAME_URL	= Pattern.compile("(.*)(https?://.*)", Pattern.CASE_INSENSITIVE);
	private final static Pattern	EMAIL_P		= Pattern.compile("([^@]+)@([-.\\w]+)\\.([-.\\w]+)");
	String							xbsn;
	String							xversion;
	String							xgroupId;
	String							xartifactId;
	private String					projectURL;

	public String getBsn() {
		if (xbsn == null)
			xbsn = manifest.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (xbsn == null)
			throw new RuntimeException("Cannot create POM unless bsn is set");

		xbsn = xbsn.trim();
		int n = xbsn.lastIndexOf('.');
		if (n < 0) {
			n = xbsn.length();
			xbsn = xbsn + "." + xbsn;
		}

		if (xgroupId == null)
			xgroupId = xbsn.substring(0, n);
		if (xartifactId == null) {
			xartifactId = xbsn.substring(n + 1);
			n = xartifactId.indexOf(';');
			if (n > 0)
				xartifactId = xartifactId.substring(0, n)
					.trim();
		}

		return xbsn;
	}

	public String getGroupId() {
		getBsn();
		return xgroupId;
	}

	public String getArtifactId() {
		getBsn();
		return xartifactId;
	}

	public Version getVersion() {
		if (xversion != null)
			return new Version(xversion);
		String version = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_VERSION);
		Version v = new Version(version);
		return new Version(v.getMajor(), v.getMinor(), v.getMicro());
	}

	public PomFromManifest(Manifest manifest) {
		this.manifest = manifest;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		PrintWriter ps = IO.writer(out);

		String name = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_NAME);

		String description = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DESCRIPTION);
		String docUrl = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DOCURL);
		String bundleVendor = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_VENDOR);

		String licenses = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_LICENSE);

		Tag project = new Tag("project");
		project.addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
		project.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		project.addAttribute("xsi:schemaLocation",
			"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd");

		project.addContent(new Tag("modelVersion").addContent("4.0.0"));
		project.addContent(new Tag("groupId").addContent(getGroupId()));
		project.addContent(new Tag("artifactId").addContent(getArtifactId()));
		project.addContent(new Tag("version").addContent(getVersion().toString()));

		if (description != null) {
			new Tag(project, "description").addContent(description);
		}
		if (name != null) {
			new Tag(project, "name").addContent(name);
		}

		if (projectURL != null)
			new Tag(project, "url").addContent(projectURL);
		else if (docUrl != null) {
			new Tag(project, "url").addContent(docUrl);
		} else
			new Tag(project, "url").addContent("http://no-url");

		String scmheader = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_SCM);
		if (scmheader != null)
			scm.add(scmheader);

		Tag scmtag = new Tag(project, "scm");
		if (scm != null && !scm.isEmpty()) {
			for (String cm : this.scm) {
				new Tag(scmtag, "url").addContent(cm);
				new Tag(scmtag, "connection").addContent(cm);
				new Tag(scmtag, "developerConnection").addContent(cm);
			}
		} else {
			new Tag(scmtag, "url").addContent("private");
			new Tag(scmtag, "connection").addContent("private");
			new Tag(scmtag, "developerConnection").addContent("private");
		}

		if (bundleVendor != null) {
			Matcher m = NAME_URL.matcher(bundleVendor);
			String namePart = bundleVendor;
			String urlPart = this.projectURL;
			if (m.matches()) {
				namePart = m.group(1);
				urlPart = m.group(2);
			}
			Tag organization = new Tag(project, "organization");
			new Tag(organization, "name").addContent(namePart.trim());
			if (urlPart != null) {
				new Tag(organization, "url").addContent(urlPart.trim());
			}
		}
		if (!developers.isEmpty()) {
			Tag d = new Tag(project, "developers");
			for (String email : developers) {
				String id = email;
				String xname = email;
				String organization = null;

				Matcher m = EMAIL_P.matcher(email);
				if (m.matches()) {
					xname = m.group(1);
					organization = m.group(2);
				}

				Tag developer = new Tag(d, "developer");
				new Tag(developer, "id").addContent(id);
				new Tag(developer, "name").addContent(xname);
				new Tag(developer, "email").addContent(email);
				if (organization != null)
					new Tag(developer, "organization").addContent(organization);
			}
		}
		if (licenses != null) {
			Tag ls = new Tag(project, "licenses");

			Parameters map = Processor.parseHeader(licenses, null);
			for (Iterator<Entry<String, Attrs>> e = map.entrySet()
				.iterator(); e.hasNext();) {

				// Bundle-License:
				// http://www.opensource.org/licenses/apache2.0.php; \
				// description="${Bundle-Copyright}"; \
				// link=LICENSE
				//
				//  <license>
				//    <name>This material is licensed under the Apache
				// Software License, Version 2.0</name>
				//    <url>http://www.apache.org/licenses/LICENSE-2.0</url>
				//    <distribution>repo</distribution>
				//    </license>

				Entry<String, Attrs> entry = e.next();
				Tag l = new Tag(ls, "license");
				Map<String, String> values = entry.getValue();
				String url = entry.getKey();

				if (values.containsKey("description"))
					tagFromMap(l, values, "description", "name", url);
				else
					tagFromMap(l, values, "name", "name", url);

				tagFromMap(l, values, "url", "url", url);
				tagFromMap(l, values, "distribution", "distribution", "repo");
			}
		}
		project.print(0, ps);
		ps.flush();
	}

	/**
	 * Utility function to print a tag from a map
	 *
	 * @param ps
	 * @param values
	 * @param string
	 * @param tag
	 * @param object
	 */
	private Tag tagFromMap(Tag parent, Map<String, String> values, String string, String tag, String object) {
		String value = values.get(string);
		if (value == null)
			value = object;
		if (value == null)
			return parent;
		new Tag(parent, tag).addContent(value.trim());
		return parent;
	}

	public void setSCM(String scm) {
		this.scm.add(scm);
	}

	public void setURL(String url) {
		this.projectURL = url;
	}

	public void setBsn(String bsn) {
		this.xbsn = bsn;
	}

	public void addDeveloper(String email) {
		this.developers.add(email);
	}

	public void setVersion(String version) {
		this.xversion = version;
	}

	public void setArtifact(String artifact) {
		this.xartifactId = artifact;
	}

	public void setGroup(String group) {
		this.xgroupId = group;
	}
}
