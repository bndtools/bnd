---
layout: default
class: Header
title: Bundle-Name STRING
summary: The Bundle-Name header defines a readable name for this bundle. This should be a short, hu- man-readable name that can contain spaces. 
---


If the Bundle-Name is not set, it will default to the Bundle-SymbolicName.
	
			//
			// Use the same name for the bundle name as BSN when
			// the bundle name is not set
			//
			if (main.getValue(BUNDLE_NAME) == null) {
				main.putValue(BUNDLE_NAME, bsn);
			}

