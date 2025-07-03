---
layout: default
class: Builder
title: -metatypeannotations-options SELECTORS
summary: Restricts the use of Metatype Annotation to a minimum version.
---

```properties
-metatypeannotations-options: version;minimum=1.2.0
```

Analogous to `-dsannotations-options`, this will also restrict the use of OSGi Metatype annotations to minimum 1.2.0 version. The version number denotes that the users can use any version equal to or higher than 1.2.0, provided that the users have the Metatype annotations included on the build path.
