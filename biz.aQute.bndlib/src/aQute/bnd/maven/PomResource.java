package aQute.bnd.maven;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.tag.*;
import aQute.libg.header.*;

public class PomResource extends WriteResource {
	final Manifest				manifest;
	private Map<String, String>	scm;
	final static Pattern		NAME_URL	= Pattern.compile("(.*)(http://.*)");

	public PomResource(Manifest manifest) {
		this.manifest = manifest;
	}

	@Override public long lastModified() {
		return 0;
	}

	@Override public void write(OutputStream out) throws IOException {
		PrintWriter ps = IO.writer(out);

		String name = manifest.getMainAttributes().getValue(Analyzer.BUNDLE_NAME);

		String description = manifest.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION);
		String docUrl = manifest.getMainAttributes().getValue(Constants.BUNDLE_DOCURL);
		String version = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		String bundleVendor = manifest.getMainAttributes().getValue(Constants.BUNDLE_VENDOR);

		String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		String licenses = manifest.getMainAttributes().getValue(Constants.BUNDLE_LICENSE);

		if (bsn == null) {
			throw new RuntimeException("Cannot create POM unless bsn is set");
		}

		bsn = bsn.trim();
		int n = bsn.lastIndexOf('.');
		if (n <= 0)
			throw new RuntimeException(
					"Can not create POM unless Bundle-SymbolicName contains a . to separate group and  artifact id");

		if (version == null)
			version = "0";

		String groupId = bsn.substring(0, n);
		String artifactId = bsn.substring(n + 1);
		n = artifactId.indexOf(';');
		if (n > 0)
			artifactId = artifactId.substring(0, n).trim();

		Tag project = new Tag("project");
		project.addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
		project.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		project.addAttribute("xmlns:xsi", "");
		project.addAttribute("xsi:schemaLocation",
				"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd");

		project.addContent(new Tag("modelVersion").addContent("4.0.0"));
		project.addContent(new Tag("groupId").addContent(groupId));
		project.addContent(new Tag("artifactId").addContent(artifactId));
		project.addContent(new Tag("version").addContent(version));

		if (description != null) {
			new Tag(project, "description").addContent(description);
		}
		if (name != null) {
			new Tag(project, "name").addContent(name);
		}
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
		if (licenses != null) {
			Tag ls = new Tag(project, "licenses");

			Parameters map = Processor.parseHeader(licenses, null);
			for (Iterator<Entry<String, Attrs>> e = map.entrySet().iterator(); e
					.hasNext();) {

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
	private Tag tagFromMap(Tag parent, Map<String, String> values, String string, String tag,
			String object) {
		String value = values.get(string);
		if (value == null)
			value = object;
		if (value == null)
			return parent;
		new Tag(parent, tag).addContent(value.trim());
		return parent;
	}

	public void setProperties(Map<String, String> scm) {
		this.scm = scm;
	}
}
