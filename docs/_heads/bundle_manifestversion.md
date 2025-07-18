---
layout: default
title: Bundle-ManifestVersion ::= 2
class: Header
summary: |
   The Bundle-ManifestVersion is always set to 2, there is no way to override this.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `# Bundle-ManifestVersion: 2`

- Values: `2`

- Pattern: `\d+`

<!-- Manual content from: ext/bundle_manifestversion.md --><br /><br />

# Bundle-ManifestVersion

The `Bundle-ManifestVersion` header is always set to `2` for OSGi R4 and later bundles. This value is required by the OSGi specification and cannot be changed or omitted. It ensures compatibility with the OSGi framework.

Example:

```
Bundle-ManifestVersion: 2
```

This header is automatically set by bnd and should not be modified manually.

TODO Needs review - AI Generated content
