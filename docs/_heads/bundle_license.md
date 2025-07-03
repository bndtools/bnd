---
layout: default
title: Bundle-License ::= '<<[EXTERNAL]>>' | ( license ( ',' license ) * )
class: Header
summary: |
   The Bundle-License header provides an optional machine readable form of license information.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-License: http://www.opensource.org/licenses/jabberpl.php`

- Values: `http://www.apache.org/licenses/LICENSE-2.0,<<EXTERNAL>>`

- Pattern: `(.*|<<EXTERNAL>>)`

### Options ###

- `description`
  - Example: `description="Describe the license here"`

  - Pattern: `.*`


- `link`
  - Example: ``

  - Pattern: `.*`

<!-- Manual content from: ext/bundle_license.md --><br /><br />
	
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

	
