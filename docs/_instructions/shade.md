---
layout: default
class: Builder
title: -shade SELECTOR
summary: Rename (shade) packages in the jar
---

OSGi has _private_ packages that can have overlapping names with other bundles. However, bnd often generates 
bundles that have to run inside a classic class path. Using the same classes, as for example [-conditionalpackage](conditionalpackage.html)
promotes can then wreak havoc if different versions are used. This can even cause problems in OSGi since a
JAR on the classpath will load a class from the class path if it happens to be there.

The `-shade` function selects packages, and for each selected package it will rename the resources in the JAR. It will then
parse all the class files in the JAR and rename the references to renamed classes. 

By default, the selected packages will be renamed to a hash that is based on the bundle's BSN, version, and package. This is
the preferred way to use this function. It can reduce the package size since the long name is replaced with a shorter
hash of 8 characters. It also ensures that multiple versions of the same bundles do not clash.

However, it is also possible to prefix the package with a self chosen prefix using the `prefix:` directive. The
given prefix is then prefixed to package name.

### Simple Example 

The following example will rename the package `org.osgi.framework` and any sub packages:

    -shade org.osgi.framework.*;prefix:=com.example

The `org.osgi.framework` package will be renamed to `com.example.org.osgi.framework`.

### Example with Conditional Package

The `-conditionalpackage` instruction drags in packages that are referred to by classes in the JAR. For example,
the `aQute.libg` library was designed to be used for this. Although this model has a great number of advantages,
it has the drawback that the same packages can be on the classpath multiple times, causing conflicts. This
can never happen in an OSGi framework, but for a class path resident JAR it is a serious issue.

    -conditionalpackage: aQute.lib*
    -shade: ${-conditionalpackage}

