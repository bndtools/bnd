---
layout: default
class: Header
title: Bundle-License ::= '<<[EXTERNAL]>>' | ( license ( ',' license ) * )
summary: The Bundle-License header provides an optional machine readable form of license information. 
---

# Bundle-License

The `Bundle-License` header provides machine-readable license information for the bundle. It can list one or more licenses, separated by commas, and may include additional attributes such as a description or a link to the license text.

Example:

```
Bundle-License: Apache-2.0;description='Apache License, Version 2.0';link='https://www.apache.org/licenses/LICENSE-2.0'
```

This header is optional but recommended for clarity and compliance. It helps users and tools understand the licensing terms of the bundle.

		/*
	 * Bundle-License header
	 */
	private void doLicense(BundleLicense annotation) {
		StringBuilder sb = new StringBuilder(annotation.name());
		if (!annotation.description().equals(""))
			sb.append(";description='").append(annotation.description().replaceAll("'", "\\'")).append("'");
		if (!annotation.link().equals(""))
			sb.append(";link='").append(annotation.link().replaceAll("'", "\\'")).append("'");
		add(Constants.BUNDLE_LICENSE, sb.toString());
	}




---
TODO Needs review - AI Generated content