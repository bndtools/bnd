---
layout: default
title: Bnd-AddXmlToTest RESOURCE ( ',' RESOURCE )
class: Macro
summary: |
   Add XML resources from the tested bundle to the output of a test report.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bnd-AddXMLToTest: a.xml`

- Pattern: `.*`

<!-- Manual content from: ext/bnd_addxmltotest.md --><br /><br />

	public void setup(Bundle fw, Bundle targetBundle) {
		startTime = System.currentTimeMillis();

		testsuite.addAttribute("hostname", hostname);
		if (targetBundle != null) {
			testsuite.addAttribute("name", "test." + targetBundle.getSymbolicName());
			testsuite.addAttribute("target", targetBundle.getLocation());
		} else {
			testsuite.addAttribute("name", "test.run");

		}
		testsuite.addAttribute("timestamp", df.format(new Date()));
		testsuite.addAttribute("framework", fw);
		testsuite.addAttribute("framework-version", fw.getVersion());

		Tag properties = new Tag("properties");
		testsuite.addContent(properties);

		for (Map.Entry<Object,Object> entry : System.getProperties().entrySet()) {
			Tag property = new Tag(properties, "property");
			property.addAttribute("name", entry.getKey());
			property.addAttribute("value", entry.getValue());
		}

		Tag bundles = new Tag(testsuite, "bundles");
		Bundle bs[] = fw.getBundleContext().getBundles();

		for (int i = 0; i < bs.length; i++) {
			Tag bundle = new Tag("bundle");
			bundle.addAttribute("location", bs[i].getLocation());
			bundle.addAttribute("modified", df.format(new Date(bs[i].getLastModified())));
			bundle.addAttribute("state", bs[i].getState());
			bundle.addAttribute("id", bs[i].getBundleId() + "");
			if ( bs[i].getSymbolicName() != null)
				bundle.addAttribute("bsn", bs[i].getSymbolicName());
			if ( bs[i].getVersion() != null)
				bundle.addAttribute("version", bs[i].getVersion());

			if (bs[i].equals(targetBundle))
				bundle.addAttribute("target", "true");

			bundles.addContent(bundle);
		}
		if (targetBundle != null) {
			String header = (String) targetBundle.getHeaders().get(aQute.bnd.osgi.Constants.BND_ADDXMLTOTEST);
			if (header != null) {
				StringTokenizer st = new StringTokenizer(header, " ,");

				while (st.hasMoreTokens()) {
					String resource = st.nextToken();
					URL url = targetBundle.getEntry(resource);

					if (url != null) {
						String name = url.getFile();
						int n = name.lastIndexOf('/');
						if (n < 0)
							n = 0;
						else
							n = n + 1;

						if (name.endsWith(".xml"))
							name = name.substring(n, name.length() - 4);
						else
							name = name.substring(n, name.length()).replace('.', '_');

						testsuite.addContent(url);

					} else {
						Tag addxml = new Tag(testsuite, "error");
						addxml.addAttribute("reason", "no such resource: " + resource);
					}
				}
			}
		}
	}
