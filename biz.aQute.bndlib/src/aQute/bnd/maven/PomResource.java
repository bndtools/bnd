package aQute.bnd.maven;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.WriteResource;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;
import aQute.libg.glob.Glob;

public class PomResource extends WriteResource {
	private static final String	VERSION		= "version";
	private static final String	ARTIFACTID	= "artifactid";
	private static final String	GROUPID		= "groupid";
	private static final String	WHERE		= "where";
	final Manifest				manifest;
	private Map<String, String>	scm;
	final Processor				processor;
	final static Pattern		NAME_URL	= Pattern.compile("(.*)(https?://.*)", Pattern.CASE_INSENSITIVE);
	private String				where;
	private String				groupId;
	private String				artifactId;
	private String				version;
	private String				name;

	public PomResource(Manifest manifest) {
		this(new Processor(), manifest);
	}

	public PomResource(Map<String, String> b, Manifest manifest) {
		this(asProcessor(b), manifest);
	}

	private static Processor asProcessor(Map<String, String> b) {
		Processor p = new Processor();
		p.addProperties(b);
		return p;
	}

	public PomResource(Processor b, Manifest manifest) {
		this.manifest = manifest;
		this.processor = b;

		Domain domain = Domain.domain(manifest);
		Entry<String, Attrs> bundleSymbolicName = domain.getBundleSymbolicName();
		if (bundleSymbolicName == null) {
			throw new RuntimeException("Cannot create POM unless bsn is set");
		}
		String bsn = bundleSymbolicName.getKey();
		if (bsn == null) {
			throw new RuntimeException("Cannot create POM unless bsn is set");
		}

		groupId = augmentManifest(domain, bsn);

		name = domain.get(Constants.BUNDLE_NAME);
		where = processor.get(WHERE);

		if (groupId == null)
			groupId = processor.get(GROUPID);

		if (groupId == null) {
			groupId = processor.get(aQute.bnd.osgi.Constants.GROUPID);
		}

		if (groupId == null) {
			groupId = processor.get("groupId");
		}

		if (groupId != null) {
			artifactId = processor.get(ARTIFACTID);
			if (artifactId == null)
				artifactId = processor.get("artifactId");

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

	public PomResource(Processor p, Manifest m, String groupId, String artifactId, String version, String where) {
		this.processor = p;
		this.manifest = m;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.where = where;
	}

	public String augmentManifest(Domain domain, String bsn) {
		String groupid = null;
		Parameters augments = new Parameters(processor.mergeProperties("-pomaugment"), processor);

		for (Entry<String, Attrs> augment : augments.entrySet()) {
			Glob g = new Glob(augment.getKey());

			if (g.matcher(bsn)
				.matches()) {
				Attrs attrs = augment.getValue();
				for (Entry<String, String> attr : attrs.entrySet()) {
					String key = attr.getKey();
					boolean mandatory = false;
					if (key.startsWith("+")) {
						key = key.substring(1);
						mandatory = true;
					}

					if (key.length() > 0 && Character.isUpperCase(key.charAt(0))) {

						if (mandatory || domain.get(key) == null) {
							domain.set(key, attr.getValue());
						}

					} else {
						if ("groupid".equals(key))
							groupid = attr.getValue();
					}
				}
				break;
			}
		}
		return groupid;
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
		String description = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DESCRIPTION);
		String docUrl = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_DOCURL);
		String bundleVendor = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_VENDOR);
		String bundleLicense = manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_LICENSE);

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
			for (Map.Entry<String, String> e : this.scm.entrySet()) {
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
		Tag ls = null;
		Parameters licenses = new Parameters(bundleLicense, processor);
		for (Entry<String, Attrs> license : licenses.entrySet()) {
			// Bundle-License: identifier;description="description";link="URL"
			//
			// <licenses>
			//   <license>
			//     <name>identifier</name>
			//     <url>URL</url>
			//     <distribution>repo</distribution>
			//     <comments>description</comments>
			//   </license>
			// </licenses>

			String identifier = license.getKey();
			if (identifier == null)
				continue;
			identifier = identifier.trim();
			if (identifier.equals("<<EXTERNAL>>"))
				continue;
			if (ls == null)
				ls = new Tag(project, "licenses");
			Tag l = new Tag(ls, "license");
			Map<String, String> attrs = license.getValue();
			tagFromMap(l, attrs, "name", "name", identifier);
			tagFromMap(l, attrs, "link", "url", identifier);
			tagFromMap(l, attrs, "distribution", "distribution", "repo");
			tagFromMap(l, attrs, "description", "comments", null);
		}

		String scm = manifest.getMainAttributes()
			.getValue(aQute.bnd.osgi.Constants.BUNDLE_SCM);
		if (scm != null && scm.length() > 0) {
			Attrs pscm = OSGiHeader.parseProperties(scm);

			Tag tscm = new Tag(project, "scm");
			for (String s : pscm.keySet()) {
				new Tag(tscm, s, pscm.get(s));
			}
		}

		Parameters developers = new Parameters(manifest.getMainAttributes()
			.getValue(aQute.bnd.osgi.Constants.BUNDLE_DEVELOPERS), processor);

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

						String[] roles = i.get(s)
							.trim()
							.split("\\s*,\\s*");
						for (String role : roles) {
							new Tag(troles, "role", role);
						}
					} else
						new Tag(tdeveloper, s, i.get(s));
				}
			}
		}
		String validate = project.validate();
		if (validate != null)
			throw new IllegalArgumentException(validate);

		PrintWriter pw = IO.writer(out);
		pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		project.print(0, pw);
		pw.flush();
	}

	/**
	 * Utility function to print a tag from a map
	 * 
	 * @param parent
	 * @param attrs
	 * @param key
	 * @param tag
	 * @param defaultValue
	 */
	private Tag tagFromMap(Tag parent, Map<String, String> attrs, String key, String tag, String defaultValue) {
		String value = attrs.get(key);
		if (value == null)
			value = attrs.get(tag);
		if (value == null)
			value = defaultValue;
		if (value == null)
			return parent;
		new Tag(parent, tag).addContent(value.trim());
		return parent;
	}

	public void setProperties(Map<String, String> scm) {
		this.scm = scm;
	}

	public String validate() {
		return null;
	}
}
