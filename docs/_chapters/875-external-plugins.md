---
title:  External Plugins
layout: default
---

## External Plugins

External Plugins are external code to bnd code but that can be executed from within bnd. The JARs for this code are coming from the Workspace repository. The External Plugin Namespace defines the namespace (`bnd.external.plugin`)  and the following attributes:

| Attribute                     | Description                   |
|-------------------------------+-------------------------------|
| `bnd.external.plugin`         | Defines the name of the plugin, should follow simple token syntax|
| objectClass                   | The interface type the implementation should implement|
| implementation                | The implementation fully qualified class name|
| subtype                       | Optional subtype when the `objectClass` has a type parameter |

There is an annotation, `aQute.bnd.service.externalplugin.ExternalPlugin`, that can be applied on a type.

For example:

    aQute.bnd.service.externalplugin.ExternalPlugin(
        name          = "calling", 
        objectClass   = Callable.class, 
        subtype       = String.class)
    public class CallImpl implements Callable<String> {
        public String call() throws Exception {
            return "hello";
        }
    }

In Bndtools, you can declare any class as an ExternalPlugin. The automatic build features will automatically build the JAR of the plugin and this will immediately become available in the rest of the build. If you use external plugins from the local workspace, make sure to declare a  `-dependson` to the external plugin project in any project that uses it, this dependency is not automatically detected. 

A JAR can contain any number of external plugins. It must ensure that it does not have any dependencies outside the bndlib it was compiled against.
