---
layout: default
title: -wablib FILE ( ',' FILE )*
class: Builder
summary: |
   Specify the libraries that must be included in a Web Archive Bundle (WAB) or WAR.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-wablib=lib/a.jar, lib/b.jar`

- Pattern: `.*`

<!-- Manual content from: ext/wablib.md --><br /><br />

	/**
	 * Turn this normal bundle in a web and add any resources.
	 *
	 * @throws Exception
	 */
	private Jar doWab(Jar dot) throws Exception {
		String wab = getProperty(WAB);
		String wablib = getProperty(WABLIB);
		if (wab == null && wablib == null)
			return dot;

		trace("wab %s %s", wab, wablib);
		setBundleClasspath(append("WEB-INF/classes", getProperty(BUNDLE_CLASSPATH)));

		Set<String> paths = new HashSet<String>(dot.getResources().keySet());

		for (String path : paths) {
			if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
				trace("wab: moving: %s", path);
				dot.rename(path, "WEB-INF/classes/" + path);
			}
		}

		Parameters clauses = parseHeader(getProperty(WABLIB));
		for (String key : clauses.keySet()) {
			File f = getFile(key);
			addWabLib(dot, f);
		}
		doIncludeResource(dot, wab);
		return dot;
	}
