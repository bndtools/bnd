---
layout: default
class: Header
title: Bundle-Activator CLASS
summary: The Bundle-Activator header specifies the name of the class used to start and stop the bundle
---

# Bundle-Activator

The `Bundle-Activator` header specifies the fully qualified name of the class that implements the `org.osgi.framework.BundleActivator` interface. This class is used by the OSGi framework to start and stop the bundle. When the bundle is started, the framework creates an instance of this class and calls its `start` method. When the bundle is stopped, the `stop` method is called.

The activator class must be included in the bundle and accessible on the bundle classpath. If the class is missing, not accessible, or not properly implemented, bnd will issue a warning or error during analysis.

Example:

```
Bundle-Activator: com.example.MyActivator
```

If the activator is not specified, the bundle will not have custom start/stop behavior.


