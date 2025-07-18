---
layout: default
class: Header
title: Bundle-ManifestVersion ::= 2
summary: The Bundle-ManifestVersion is always set to 2, there is no way to override this.
---

# Bundle-ManifestVersion

The `Bundle-ManifestVersion` header is always set to `2` for OSGi R4 and later bundles. This value is required by the OSGi specification and cannot be changed or omitted. It ensures compatibility with the OSGi framework.

Example:

```
Bundle-ManifestVersion: 2
```

This header is automatically set by bnd and should not be modified manually.

<hr />
TODO Needs review - AI Generated content