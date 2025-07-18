---
layout: default
class: Header
title: Created-By STRING
summary: Java version used in build 
---

# Created-By

The `Created-By` header in the bundle manifest records the Java version and vendor used to build the bundle. This information is automatically added by bnd during the build process. It can be useful for tracking the build environment, diagnosing compatibility issues, or auditing builds.

The value typically looks like:

```
Created-By: 17.0.8 (Eclipse Adoptium)
```

This header is set by bnd and should not be manually changed.

## Prose Explanation

The `Created-By` header is important for understanding the context in which a Java bundle was created. It provides insight into the Java Development Kit (JDK) version and the vendor's implementation details. This can be critical when you are trying to debug issues, ensure compatibility with other Java components, or verify that the bundle was built using a trusted JDK vendor.



---
TODO Needs review - AI Generated content