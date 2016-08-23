---
layout: default
class: Header
title: Bundle-Category STRING (',' STRING )
summary: The categories this bundle belongs to, can be set through the BundleCategory annotation
---
	

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

