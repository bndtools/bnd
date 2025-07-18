---
layout: default
class: Header
title: Bundle-Blueprint RESOURE (',' RESOURCE )
summary: The Bundle-Activator header specifies the name of the class used to start and stop the bundle
---

# Bundle-Blueprint

The `Bundle-Blueprint` header is used to specify the location of Blueprint XML files within the bundle. These XML files define Blueprint dependency injection containers for OSGi. The header can list one or more resource paths, separated by commas. bnd will process these files and include them in the bundle as needed.

Example:

```
Bundle-Blueprint: OSGI-INF/blueprint/context.xml, META-INF/spring/context.xml
```

This header is typically used in bundles that provide OSGi Blueprint services or use Spring DM. The specified XML files must be present in the bundle at the given locations.


<hr />
TODO Needs review - AI Generated content