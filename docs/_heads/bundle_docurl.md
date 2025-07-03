---
layout: default
title: Bundle-DocURL STRING
class: Header
summary: |
   The Bundle-DocURL headers must contain a URL pointing to documentation about this bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-DocURL: http://www.aQute.biz/Code/Bnd`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_docurl.md --><br /><br />
	
	/*
	 * Bundle-DocURL header
	 */
	private void doBundleDocURL(BundleDocURL annotation) {
		add(Constants.BUNDLE_DOCURL, annotation.value());
	}

	
