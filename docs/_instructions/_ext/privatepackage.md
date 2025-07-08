---
layout: default
class: Builder
title: -privatepackage PACKAGE-SPEC 
summary: Specify the private packages, these packages are included from the class path. Alternative to Private-Package, this version is not included in the manifest.
---

# -privatepackage

The `-privatepackage` instruction specifies packages to include from the class path as private packages. Unlike the `Private-Package` header, this instruction is not included in the manifest. It is used to control which packages are bundled privately in the output JAR.

Example:

```
-privatepackage: com.example.internal.*
```

This instruction is useful for fine-grained control over bundle contents during the build process.


