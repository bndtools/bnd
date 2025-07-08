---
layout: default
class: Header
title: Bundle-License ::= '<<[EXTERNAL]>>' | ( license ( ',' license ) * )
summary: The Bundle-License header provides an optional machine readable form of license information. 
---
	
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

	
