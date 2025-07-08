---
layout: default
title: -manifest FILE
class: Builder
summary: |
   Override manifest calculation and set fixed manifest
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-manifest=META-INF/MANIFEST.MF`

- Pattern: `.*`

<!-- Manual content from: ext/manifest.md --><br /><br />

		// Check if we override the calculation of the
		// manifest. We still need to calculated it because
		// we need to have analyzed the classpath.

		Manifest manifest = calcManifest();

		String mf = getProperty(MANIFEST);
		if (mf != null) {
			File mff = getFile(mf);
			if (mff.isFile()) {
				try {
					InputStream in = new FileInputStream(mff);
					manifest = new Manifest(in);
					in.close();
				}
				catch (Exception e) {
					error(MANIFEST + " while reading manifest file", e);
				}
			} else {
				error(MANIFEST + ", no such file " + mf);
			}
		}
