package aQute.bnd.maven;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.WriteResource;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class PomResource extends WriteResource {
	private static final String	VERSION		= "version";
	private static final String	ARTIFACTID	= "artifactid";
	private static final String	GROUPID		= "groupid";
	private static final String	WHERE		= "where";
	final Manifest				manifest;
	private Map<String,String>	scm;
	final Map<String,String>	processor;
	final static Pattern		NAME_URL	= Pattern.compile("(.*)(https?://.*)", Pattern.CASE_INSENSITIVE);
	private String				where;
	private String				groupId;
	private String				artifactId;
	private String				version;
	private String				name;

	public PomResource(Manifest manifest) {
		this(new HashMap<String,String>(), manifest);
	}

	public PomResource(Map<String,String> b, Manifest manifest) {
		this.manifest = manifest;
		this.processor = b;

		Domain domain = Domain.domain(manifest);
		String bsn = domain.getBundleSymbolicName().getKey();
		if (bsn == null) {
			throw new RuntimeException("Cannot create POM unless bsn is set");
		}
		name = domain.get(Constants.BUNDLE_NAME);
		where = processor.get(WHERE);

		if (processor.containsKey(GROUPID)) {
			groupId = processor.get(GROUPID);
			artifactId = processor.get(ARTIFACTID);
			if (artifactId == null)
				artifactId = bsn;
			if (where == null) {
				where = String.format("META-INF/maven/%s/%s/pom.xml", groupId, artifactId);
			}
		} else {
			int n = bsn.lastIndexOf('.');
			if (n <= 0)
				throw new RuntimeException("\"" + GROUPID + "\" not set and" + Constants.BUNDLE_SYMBOLICNAME
						+ " does not contain a '.' to separate into a groupid and artifactid.");

			artifactId = processor.get(ARTIFACTID);
			if (artifactId == null)
				artifactId = bsn.substring(n + 1);
			groupId = bsn.substring(0, n);
			if (where == null) {
				where = "pom.xml";
			}
		}
		if (name == null) {
			name = groupId + ":" + artifactId;
		}

		version = processor.get(VERSION);
		if (version == null)
			version = domain.getBundleVersion();
		if (version == null)
			version = "0";

	}

	public String getWhere() {
		return where;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		PrintWriter pw = IO.writer(out);

		String description = manifest.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION);
		String docUrl = manifest.getMainAttributes().getValue(Constants.BUNDLE_DOCURL);
		String bundleVendor = manifest.getMainAttributes().getValue(Constants.BUNDLE_VENDOR);
		String licenses = manifest.getMainAttributes().getValue(Constants.BUNDLE_LICENSE);

		Tag project = new Tag("project");
		project.addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
		project.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		project.addAttribute("xsi:schemaLocation",
				"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd");

		project.addContent(new Tag("modelVersion").addContent("4.0.0"));
		project.addContent(new Tag("groupId").addContent(getGroupId()));
		project.addContent(new Tag("artifactId").addContent(getArtifactId()));
		project.addContent(new Tag(VERSION).addContent(getVersion()));

		if (description == null) {
			description = name;
		}
		new Tag(project, "description").addContent(description);
		new Tag(project, "name").addContent(name);

		if (docUrl != null) {
			new Tag(project, "url").addContent(docUrl);
		}

		if (scm != null) {
			Tag scm = new Tag(project, "scm");
			for (Map.Entry<String,String> e : this.scm.entrySet()) {
				new Tag(scm, e.getKey()).addContent(e.getValue());
			}
		}

		if (bundleVendor != null) {
			Matcher m = NAME_URL.matcher(bundleVendor);
			String namePart = bundleVendor;
			String urlPart = null;
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
		if (licenses != null) {
			Tag ls = new Tag(project, "licenses");

			Parameters map = Processor.parseHeader(licenses, null);
			for (Iterator<Entry<String,Attrs>> e = map.entrySet().iterator(); e.hasNext();) {

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

				Entry<String,Attrs> entry = e.next();
				Tag l = new Tag(ls, "license");
				Map<String,String> values = entry.getValue();
				String url = entry.getKey();

				if (values.containsKey("description"))
					tagFromMap(l, values, "description", "name", url);
				else
					tagFromMap(l, values, "name", "name", url);

				tagFromMap(l, values, "url", "url", url);
				tagFromMap(l, values, "distribution", "distribution", "repo");
			}
		}

		String scm = processor.get(Constants.BUNDLE_SCM);
		if (scm != null && scm.length() > 0) {
			Attrs pscm = OSGiHeader.parseProperties(scm);

			Tag tscm = new Tag(project, "scm");
			for (String s : pscm.keySet()) {
				new Tag(tscm, s, pscm.get(s));
			}
		}

		Parameters developers = new Parameters(processor.get(Constants.BUNDLE_DEVELOPERS));
		if (developers.size() > 0) {
			Tag tdevelopers = new Tag(project, "developers");

			for (String id : developers.keySet()) {
				Tag tdeveloper = new Tag(tdevelopers, "developer");
				new Tag(tdeveloper, "id", id);

				Attrs i = new Attrs(developers.get(id));
				if (!i.containsKey("email"))
					i.put("email", id);

				i.remove("id");

				for (String s : i.keySet()) {
					if (s.equals("roles")) {
						Tag troles = new Tag(tdeveloper, "roles");

						String[] roles = i.get(s).trim().split("\\s*,\\s*");
						for (String role : roles) {
							new Tag(troles, "role", role);
						}
					} else
						new Tag(tdeveloper, s, i.get(s));
				}
			}
		}

		pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		project.print(0, pw);
		pw.flush();
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
	private Tag tagFromMap(Tag parent, Map<String,String> values, String string, String tag, String object) {
		String value = values.get(string);
		if (value == null)
			value = object;
		if (value == null)
			return parent;
		new Tag(parent, tag).addContent(value.trim());
		return parent;
	}

	public void setProperties(Map<String,String> scm) {
		this.scm = scm;
	}
}
