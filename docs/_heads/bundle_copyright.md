---
layout: default
title: Bundle-Copyright STRING
class: Header
summary: |
   The Bundle-Copyright header contains the copyright specification for this bundle. Can be set with the BundleCopyright annotation.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Copyright: OSGi (c) 2002`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_copyright.md --><br /><br />
	

	/*
	 * Bundle-Copyright header
	 */
	private void doBundeCopyright(BundleCopyright annotation) {
		add(Constants.BUNDLE_COPYRIGHT, annotation.value());
	}
