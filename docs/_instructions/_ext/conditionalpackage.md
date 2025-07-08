---
layout: default
class: Project
title: -conditionalpackage PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *  
summary: Recursively add packages from the class path when referred and when they match one of the package specifications. 
---
The `-conditionalpackage` instruction implements a feature that is either looked up in awe (when understood) or regarded with disgust. Though one has to be very careful with its use, it is a feature that can significantly reduce runtime dependencies. So what does it do? Well, it copies the content of packages from the current class path into your bundle that:

* Are currently referenced inside the bundle, and
* Match the given selector
* Repeat until no more packages are added

The purpose of this instruction is very much the same as static linking. Unix'es have a similar feature with static linking. It addresses the problem of _util_ libraries. Utility libraries are extremely useful because they allow us to create primitives we can use in many places. However, in reality utility libraries are often quite, well let's say, all over the place. As a result utility libraries tend to drag in a lot of transient dependencies that are actually not all needed. As a common example, someone once used a single method from an Apache Commons library and dragged in 20 Mb of dependencies.

The purpose therefore of the `-conditionalpackage` instruction is to pick cohesive packages from a utility library and copy them in the bundle. Any dependencies of those copied packages will also be copied if they match the selectors.

The packages that are copied cannot be exported, they must be private. This makes it possible to rely on the exact version that the bundle is compiled against. It also ensures that no information is leaked between bundles when statics are used. 

The given `PACKAGE-SPEC` follows the format outlined in [Selector](../chapters/820-instructions.html#selector).

For example:

	-conditionalpackage: aQute.lib*

will slurp any packages that have a name that matches `aQute.lib*` (e.g. both `aQute.lib` and `aQute.libg`) and are referred to by the current JAR's contents.

On the other hand the example:

    -conditionalpackage: mypackage.example.*
    
will copy the package `mypackage.example` and all its sub-packages into the bundle in case they are referred to by the current JAR's contents.  

## Utility Libraries

A good example of a suitable library is the aQute.libg project. It is a collection of packages that each implement a single function. This ranges from a Strings class with simple String utilities, all the way to a Trajan graph analyzer and simple fork-join framework. Though some of the packages are dependent on each other, most packages have no dependencies whatsoever.

When preparing a library for `-conditionalpackage` you should take the following into account:

* Only use it for packages that are highly cohesive
* Minimize external dependencies
* Do not maintain shared state in the code
* Do not use it to create abstraction of entities (services are better for that)

## Objections

One objection that is raised against this model is that you copy code. However, this model only copies the binaries, not source code, therefore, there is no real duplication. Yes, they then say, but when there is a bug I need to fix in many places? Well, the bundles will have to be rebuild but that is generally a good idea when a dependency changes. And think about it, that bundle was tested with the buggy code, it is highly unlikely that this bug seriously affects the bundle.
   