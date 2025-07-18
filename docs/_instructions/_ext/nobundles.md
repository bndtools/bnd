---
layout: default
class: Project
title: -nobundles  BOOLEAN
summary:  Do not build the project.
---

# -nobundles

The `-nobundles` instruction tells bnd to skip building the project. This can be useful for projects that only provide resources or configuration, or for disabling builds in certain environments.

Example:

```
-nobundles: true
```

When this instruction is set to `true`, bnd will not produce any output JARs for the project.

---
TODO Needs review - AI Generated content