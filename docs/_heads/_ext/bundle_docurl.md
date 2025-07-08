---
layout: default
class: Header
title: Bundle-DocURL STRING
summary: The Bundle-DocURL headers must contain a URL pointing to documentation about this bundle.
---

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

