package aQute.bnd.main;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.osgi.*;
import aQute.lib.tag.*;
import aQute.libg.version.*;

public class PomResource extends WriteResource {
	final Manifest				manifest;
	private Map<String, String>	scm;
	final static Pattern		NAME_URL	= Pattern.compile("(.*)(http://.*)");
	String xbsn;
	String xgroupId;
	String xartifactId;
	
	public String getBsn() {
		if ( xbsn == null )
			xbsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (xbsn == null)
			throw new RuntimeException("Cannot create POM unless bsn is set");

		xbsn = xbsn.trim();
		int n = xbsn.lastIndexOf('.');
		if (n < 0)  {
			n = xbsn.length();
			xbsn = xbsn + "." + xbsn;
		}
		
		xgroupId = xbsn.substring(0, n);
		xartifactId = xbsn.substring(n + 1);
		n = xartifactId.indexOf(';');
		if (n > 0)
			xartifactId = xartifactId.substring(0, n).trim();
		
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
		String version = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		return new Version(version);
	}
	
	public PomResource(Manifest manifest) {
		this.manifest = manifest;
	}

	@Override public long lastModified() {
		return 0;
	}

	@Override public void write(OutputStream out) throws IOException {
		PrintWriter ps = new PrintWriter(out);

		String name = manifest.getMainAttributes().getValue(Analyzer.BUNDLE_NAME);

		String description = manifest.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION);
		String docUrl = manifest.getMainAttributes().getValue(Constants.BUNDLE_DOCURL);
		String bundleVendor = manifest.getMainAttributes().getValue(Constants.BUNDLE_VENDOR);

		String licenses = manifest.getMainAttributes().getValue(Constants.BUNDLE_LICENSE);

		Tag project = new Tag("project");
		project.addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
		project.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		project.addAttribute("xmlns:xsi", "");
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

			Map<String, Map<String, String>> map = Processor.parseHeader(licenses, null);
			for (Iterator<Map.Entry<String, Map<String, String>>> e = map.entrySet().iterator(); e
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

				Map.Entry<String, Map<String, String>> entry = e.next();
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
		String value = (String) values.get(string);
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
