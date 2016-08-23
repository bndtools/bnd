---
layout: default
class: Header
title: Bundle-Copyright STRING
summary: The Bundle-Copyright header contains the copyright specification for this bundle. Can be set with the BundleCopyright annotation. 
---
	

	/*
	 * Bundle-Copyright header
	 */
	private void doBundeCopyright(BundleCopyright annotation) {
		add(Constants.BUNDLE_COPYRIGHT, annotation.value());
	}

