---
layout: default
title: Bundle-Category STRING (',' STRING )
class: Header
summary: |
   The categories this bundle belongs to, can be set through the BundleCategory annotation
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Category: test`

- Values: `osgi,test,game,util,eclipse,netbeans,jdk,specification`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_category.md --><br /><br />
	

	/*
	 * Bundle-Category header
	 */
	private void doBundleCategory(BundleCategory annotation) {
		if (annotation.custom() != null)
			for (String s : annotation.custom()) {
				add(Constants.BUNDLE_CATEGORY, s);
			}

		if (annotation.value() != null)
			for (Category s : annotation.value()) {
				add(Constants.BUNDLE_CATEGORY, s.toString());
			}
	}
