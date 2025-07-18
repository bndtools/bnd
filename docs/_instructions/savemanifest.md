---
layout: default
title: -savemanifest FILE
class: Builder
summary: |
   Write out the manifest to a separate file after it has been calculated.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-savemanifest=file.txt`

- Pattern: `.*`

<!-- Manual content from: ext/savemanifest.md --><br /><br />

# -savemanifest

The `-savemanifest` instruction allows you to write out the calculated manifest to a separate file after it has been generated. This is useful for debugging, auditing, or sharing the manifest independently of the JAR.

Example:

```
-savemanifest: my-manifest.mf
```

The specified file will contain the manifest as it would appear in the final bundle.


TODO Needs review - AI Generated content
