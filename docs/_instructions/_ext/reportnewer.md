---
layout: default
class: Project
title: -reportnewer BOOLEAN 
summary: Report any entries that were added to the build since the last JAR was made.
---

# -reportnewer

The `-reportnewer` instruction reports any entries that were added to the build since the last JAR was made. This is useful for tracking changes and ensuring that only new or updated files are included in the build output.

Example:

```
-reportnewer: true
```

When enabled, bnd will output a list of files that are newer than the previous build.

