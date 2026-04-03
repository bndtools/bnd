---
layout: default
class: Project
title: -failok ('true' | 'false')?
summary: Will ignore any error during building and assume all went ok. 
---

# -failok

The `-failok` instruction allows the build to continue even if errors occur. When set to `true`, bnd will ignore any errors during the build process and assume everything went fine. This can be useful for experimental or non-critical builds, but should be used with caution as it may hide real problems.

Example:

```
-failok: true
```

By default, this option is not set and build errors will cause the process to fail.


<hr />
TODO Needs review - AI Generated content