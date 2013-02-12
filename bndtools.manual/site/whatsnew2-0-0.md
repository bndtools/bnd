---
title: New in Bndtools 2.0
description: Based on bnd 2.0
author: Neil Bartlett
---

Support for OSGi R5 Resolver
============================

Bndtools 2.0 integrates the reference implementation of the OSGI Release 5 Resolver specification. We can now create run descriptors based on a very small number of "core" bundles that define our application, and allow all other dependencies to be automatically added through resolution.

This includes not just static dependencies arising from package imports, but also extender bundles (e.g. Declarative Services or Blueprint), service providers, or any arbitrary capability defined with `Provide`/`Require-Capability` headers.

Export as Standalone Executable
===============================

A run descriptor (bndrun file) is a description of an application, including all of the bundles and configuration required to run it. Previously these files were only used to launch in testing or debugging mode from Bndtools. However they can now be exported to a standalone executable JAR file, which precisely reproduces the runtime configuration used in testing.

The export feature is available both from the Bndtools GUI and as a bnd command and ANT task.

Baselining
==========

In version 1.0, Bndtools included a Diff and Release tool that compared bundles with their released version in a repository, and suggested the correct versions for the exported packages and the bundle itself. However it was still possible for developers to forget to run the tool, and thereby fail to indicate changes that had been made.

Now, the Diff/Release functionality has been pushed down into bnd and incorporated into the build process. This means that bnd will automatically check versions, and optionally break the build when they do not accurately reflect the changes made in the code. Thus developers can catch errors before checking in code, and/or they can be caught by a continuous integration server.

Enhanced Semantic Versioning
============================

Bnd and Bndtools now support the `@ProviderType` and `@ConsumerType` annotations on API interfaces. These define the role of that interface within the service contract, and thus how implementers must be versioned.

For example if a bundle implements an interface annotated with `@ProviderType`, it is now known to be provider and will import the API using a narrow version range such as `[1.0, 1.1)`. On the other hand, if our bundle only implements `@ConsumerType` interfaces then it uses a normal consumer import range e.g. `[1.0, 2.0)`.

Exported Package Decorations
============================

Bndtools 2.0 adds decorators to the Eclipse Package Explorer for exported and excluded packages. These enable you to see at a glance:

* which packages are exported (i.e. public), along with their version;
* which packages are excluded from any bundle in the project. 

Improved Incremental Builder
============================

In Bndtools 1.0 the incremental project builder was inefficient and intrusive, occasionally  even blocking the user from working while it carried out certain tasks. The new builder in 2.0 minimises the work it needs to do by being smarter about dependencies, and as a result is faster and much less intrusive.

Support for Apache ACE
======================

Apache ACE is a software distribution framework that allows you to centrally manage and distribute software components, configuration data and other artifacts to target systems. Bndtools now supports deploying directy to an ACE server from within the IDE.

Miscellaneous
=============

* Many bug fixes, particularly for bugs affecting Windows users;
* Regeneration of bundles on resource changes (i.e. files referenced by `Include-Resource`);
* Support for alternative annotation styles for Declarative Services components, including the standard annotations defined in DS 1.2;
* For build and runtime dependencies, the best match from all available repositories is chosen, rather than the first available match;
* Support for Java 7 bytecode and Execution Environment;
* Validation of `Provide-Capability` and `Require-Capability` headers.
