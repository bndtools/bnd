---
layout: default
class: Header
title: Bundle-ClassPath ::= entry ( ',' entry )*
summary: The Bundle-ClassPath header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The full stop ('.' \u002E) specifies the root di- rectory of the bundle's JAR. The full stop is also the default
---

# Bundle-ClassPath

The `Bundle-ClassPath` header defines the internal class path for the bundle. It is a comma-separated list of JAR file paths or directories (inside the bundle) that contain classes and resources. The special entry `.` refers to the root of the bundle JAR and is the default if the header is not specified.

All files or directories listed in the `Bundle-ClassPath` must be present in the bundle. You can use the `Include-Resource` instruction to include additional JARs or directories. In most cases, it is recommended to avoid using `Bundle-ClassPath` unless necessary, as it can complicate class loading. Instead, use the `@` option in `Include-Resource` to unroll JARs into the main bundle.

Example:

```
Bundle-ClassPath: ., lib/extra.jar
```

This header is important for advanced scenarios where you need to include additional classpath entries inside your bundle.


<hr />
TODO Needs review - AI Generated content