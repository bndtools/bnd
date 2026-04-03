---
layout: default
class: Project
title: -resourceonly  BOOLEAN
summary: Ignores warning if the bundle only contains resources and no classes. 
---

# -resourceonly

The `-resourceonly` instruction tells bnd to ignore warnings if the bundle contains only resources and no classes. This is useful for bundles that are intended to provide configuration files, images, or other non-code assets.

Example:

```
-resourceonly: true
```

When set, bnd will not warn about the absence of classes in the bundle.


<hr />
TODO Needs review - AI Generated content