---
layout: default
title: Bundle-Name STRING
class: Header
summary: |
   The Bundle-Name header defines a readable name for this bundle. This should be a short, hu- man-readable name that can contain spaces.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Name: My Bundle`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_name.md --><br /><br />

# Bundle-Name

The `Bundle-Name` header defines a short, human-readable name for the bundle. This name is intended for display in user interfaces and tools. If the `Bundle-Name` is not set, it will default to the value of the `Bundle-SymbolicName` header.

Example:

```
Bundle-Name: My Example Bundle
```

This header is optional but recommended for clarity and usability.

If the Bundle-Name is not set, it will default to the Bundle-SymbolicName.
	
			//
			// Use the same name for the bundle name as BSN when
			// the bundle name is not set
			//
			if (main.getValue(BUNDLE_NAME) == null) {
				main.putValue(BUNDLE_NAME, bsn);
			}



TODO Needs review - AI Generated content
