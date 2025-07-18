---
layout: default
title: -failok ('true' | 'false')?
class: Project
summary: |
   Will ignore any error during building and assume all went ok.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-failok=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/failok.md --><br /><br />

# -failok

The `-failok` instruction allows the build to continue even if errors occur. When set to `true`, bnd will ignore any errors during the build process and assume everything went fine. This can be useful for experimental or non-critical builds, but should be used with caution as it may hide real problems.

Example:

```
-failok: true
```

By default, this option is not set and build errors will cause the process to fail.


TODO Needs review - AI Generated content
