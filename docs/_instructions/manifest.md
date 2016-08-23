---
layout: default
class: Builder
title: -manifest FILE   
summary:  Override manifest calculation and set fixed manifest
---

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
