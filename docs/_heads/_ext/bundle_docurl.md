---
layout: default
class: Header
title: Bundle-DocURL STRING
summary: The Bundle-DocURL headers must contain a URL pointing to documentation about this bundle.
---
	
	/*
	 * Bundle-DocURL header
	 */
	private void doBundleDocURL(BundleDocURL annotation) {
		add(Constants.BUNDLE_DOCURL, annotation.value());
	}

	