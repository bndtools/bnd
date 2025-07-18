---
layout: default
title: -resourceonly  BOOLEAN
class: Project
summary: |
   Ignores warning if the bundle only contains resources and no classes.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-resourceonly=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/resourceonly.md --><br /><br />

# -resourceonly

The `-resourceonly` instruction tells bnd to ignore warnings if the bundle contains only resources and no classes. This is useful for bundles that are intended to provide configuration files, images, or other non-code assets.

Example:

```
-resourceonly: true
```

When set, bnd will not warn about the absence of classes in the bundle.


TODO Needs review - AI Generated content
