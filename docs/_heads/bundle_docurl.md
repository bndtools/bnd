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

# Bundle-DocURL

The `Bundle-DocURL` header specifies a URL that points to documentation for the bundle. This can be a website, wiki page, or any other resource that provides more information about the bundle's usage, features, or configuration.

Example:

```
Bundle-DocURL: https://docs.example.com/my-bundle
```

Providing this header is optional but highly recommended for discoverability and support.
	
	/*
	 * Bundle-DocURL header
	 */
	private void doBundleDocURL(BundleDocURL annotation) {
		add(Constants.BUNDLE_DOCURL, annotation.value());
	}



<hr />
TODO Needs review - AI Generated content
