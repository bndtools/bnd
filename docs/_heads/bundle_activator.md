---
layout: default
title: Bundle-Activator CLASS
class: Header
summary: |
   The Bundle-Activator header specifies the name of the class used to start and stop the bundle
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Activator: com.acme.foo.Activator`

- Values: `${classes;implementing;org.osgi.framework.BundleActivator}`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_activator.md --><br /><br />

# Bundle-Activator

The `Bundle-Activator` header specifies the fully qualified name of the class that implements the `org.osgi.framework.BundleActivator` interface. This class is used by the OSGi framework to start and stop the bundle. When the bundle is started, the framework creates an instance of this class and calls its `start` method. When the bundle is stopped, the `stop` method is called.

The activator class must be included in the bundle and accessible on the bundle classpath. If the class is missing, not accessible, or not properly implemented, bnd will issue a warning or error during analysis.

Example:

```
Bundle-Activator: com.example.MyActivator
```

If the activator is not specified, the bundle will not have custom start/stop behavior.


TODO Needs review - AI Generated content
