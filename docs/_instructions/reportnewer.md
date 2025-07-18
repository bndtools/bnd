---
layout: default
title: -reportnewer BOOLEAN
class: Project
summary: |
   Report any entries that were added to the build since the last JAR was made.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-reportnewer=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/reportnewer.md --><br /><br />

# -reportnewer

The `-reportnewer` instruction reports any entries that were added to the build since the last JAR was made. This is useful for tracking changes and ensuring that only new or updated files are included in the build output.

Example:

```
-reportnewer: true
```

When enabled, bnd will output a list of files that are newer than the previous build.


TODO Needs review - AI Generated content
